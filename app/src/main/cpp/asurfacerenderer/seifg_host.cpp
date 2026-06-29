#include <jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <volk.h>

#include <chrono>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

#include "seifg.h"
#include "seifg_copy.h"

#define SPIKE_LOG(...) __android_log_print(ANDROID_LOG_INFO, "seifg_host", __VA_ARGS__)
#define SPIKE_ERR(...) __android_log_print(ANDROID_LOG_ERROR, "seifg_host", __VA_ARGS__)

namespace {

uint64_t enumerateDeviceUUID() {
    if (volkInitialize() != VK_SUCCESS) { SPIKE_ERR("volkInitialize failed"); return 0; }
    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &app;
    VkInstance inst = VK_NULL_HANDLE;
    if (vkCreateInstance(&ci, nullptr, &inst) != VK_SUCCESS) { SPIKE_ERR("vkCreateInstance failed"); return 0; }
    volkLoadInstance(inst);
    uint32_t count = 0;
    vkEnumeratePhysicalDevices(inst, &count, nullptr);
    if (count == 0) { SPIKE_ERR("no physical devices"); vkDestroyInstance(inst, nullptr); return 0; }
    std::vector<VkPhysicalDevice> devs(count);
    vkEnumeratePhysicalDevices(inst, &count, devs.data());
    VkPhysicalDeviceProperties props{};
    vkGetPhysicalDeviceProperties(devs.at(0), &props);
    uint64_t uuid = (static_cast<uint64_t>(props.vendorID) << 32) | props.deviceID;
    SPIKE_LOG("physical device '%s' uuid=%llu", props.deviceName, (unsigned long long)uuid);
    vkDestroyInstance(inst, nullptr);
    return uuid;
}

AHardwareBuffer* allocAhb(uint32_t w, uint32_t h) {
    AHardwareBuffer_Desc d{};
    d.width = w;
    d.height = h;
    d.layers = 1;
    d.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
    d.usage = AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE
            | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT
            | AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN
            | AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN;
    AHardwareBuffer* ahb = nullptr;
    if (AHardwareBuffer_allocate(&d, &ahb) != 0) { SPIKE_ERR("AHB alloc failed (%ux%u)", w, h); return nullptr; }
    return ahb;
}

void fillAhbSolid(AHardwareBuffer* ahb, uint8_t r, uint8_t g, uint8_t b) {
    AHardwareBuffer_Desc d{};
    AHardwareBuffer_describe(ahb, &d);
    void* ptr = nullptr;
    if (AHardwareBuffer_lock(ahb, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, nullptr, &ptr) != 0) return;
    auto* base = static_cast<uint8_t*>(ptr);
    for (uint32_t y = 0; y < d.height; ++y) {
        uint8_t* row = base + (size_t)y * d.stride * 4;
        for (uint32_t x = 0; x < d.width; ++x) {
            row[x * 4 + 0] = r; row[x * 4 + 1] = g; row[x * 4 + 2] = b; row[x * 4 + 3] = 255;
        }
    }
    AHardwareBuffer_unlock(ahb, nullptr);
}

void readAhbCenter(AHardwareBuffer* ahb, const char* label) {
    AHardwareBuffer_Desc d{};
    AHardwareBuffer_describe(ahb, &d);
    void* ptr = nullptr;
    if (AHardwareBuffer_lock(ahb, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, nullptr, &ptr) != 0) {
        SPIKE_ERR("readback lock failed (%s)", label); return;
    }
    auto* base = static_cast<uint8_t*>(ptr);
    uint32_t cx = d.width / 2, cy = d.height / 2;
    uint8_t* px = base + (size_t)cy * d.stride * 4 + (size_t)cx * 4;
    uint64_t sum = 0;
    for (uint32_t y = 0; y < d.height; y += 16)
        for (uint32_t x = 0; x < d.width; x += 16)
            sum += base[(size_t)y * d.stride * 4 + (size_t)x * 4];
    SPIKE_LOG("%s center=(%u,%u,%u,%u) Rsum16=%llu", label, px[0], px[1], px[2], px[3], (unsigned long long)sum);
    AHardwareBuffer_unlock(ahb, nullptr);
}

int64_t nowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}

}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_renderer_ASurfaceRenderer_nativeSeifgHostSpike(JNIEnv* env, jobject) {
    SPIKE_LOG("=== seifg host spike start ===");

    const uint64_t uuid = enumerateDeviceUUID();
    if (uuid == 0) { SPIKE_ERR("no device uuid; abort"); return; }

    const uint32_t W = 1280, H = 720;
    AHardwareBuffer* in0 = allocAhb(W, H);
    AHardwareBuffer* in1 = allocAhb(W, H);
    AHardwareBuffer* out = allocAhb(W, H);
    AHardwareBuffer* src = allocAhb(W, H);
    if (!in0 || !in1 || !out || !src) { SPIKE_ERR("AHB alloc failed; abort"); return; }

    fillAhbSolid(src, 220, 40, 40);
    fillAhbSolid(in1, 40, 40, 220);

    HostCopier copier;
    if (!copier.init(uuid)) { SPIKE_ERR("copier init failed; abort"); return; }
    if (!copier.copy(src, in0, VK_FORMAT_R8G8B8A8_UNORM, W, H)) { SPIKE_ERR("copier copy failed; abort"); return; }
    SPIKE_LOG("GPU-copied src(red)->in0; in1=blue(CPU). out should be midpoint if copy worked");

    seifg::initialize(uuid, false, 1.0F / 0.30F, 1, {});
    int32_t ctxId = seifg::createContextFromAHB(in0, in1, {out}, VkExtent2D{W, H}, VK_FORMAT_R8G8B8A8_UNORM);
    SPIKE_LOG("createContextFromAHB() OK id=%d", ctxId);

    for (int i = 0; i < 5; ++i) {
        int64_t t0 = nowMs();
        seifg::presentContext(ctxId, -1, {});
        seifg::waitIdle();
        SPIKE_LOG("generate[%d] took %lld ms", i, (long long)(nowMs() - t0));
    }

    readAhbCenter(in0, "in0");
    readAhbCenter(in1, "in1");
    readAhbCenter(out, "out(interp)");

    seifg::deleteContext(ctxId);
    seifg::finalize();

    copier.destroy();
    AHardwareBuffer_release(in0);
    AHardwareBuffer_release(in1);
    AHardwareBuffer_release(out);
    AHardwareBuffer_release(src);

    SPIKE_LOG("=== seifg host spike SUCCESS ===");
}
