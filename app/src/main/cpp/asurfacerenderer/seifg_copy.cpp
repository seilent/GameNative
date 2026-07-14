#include "seifg_copy.h"

#include <android/log.h>
#include <vector>

#define CLOG(...) __android_log_print(ANDROID_LOG_INFO, "seifg_copier", __VA_ARGS__)
#define CERR(...) __android_log_print(ANDROID_LOG_ERROR, "seifg_copier", __VA_ARGS__)

bool HostCopier::init(uint64_t wantUuid) {
    if (volkInitialize() != VK_SUCCESS) { CERR("copier: volkInitialize failed"); return false; }

    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo ici{};
    ici.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ici.pApplicationInfo = &app;
    if (vkCreateInstance(&ici, nullptr, &instance) != VK_SUCCESS) { CERR("copier: vkCreateInstance failed"); return false; }
    volkLoadInstance(instance);

    uint32_t n = 0;
    vkEnumeratePhysicalDevices(instance, &n, nullptr);
    std::vector<VkPhysicalDevice> devs(n);
    vkEnumeratePhysicalDevices(instance, &n, devs.data());
    for (auto d : devs) {
        VkPhysicalDeviceProperties p{};
        vkGetPhysicalDeviceProperties(d, &p);
        uint64_t uuid = (static_cast<uint64_t>(p.vendorID) << 32) | p.deviceID;
        if (uuid == wantUuid) { phys = d; break; }
    }
    if (phys == VK_NULL_HANDLE && !devs.empty()) phys = devs.at(0);
    if (phys == VK_NULL_HANDLE) { CERR("copier: no physical device"); return false; }

    vkGetPhysicalDeviceMemoryProperties(phys, &memProps);

    uint32_t fc = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(phys, &fc, nullptr);
    std::vector<VkQueueFamilyProperties> fams(fc);
    vkGetPhysicalDeviceQueueFamilyProperties(phys, &fc, fams.data());
    bool found = false;
    for (uint32_t i = 0; i < fc; ++i) {
        if (fams[i].queueFlags & (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_TRANSFER_BIT)) { qfam = i; found = true; break; }
    }
    if (!found) { CERR("copier: no graphics/transfer queue"); return false; }

    const float prio = 1.0F;
    VkDeviceQueueCreateInfo qci{};
    qci.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    qci.queueFamilyIndex = qfam;
    qci.queueCount = 1;
    qci.pQueuePriorities = &prio;

    const char* exts[] = {
        "VK_ANDROID_external_memory_android_hardware_buffer",
        "VK_KHR_external_memory",
        "VK_KHR_sampler_ycbcr_conversion",
        "VK_KHR_dedicated_allocation",
        "VK_KHR_bind_memory2",
        "VK_KHR_get_memory_requirements2",
        "VK_KHR_maintenance1",
    };
    VkDeviceCreateInfo dci{};
    dci.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    dci.queueCreateInfoCount = 1;
    dci.pQueueCreateInfos = &qci;
    dci.enabledExtensionCount = sizeof(exts) / sizeof(exts[0]);
    dci.ppEnabledExtensionNames = exts;
    if (vkCreateDevice(phys, &dci, nullptr, &device) != VK_SUCCESS) { CERR("copier: vkCreateDevice failed"); return false; }

    volkLoadDeviceTable(&t, device);
    t.vkGetDeviceQueue(device, qfam, 0, &queue);

    VkCommandPoolCreateInfo pci{};
    pci.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    pci.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    pci.queueFamilyIndex = qfam;
    if (t.vkCreateCommandPool(device, &pci, nullptr, &pool) != VK_SUCCESS) { CERR("copier: vkCreateCommandPool failed"); return false; }

    ready = true;
    CLOG("copier: init OK (qfam=%u)", qfam);
    return true;
}

bool HostCopier::importImage(AHardwareBuffer* ahb, VkFormat format,
        uint32_t w, uint32_t h, VkImageUsageFlags usage, Img& out) {
    VkAndroidHardwareBufferFormatPropertiesANDROID fmtProps{};
    fmtProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID;
    VkAndroidHardwareBufferPropertiesANDROID ahbProps{};
    ahbProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;
    ahbProps.pNext = &fmtProps;
    if (t.vkGetAndroidHardwareBufferPropertiesANDROID(device, ahb, &ahbProps) != VK_SUCCESS) {
        CERR("copier: GetAhbProperties failed"); return false;
    }

    VkExternalMemoryImageCreateInfo ext{};
    ext.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO;
    ext.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;
    VkImageCreateInfo ici{};
    ici.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    ici.pNext = &ext;
    ici.imageType = VK_IMAGE_TYPE_2D;
    ici.format = format;
    ici.extent = { w, h, 1 };
    ici.mipLevels = 1;
    ici.arrayLayers = 1;
    ici.samples = VK_SAMPLE_COUNT_1_BIT;
    ici.tiling = VK_IMAGE_TILING_OPTIMAL;
    ici.usage = usage;
    ici.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    ici.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    if (t.vkCreateImage(device, &ici, nullptr, &out.image) != VK_SUCCESS) { CERR("copier: vkCreateImage failed"); return false; }

    uint32_t memType = UINT32_MAX;
    for (uint32_t i = 0; i < memProps.memoryTypeCount; ++i)
        if (ahbProps.memoryTypeBits & (1u << i)) { memType = i; break; }
    if (memType == UINT32_MAX) { CERR("copier: no AHB memory type"); t.vkDestroyImage(device, out.image, nullptr); out.image = VK_NULL_HANDLE; return false; }

    VkMemoryDedicatedAllocateInfo ded{};
    ded.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO;
    ded.image = out.image;
    VkImportAndroidHardwareBufferInfoANDROID imp{};
    imp.sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID;
    imp.pNext = &ded;
    imp.buffer = ahb;
    VkMemoryAllocateInfo mai{};
    mai.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    mai.pNext = &imp;
    mai.allocationSize = ahbProps.allocationSize;
    mai.memoryTypeIndex = memType;
    if (t.vkAllocateMemory(device, &mai, nullptr, &out.mem) != VK_SUCCESS) { CERR("copier: import alloc failed"); t.vkDestroyImage(device, out.image, nullptr); out.image = VK_NULL_HANDLE; return false; }
    if (t.vkBindImageMemory(device, out.image, out.mem, 0) != VK_SUCCESS) { CERR("copier: bind failed"); destroyImg(out); return false; }
    return true;
}

void HostCopier::destroyImg(Img& i) {
    if (i.image) t.vkDestroyImage(device, i.image, nullptr);
    if (i.mem) t.vkFreeMemory(device, i.mem, nullptr);
    i.image = VK_NULL_HANDLE; i.mem = VK_NULL_HANDLE;
}

HostCopier::Img* HostCopier::getImg(AHardwareBuffer* ahb, VkFormat format, uint32_t w, uint32_t h) {
    auto it = imgCache.find(ahb);
    if (it != imgCache.end()) return &it->second;
    Img img{};
    if (!importImage(ahb, format, w, h,
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT, img))
        return nullptr;
    auto res = imgCache.emplace(ahb, img);
    return &res.first->second;
}

bool HostCopier::copy(AHardwareBuffer* src, AHardwareBuffer* dst,
        VkFormat format, uint32_t w, uint32_t h) {
    if (!ready) return false;
    Img* s = getImg(src, format, w, h);
    Img* d = getImg(dst, format, w, h);
    if (!s || !d) return false;

    if (cmd == VK_NULL_HANDLE) {
        VkCommandBufferAllocateInfo cba{};
        cba.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
        cba.commandPool = pool;
        cba.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        cba.commandBufferCount = 1;
        t.vkAllocateCommandBuffers(device, &cba, &cmd);
    }
    t.vkResetCommandBuffer(cmd, 0);

    VkCommandBufferBeginInfo bi{};
    bi.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    t.vkBeginCommandBuffer(cmd, &bi);

    auto barrier = [&](VkImage img, VkImageLayout from, VkImageLayout to,
                       VkAccessFlags srcA, VkAccessFlags dstA) {
        VkImageMemoryBarrier b{};
        b.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        b.oldLayout = from; b.newLayout = to;
        b.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        b.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        b.image = img;
        b.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
        b.srcAccessMask = srcA; b.dstAccessMask = dstA;
        t.vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
            0, 0, nullptr, 0, nullptr, 1, &b);
    };
    barrier(s->image, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, 0, VK_ACCESS_TRANSFER_READ_BIT);
    barrier(d->image, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 0, VK_ACCESS_TRANSFER_WRITE_BIT);

    VkImageCopy region{};
    region.srcSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
    region.dstSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
    region.extent = { w, h, 1 };
    t.vkCmdCopyImage(cmd, s->image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
        d->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);

    t.vkEndCommandBuffer(cmd);
    VkSubmitInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    si.commandBufferCount = 1;
    si.pCommandBuffers = &cmd;
    t.vkQueueSubmit(queue, 1, &si, VK_NULL_HANDLE);
    t.vkQueueWaitIdle(queue);
    return true;
}

bool HostCopier::initShared(VkPhysicalDevice p, VkDevice dev, VkQueue q, uint32_t qf) {
    phys = p;
    device = dev;
    queue = q;
    qfam = qf;
    sharedDevice = true;
    volkLoadDeviceTable(&t, device);
    vkGetPhysicalDeviceMemoryProperties(phys, &memProps);
    VkCommandPoolCreateInfo pci{};
    pci.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    pci.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    pci.queueFamilyIndex = qfam;
    if (t.vkCreateCommandPool(device, &pci, nullptr, &pool) != VK_SUCCESS) { CERR("copier: shared vkCreateCommandPool failed"); return false; }
    ready = true;
    CLOG("copier: initShared OK (qfam=%u)", qfam);
    return true;
}

bool HostCopier::submitCopies(const CopyPair* pairs, uint32_t count, uint32_t cmdSlot,
        VkFormat format, uint32_t w, uint32_t h,
        VkSemaphore sem, uint64_t waitValue, uint64_t signalValue) {
    if (!ready || cmdSlot >= 2) return false;
    VkCommandBuffer cb = cmds[cmdSlot];
    if (cb == VK_NULL_HANDLE) {
        VkCommandBufferAllocateInfo cba{};
        cba.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
        cba.commandPool = pool;
        cba.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        cba.commandBufferCount = 1;
        if (t.vkAllocateCommandBuffers(device, &cba, &cb) != VK_SUCCESS) return false;
        cmds[cmdSlot] = cb;
    }
    t.vkResetCommandBuffer(cb, 0);
    VkCommandBufferBeginInfo bi{};
    bi.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    t.vkBeginCommandBuffer(cb, &bi);
    for (uint32_t p = 0; p < count; p++) {
        Img* s = getImg(pairs[p].src, format, w, h);
        Img* d = getImg(pairs[p].dst, format, w, h);
        if (!s || !d) { t.vkEndCommandBuffer(cb); return false; }
        auto barrier = [&](VkImage img, VkImageLayout to, VkAccessFlags dstA) {
            VkImageMemoryBarrier b{};
            b.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
            b.oldLayout = VK_IMAGE_LAYOUT_UNDEFINED; b.newLayout = to;
            b.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            b.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            b.image = img;
            b.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
            b.srcAccessMask = 0; b.dstAccessMask = dstA;
            t.vkCmdPipelineBarrier(cb, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                0, 0, nullptr, 0, nullptr, 1, &b);
        };
        barrier(s->image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_ACCESS_TRANSFER_READ_BIT);
        barrier(d->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_ACCESS_TRANSFER_WRITE_BIT);
        VkImageCopy region{};
        region.srcSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        region.dstSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        region.extent = { w, h, 1 };
        t.vkCmdCopyImage(cb, s->image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            d->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
    }
    t.vkEndCommandBuffer(cb);

    const bool doWait = (waitValue > 0);
    uint64_t waitVals[1] = { waitValue };
    uint64_t signalVals[1] = { signalValue };
    VkTimelineSemaphoreSubmitInfo tsi{};
    tsi.sType = VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO;
    tsi.waitSemaphoreValueCount = doWait ? 1u : 0u;
    tsi.pWaitSemaphoreValues = doWait ? waitVals : nullptr;
    tsi.signalSemaphoreValueCount = 1;
    tsi.pSignalSemaphoreValues = signalVals;
    VkPipelineStageFlags waitStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
    VkSubmitInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    si.pNext = &tsi;
    si.waitSemaphoreCount = doWait ? 1u : 0u;
    si.pWaitSemaphores = doWait ? &sem : nullptr;
    si.pWaitDstStageMask = doWait ? &waitStage : nullptr;
    si.commandBufferCount = 1;
    si.pCommandBuffers = &cb;
    si.signalSemaphoreCount = 1;
    si.pSignalSemaphores = &sem;
    return t.vkQueueSubmit(queue, 1, &si, VK_NULL_HANDLE) == VK_SUCCESS;
}

bool HostCopier::waitTimeline(VkSemaphore sem, uint64_t value) {
    VkSemaphoreWaitInfo wi{};
    wi.sType = VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO;
    wi.semaphoreCount = 1;
    wi.pSemaphores = &sem;
    wi.pValues = &value;
    return t.vkWaitSemaphores(device, &wi, UINT64_MAX) == VK_SUCCESS;
}

void HostCopier::destroy() {
    if (!ready) return;
    for (auto& kv : imgCache) destroyImg(kv.second);
    imgCache.clear();
    if (pool) t.vkDestroyCommandPool(device, pool, nullptr);
    if (!sharedDevice) {
        if (device) t.vkDestroyDevice(device, nullptr);
        if (instance) vkDestroyInstance(instance, nullptr);
    }
    cmd = VK_NULL_HANDLE;
    cmds[0] = VK_NULL_HANDLE;
    cmds[1] = VK_NULL_HANDLE;
    pool = VK_NULL_HANDLE;
    queue = VK_NULL_HANDLE;
    device = VK_NULL_HANDLE;
    phys = VK_NULL_HANDLE;
    instance = VK_NULL_HANDLE;
    sharedDevice = false;
    ready = false;
}
