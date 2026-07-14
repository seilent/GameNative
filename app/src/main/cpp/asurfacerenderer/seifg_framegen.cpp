#include "seifg_framegen.h"

#include <android/log.h>
#include <volk.h>
#include <functional>
#include <vector>

#include "seifg.h"

#define FLOG(...) __android_log_print(ANDROID_LOG_INFO, "seifg_host", __VA_ARGS__)
#define FERR(...) __android_log_print(ANDROID_LOG_ERROR, "seifg_host", __VA_ARGS__)

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
        case 5: return VK_FORMAT_B8G8R8A8_UNORM;
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

}

bool HostFramegen::init(uint32_t width, uint32_t height, uint32_t ahbFormat, uint32_t quality_, uint32_t multiplier, uint32_t flowDownscale) {
    frameIdx = 0;
    tv = 0;
    w = width; h = height; ahbFmt = ahbFormat; vkFmt = ahbToVk(ahbFormat);
    quality = quality_ > 4 ? 4 : quality_;
    if (multiplier < 2) multiplier = 2;
    if (multiplier > MAX_INTERPS + 1) multiplier = MAX_INTERPS + 1;
    numInterps = multiplier - 1;
    const uint64_t uuid = enumUuid();
    if (uuid == 0) { FERR("no device uuid"); return false; }
    seifg::initialize(uuid, false, quality, (uint64_t)multiplier, {});
    if (flowDownscale > 1) seifg::setFlowDownscale(flowDownscale);
    if (!copier.initShared(seifg::getPhysicalDevice(), seifg::getDevice(), seifg::getQueue(), seifg::getQueueFamily())) { FERR("copier initShared failed"); return false; }
    VkSemaphoreTypeCreateInfo stci{};
    stci.sType = VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO;
    stci.semaphoreType = VK_SEMAPHORE_TYPE_TIMELINE;
    stci.initialValue = 0;
    VkSemaphoreCreateInfo sci{};
    sci.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
    sci.pNext = &stci;
    if (vkCreateSemaphore(seifg::getDevice(), &sci, nullptr, &timelineSem) != VK_SUCCESS) { FERR("timeline sem create failed"); return false; }
    in0 = allocAhb(w, h, ahbFmt);
    in1 = allocAhb(w, h, ahbFmt);
    if (!in0 || !in1) { FERR("AHB alloc failed"); return false; }
    std::vector<AHardwareBuffer*> outs(numInterps);
    for (uint32_t i = 0; i < numInterps; i++) {
        outAhb[i] = allocAhb(w, h, ahbFmt);
        presentBuf[i][0] = allocAhb(w, h, ahbFmt);
        presentBuf[i][1] = allocAhb(w, h, ahbFmt);
        if (!outAhb[i] || !presentBuf[i][0] || !presentBuf[i][1]) { FERR("AHB alloc failed"); return false; }
        outs[i] = outAhb[i];
    }
    ctxId = seifg::createContextFromAHB(in0, in1, outs, VkExtent2D{w, h}, static_cast<VkFormat>(vkFmt));
    if (ctxId < 0) { FERR("createContext failed"); return false; }
    ready = true;
    FLOG("init OK %ux%u fmt=%u vk=%d quality=%u mult=%u ctx=%d", w, h, ahbFmt, vkFmt, quality, multiplier, ctxId);
    return true;
}

uint32_t HostFramegen::submit(AHardwareBuffer* incoming, AHardwareBuffer** outInterps) {
    if (!ready) return 0;
    const VkFormat fmt = static_cast<VkFormat>(vkFmt);
    if (frameIdx == 0) {
        const uint64_t v = tv + 1;
        HostCopier::CopyPair prime{ incoming, in1 };
        if (copier.submitCopies(&prime, 1, 0, fmt, w, h, timelineSem, 0, v)) {
            copier.waitTimeline(timelineSem, v);
            tv = v;
        }
        frameIdx++;
        return 0;
    }
    const uint64_t inputVal = tv + 1;
    const uint64_t interpVal = tv + 2;
    const uint64_t outputVal = tv + 3;
    HostCopier::CopyPair inPairs[2] = { { in1, in0 }, { incoming, in1 } };
    if (!copier.submitCopies(inPairs, 2, 0, fmt, w, h, timelineSem, 0, inputVal)) { FERR("input copies failed"); return 0; }
    const uint64_t idx = frameIdx++;
    seifg::presentContextTimeline(ctxId, timelineSem, inputVal, interpVal);
    HostCopier::CopyPair outPairs[MAX_INTERPS];
    for (uint32_t i = 0; i < numInterps; i++) {
        outPairs[i].src = outAhb[i];
        outPairs[i].dst = presentBuf[i][idx % 2];
    }
    const bool outOk = copier.submitCopies(outPairs, numInterps, 1, fmt, w, h, timelineSem, interpVal, outputVal);
    copier.waitTimeline(timelineSem, outOk ? outputVal : interpVal);
    for (uint32_t i = 0; i < numInterps; i++)
        outInterps[i] = outOk ? presentBuf[i][idx % 2] : outAhb[i];
    tv = outOk ? outputVal : interpVal;
    return numInterps;
}

void HostFramegen::destroy() {
    seifg::waitIdle();
    if (timelineSem != VK_NULL_HANDLE && seifg::getDevice() != VK_NULL_HANDLE)
        vkDestroySemaphore(seifg::getDevice(), timelineSem, nullptr);
    timelineSem = VK_NULL_HANDLE;
    copier.destroy();
    if (ctxId >= 0) { seifg::deleteContext(ctxId); seifg::finalize(); ctxId = -1; }
    if (in0) AHardwareBuffer_release(in0);
    if (in1) AHardwareBuffer_release(in1);
    for (uint32_t i = 0; i < MAX_INTERPS; i++) {
        if (outAhb[i]) AHardwareBuffer_release(outAhb[i]);
        if (presentBuf[i][0]) AHardwareBuffer_release(presentBuf[i][0]);
        if (presentBuf[i][1]) AHardwareBuffer_release(presentBuf[i][1]);
        outAhb[i] = nullptr;
        presentBuf[i][0] = presentBuf[i][1] = nullptr;
    }
    in0 = in1 = nullptr;
    ready = false;
}
