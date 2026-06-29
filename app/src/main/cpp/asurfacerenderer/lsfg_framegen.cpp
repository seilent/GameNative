#include "lsfg_framegen.h"

#include <android/log.h>
#include <volk.h>
#include <functional>
#include <vector>

#include "lsfg_3_1.hpp"
#include "extract/extract.hpp"
#include "extract/trans.hpp"
#include "config/config.hpp"

#define FLOG(...) __android_log_print(ANDROID_LOG_INFO, "lsfg_host", __VA_ARGS__)
#define FERR(...) __android_log_print(ANDROID_LOG_ERROR, "lsfg_host", __VA_ARGS__)

namespace {

uint64_t enumUuid() {
    if (volkInitialize() != VK_SUCCESS) return 0;
    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &app;
    VkInstance inst = VK_NULL_HANDLE;
    if (vkCreateInstance(&ci, nullptr, &inst) != VK_SUCCESS) return 0;
    volkLoadInstance(inst);
    uint32_t n = 0;
    vkEnumeratePhysicalDevices(inst, &n, nullptr);
    if (n == 0) { vkDestroyInstance(inst, nullptr); return 0; }
    std::vector<VkPhysicalDevice> devs(n);
    vkEnumeratePhysicalDevices(inst, &n, devs.data());
    VkPhysicalDeviceProperties p{};
    vkGetPhysicalDeviceProperties(devs.at(0), &p);
    uint64_t uuid = (static_cast<uint64_t>(p.vendorID) << 32) | p.deviceID;
    vkDestroyInstance(inst, nullptr);
    return uuid;
}

int ahbToVk(uint32_t ahbFormat) {
    switch (ahbFormat) {
        case AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM: return VK_FORMAT_R8G8B8A8_UNORM;
        case 5: return VK_FORMAT_B8G8R8A8_UNORM; // AHARDWAREBUFFER_FORMAT_B8G8R8A8_UNORM
        case AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM: return VK_FORMAT_R8G8B8A8_UNORM;
        case AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT: return VK_FORMAT_R16G16B16A16_SFLOAT;
        case AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM: return VK_FORMAT_A2B10G10R10_UNORM_PACK32;
        default: return VK_FORMAT_R8G8B8A8_UNORM;
    }
}

AHardwareBuffer* allocAhb(uint32_t w, uint32_t h, uint32_t format) {
    AHardwareBuffer_Desc d{};
    d.width = w; d.height = h; d.layers = 1;
    d.format = format;
    d.usage = AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT
            | AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN | AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN;
    AHardwareBuffer* a = nullptr;
    if (AHardwareBuffer_allocate(&d, &a) != 0) return nullptr;
    return a;
}

} // namespace

bool HostFramegen::init(const std::string& dllPath, uint32_t width, uint32_t height, uint32_t ahbFormat) {
    w = width; h = height; ahbFmt = ahbFormat; vkFmt = ahbToVk(ahbFormat);
    const uint64_t uuid = enumUuid();
    if (uuid == 0) { FERR("framegen: no device uuid"); return false; }

    if (!copier.init(uuid)) { FERR("framegen: copier init failed"); return false; }

    in0 = allocAhb(w, h, ahbFmt);
    in1 = allocAhb(w, h, ahbFmt);
    out = allocAhb(w, h, ahbFmt);
    presentBuf[0] = allocAhb(w, h, ahbFmt);
    presentBuf[1] = allocAhb(w, h, ahbFmt);
    if (!in0 || !in1 || !out || !presentBuf[0] || !presentBuf[1]) { FERR("framegen: AHB alloc failed"); return false; }

    Config::activeConf.dll = dllPath;
    auto loader = [](const std::string& name) -> std::vector<uint8_t> {
        return Extract::translateShader(Extract::getShader(name));
    };
    try {
        Extract::extractShaders();
        LSFG_3_1::initialize(uuid, false, 1.0F / 0.30F, 1, loader);
        ctxId = LSFG_3_1::createContextFromAHB(
            in0, in1, { out }, VkExtent2D{ w, h }, static_cast<VkFormat>(vkFmt));
    } catch (const std::exception& e) {
        FERR("framegen: engine init threw: %s", e.what());
        return false;
    }
    ready = true;
    FLOG("framegen: init OK %ux%u ahbFmt=%u vkFmt=%d ctx=%d", w, h, ahbFmt, vkFmt, ctxId);
    return true;
}

AHardwareBuffer* HostFramegen::submit(AHardwareBuffer* incoming) {
    if (!ready) return nullptr;
    AHardwareBuffer* dst = (frameIdx % 2 == 0) ? in0 : in1;
    if (!copier.copy(incoming, dst, static_cast<VkFormat>(vkFmt), w, h)) {
        FERR("framegen: copy failed");
        return nullptr;
    }
    const uint64_t idx = frameIdx++;
    if (idx == 0) return nullptr;
    try {
        LSFG_3_1::presentContext(ctxId, -1, {});
        LSFG_3_1::waitIdle();
    } catch (const std::exception& e) {
        FERR("framegen: generate threw: %s", e.what());
        return nullptr;
    }
    AHardwareBuffer* pb = presentBuf[idx % 2];
    if (!copier.copy(out, pb, static_cast<VkFormat>(vkFmt), w, h)) return out;
    return pb;
}

void HostFramegen::destroy() {
    if (ctxId >= 0) { try { LSFG_3_1::deleteContext(ctxId); LSFG_3_1::finalize(); } catch (...) {} ctxId = -1; }
    copier.destroy();
    if (in0) AHardwareBuffer_release(in0);
    if (in1) AHardwareBuffer_release(in1);
    if (out) AHardwareBuffer_release(out);
    if (presentBuf[0]) AHardwareBuffer_release(presentBuf[0]);
    if (presentBuf[1]) AHardwareBuffer_release(presentBuf[1]);
    in0 = in1 = out = nullptr;
    presentBuf[0] = presentBuf[1] = nullptr;
    ready = false;
}
