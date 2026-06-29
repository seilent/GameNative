#include <jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <volk.h>

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

#include "lsfg_3_1.hpp"
#include "extract/extract.hpp"
#include "extract/trans.hpp"
#include "config/config.hpp"

#define SPIKE_LOG(...) __android_log_print(ANDROID_LOG_INFO, "lsfg_host", __VA_ARGS__)
#define SPIKE_ERR(...) __android_log_print(ANDROID_LOG_ERROR, "lsfg_host", __VA_ARGS__)

namespace {

uint64_t enumerateDeviceUUID() {
    if (volkInitialize() != VK_SUCCESS) {
        SPIKE_ERR("volkInitialize failed");
        return 0;
    }
    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &app;
    VkInstance inst = VK_NULL_HANDLE;
    if (vkCreateInstance(&ci, nullptr, &inst) != VK_SUCCESS) {
        SPIKE_ERR("vkCreateInstance failed");
        return 0;
    }
    volkLoadInstance(inst);
    uint32_t count = 0;
    vkEnumeratePhysicalDevices(inst, &count, nullptr);
    if (count == 0) {
        SPIKE_ERR("no physical devices");
        vkDestroyInstance(inst, nullptr);
        return 0;
    }
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
            | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT;
    AHardwareBuffer* ahb = nullptr;
    if (AHardwareBuffer_allocate(&d, &ahb) != 0) {
        SPIKE_ERR("AHardwareBuffer_allocate failed (%ux%u)", w, h);
        return nullptr;
    }
    return ahb;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_renderer_ASurfaceRenderer_nativeLsfgHostSpike(
        JNIEnv* env, jobject, jstring jDllPath) {
    const char* dll = env->GetStringUTFChars(jDllPath, nullptr);
    SPIKE_LOG("=== Phase 0 host engine spike start; dll=%s ===", dll ? dll : "(null)");
    Config::activeConf.dll = dll ? dll : "";
    if (dll) env->ReleaseStringUTFChars(jDllPath, dll);

    const uint64_t uuid = enumerateDeviceUUID();
    if (uuid == 0) { SPIKE_ERR("no device uuid; abort"); return; }

    const uint32_t W = 1280, H = 720;
    AHardwareBuffer* in0 = allocAhb(W, H);
    AHardwareBuffer* in1 = allocAhb(W, H);
    AHardwareBuffer* out = allocAhb(W, H);
    if (!in0 || !in1 || !out) { SPIKE_ERR("AHB alloc failed; abort"); return; }

    auto loader = [](const std::string& name) -> std::vector<uint8_t> {
        auto dxbc = Extract::getShader(name);
        return Extract::translateShader(dxbc);
    };

    try {
        SPIKE_LOG("extractShaders()...");
        Extract::extractShaders();
        SPIKE_LOG("extractShaders() OK");

        SPIKE_LOG("initialize()...");
        LSFG_3_1::initialize(uuid, false, 1.0F / 0.30F, 1, loader);
        SPIKE_LOG("initialize() OK");

        SPIKE_LOG("createContextFromAHB()...");
        int32_t ctxId = LSFG_3_1::createContextFromAHB(
                in0, in1, { out }, VkExtent2D{ W, H }, VK_FORMAT_R8G8B8A8_UNORM);
        SPIKE_LOG("createContextFromAHB() OK id=%d", ctxId);

        SPIKE_LOG("presentContext()...");
        LSFG_3_1::presentContext(ctxId, -1, {});
        SPIKE_LOG("presentContext() OK");

        SPIKE_LOG("waitIdle()...");
        LSFG_3_1::waitIdle();
        SPIKE_LOG("waitIdle() OK");

        LSFG_3_1::deleteContext(ctxId);
        LSFG_3_1::finalize();
        SPIKE_LOG("=== Phase 0 host engine spike SUCCESS ===");
    } catch (const std::exception& e) {
        SPIKE_ERR("spike threw: %s", e.what());
    } catch (...) {
        SPIKE_ERR("spike threw unknown exception");
    }

    AHardwareBuffer_release(in0);
    AHardwareBuffer_release(in1);
    AHardwareBuffer_release(out);
}
