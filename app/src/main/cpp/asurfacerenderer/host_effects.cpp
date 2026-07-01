#include "host_effects.h"
#include "seifg_copy.h"

#include <android/log.h>
#include <cstring>
#include <cmath>

#include "host_effects_vert.h"
#include "host_effects_frag.h"

#define HELOG(...) __android_log_print(ANDROID_LOG_INFO, "asr_effects", __VA_ARGS__)
#define HEERR(...) __android_log_print(ANDROID_LOG_ERROR, "asr_effects", __VA_ARGS__)

struct EffectPushConstants {
    float ndcX0, ndcY0, ndcX1, ndcY1;
    int32_t useTexAlpha;
    int32_t effectId;
    float sharpness;
    float resW, resH;
    int32_t effectMask;
    float brightness;
    float contrast;
    float gamma;
    float outW, outH;
};

bool HostEffects::isActive(const HostEffectParams& params) const {
    return params.effectId != 0 ||
           params.effectMask != 0 ||
           std::abs(params.brightness) > 0.001f ||
           std::abs(params.contrast) > 0.001f ||
           std::abs(params.gamma - 1.0f) > 0.001f;
}

bool HostEffects::init(HostCopier& copier, uint32_t panelW, uint32_t panelH, uint32_t ahbFormat, VkFormat vkFormat) {
    if (ready && pW == panelW && pH == panelH) return true;
    if (ready) destroy();

    cop = &copier;
    device = copier.getDevice();
    queue = copier.getQueue();
    qfam = copier.getQueueFamily();
    pool = copier.getCommandPool();
    t = &copier.getTable();
    memProps = copier.getMemProps();
    pW = panelW;
    pH = panelH;
    ahbFmt_ = ahbFormat;
    vkFmt_ = vkFormat;

    if (!createRenderPass()) { HEERR("createRenderPass failed"); return false; }
    if (!createSampler()) { HEERR("createSampler failed"); return false; }
    if (!createDescriptorPool()) { HEERR("createDescriptorPool failed"); return false; }
    if (!createPipeline()) { HEERR("createPipeline failed"); return false; }
    if (!createRingSlots()) { HEERR("createRingSlots failed"); return false; }

    VkCommandBufferAllocateInfo cba{};
    cba.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    cba.commandPool = pool;
    cba.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    cba.commandBufferCount = 1;
    if (t->vkAllocateCommandBuffers(device, &cba, &cmd) != VK_SUCCESS) {
        HEERR("allocate cmd failed"); return false;
    }

    VkFenceCreateInfo fci{};
    fci.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    if (t->vkCreateFence(device, &fci, nullptr, &fence) != VK_SUCCESS) {
        HEERR("createFence failed"); return false;
    }

    ready = true;
    HELOG("init OK %ux%u ring=%u", pW, pH, RING_SIZE);
    return true;
}

bool HostEffects::createRenderPass() {
    VkAttachmentDescription att{};
    att.format = vkFmt_;
    att.samples = VK_SAMPLE_COUNT_1_BIT;
    att.loadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    att.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    att.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    att.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    att.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    att.finalLayout = VK_IMAGE_LAYOUT_GENERAL;

    VkAttachmentReference ref{};
    ref.attachment = 0;
    ref.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

    VkSubpassDescription sub{};
    sub.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    sub.colorAttachmentCount = 1;
    sub.pColorAttachments = &ref;

    VkRenderPassCreateInfo rp{};
    rp.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    rp.attachmentCount = 1;
    rp.pAttachments = &att;
    rp.subpassCount = 1;
    rp.pSubpasses = &sub;
    return t->vkCreateRenderPass(device, &rp, nullptr, &renderPass) == VK_SUCCESS;
}

bool HostEffects::createSampler() {
    VkSamplerCreateInfo sci{};
    sci.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    sci.magFilter = VK_FILTER_LINEAR;
    sci.minFilter = VK_FILTER_LINEAR;
    sci.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    sci.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sci.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sci.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sci.maxLod = 0.0f;
    return t->vkCreateSampler(device, &sci, nullptr, &sampler) == VK_SUCCESS;
}

bool HostEffects::createDescriptorPool() {
    VkDescriptorSetLayoutBinding binding{};
    binding.binding = 0;
    binding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    binding.descriptorCount = 1;
    binding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    binding.pImmutableSamplers = nullptr;

    VkDescriptorSetLayoutCreateInfo dslci{};
    dslci.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    dslci.bindingCount = 1;
    dslci.pBindings = &binding;
    if (t->vkCreateDescriptorSetLayout(device, &dslci, nullptr, &descSetLayout) != VK_SUCCESS)
        return false;

    VkDescriptorPoolSize poolSize{};
    poolSize.type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    poolSize.descriptorCount = 1;

    VkDescriptorPoolCreateInfo dpci{};
    dpci.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    dpci.maxSets = 1;
    dpci.poolSizeCount = 1;
    dpci.pPoolSizes = &poolSize;
    if (t->vkCreateDescriptorPool(device, &dpci, nullptr, &descPool) != VK_SUCCESS)
        return false;

    VkDescriptorSetAllocateInfo dsai{};
    dsai.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    dsai.descriptorPool = descPool;
    dsai.descriptorSetCount = 1;
    dsai.pSetLayouts = &descSetLayout;
    return t->vkAllocateDescriptorSets(device, &dsai, &descSet) == VK_SUCCESS;
}

bool HostEffects::createPipeline() {
    VkShaderModuleCreateInfo vertSmi{};
    vertSmi.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    vertSmi.codeSize = sizeof(host_effects_vert_code);
    vertSmi.pCode = host_effects_vert_code;
    VkShaderModule vertMod = VK_NULL_HANDLE;
    if (t->vkCreateShaderModule(device, &vertSmi, nullptr, &vertMod) != VK_SUCCESS)
        return false;

    VkShaderModuleCreateInfo fragSmi{};
    fragSmi.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    fragSmi.codeSize = sizeof(host_effects_frag_code);
    fragSmi.pCode = host_effects_frag_code;
    VkShaderModule fragMod = VK_NULL_HANDLE;
    if (t->vkCreateShaderModule(device, &fragSmi, nullptr, &fragMod) != VK_SUCCESS) {
        t->vkDestroyShaderModule(device, vertMod, nullptr);
        return false;
    }

    VkPipelineShaderStageCreateInfo stages[2]{};
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vertMod;
    stages[0].pName = "main";
    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fragMod;
    stages[1].pName = "main";

    VkPipelineVertexInputStateCreateInfo vi{};
    vi.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;

    VkPipelineInputAssemblyStateCreateInfo ia{};
    ia.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    ia.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;

    VkViewport viewport{0, 0, (float)pW, (float)pH, 0.0f, 1.0f};
    VkRect2D scissor{{0, 0}, {pW, pH}};
    VkPipelineViewportStateCreateInfo vps{};
    vps.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    vps.viewportCount = 1;
    vps.pViewports = &viewport;
    vps.scissorCount = 1;
    vps.pScissors = &scissor;

    VkPipelineRasterizationStateCreateInfo rs{};
    rs.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rs.polygonMode = VK_POLYGON_MODE_FILL;
    rs.cullMode = VK_CULL_MODE_NONE;
    rs.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    rs.lineWidth = 1.0f;

    VkPipelineMultisampleStateCreateInfo ms{};
    ms.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    ms.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineColorBlendAttachmentState cba{};
    cba.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                         VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    VkPipelineColorBlendStateCreateInfo cb{};
    cb.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    cb.attachmentCount = 1;
    cb.pAttachments = &cba;

    VkPushConstantRange pcRange{};
    pcRange.stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    pcRange.offset = 0;
    pcRange.size = sizeof(EffectPushConstants);

    VkPipelineLayoutCreateInfo plci{};
    plci.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    plci.setLayoutCount = 1;
    plci.pSetLayouts = &descSetLayout;
    plci.pushConstantRangeCount = 1;
    plci.pPushConstantRanges = &pcRange;
    if (t->vkCreatePipelineLayout(device, &plci, nullptr, &pipelineLayout) != VK_SUCCESS) {
        t->vkDestroyShaderModule(device, vertMod, nullptr);
        t->vkDestroyShaderModule(device, fragMod, nullptr);
        return false;
    }

    VkGraphicsPipelineCreateInfo gpci{};
    gpci.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    gpci.stageCount = 2;
    gpci.pStages = stages;
    gpci.pVertexInputState = &vi;
    gpci.pInputAssemblyState = &ia;
    gpci.pViewportState = &vps;
    gpci.pRasterizationState = &rs;
    gpci.pMultisampleState = &ms;
    gpci.pColorBlendState = &cb;
    gpci.layout = pipelineLayout;
    gpci.renderPass = renderPass;
    gpci.subpass = 0;

    VkResult r = t->vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &gpci, nullptr, &pipeline);
    t->vkDestroyShaderModule(device, vertMod, nullptr);
    t->vkDestroyShaderModule(device, fragMod, nullptr);
    return r == VK_SUCCESS;
}

bool HostEffects::createRingSlots() {
    for (uint32_t i = 0; i < RING_SIZE; i++) {
        AHardwareBuffer_Desc d{};
        d.width = pW; d.height = pH; d.layers = 1;
        d.format = ahbFmt_;
        d.usage = AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT | AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE;
        if (AHardwareBuffer_allocate(&d, &ring[i].ahb) != 0) return false;

        VkAndroidHardwareBufferFormatPropertiesANDROID fmtProps{};
        fmtProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID;
        VkAndroidHardwareBufferPropertiesANDROID ahbProps{};
        ahbProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;
        ahbProps.pNext = &fmtProps;
        if (t->vkGetAndroidHardwareBufferPropertiesANDROID(device, ring[i].ahb, &ahbProps) != VK_SUCCESS)
            return false;

        VkExternalMemoryImageCreateInfo ext{};
        ext.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO;
        ext.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;

        VkImageCreateInfo ici{};
        ici.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
        ici.pNext = &ext;
        ici.imageType = VK_IMAGE_TYPE_2D;
        ici.format = vkFmt_;
        ici.extent = { pW, pH, 1 };
        ici.mipLevels = 1;
        ici.arrayLayers = 1;
        ici.samples = VK_SAMPLE_COUNT_1_BIT;
        ici.tiling = VK_IMAGE_TILING_OPTIMAL;
        ici.usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
        ici.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        ici.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        if (t->vkCreateImage(device, &ici, nullptr, &ring[i].image) != VK_SUCCESS) return false;

        uint32_t memType = UINT32_MAX;
        for (uint32_t m = 0; m < memProps.memoryTypeCount; ++m)
            if (ahbProps.memoryTypeBits & (1u << m)) { memType = m; break; }
        if (memType == UINT32_MAX) return false;

        VkMemoryDedicatedAllocateInfo ded{};
        ded.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO;
        ded.image = ring[i].image;
        VkImportAndroidHardwareBufferInfoANDROID imp{};
        imp.sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID;
        imp.pNext = &ded;
        imp.buffer = ring[i].ahb;
        VkMemoryAllocateInfo mai{};
        mai.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
        mai.pNext = &imp;
        mai.allocationSize = ahbProps.allocationSize;
        mai.memoryTypeIndex = memType;
        if (t->vkAllocateMemory(device, &mai, nullptr, &ring[i].mem) != VK_SUCCESS) return false;
        if (t->vkBindImageMemory(device, ring[i].image, ring[i].mem, 0) != VK_SUCCESS) return false;

        VkImageViewCreateInfo ivci{};
        ivci.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        ivci.image = ring[i].image;
        ivci.viewType = VK_IMAGE_VIEW_TYPE_2D;
        ivci.format = vkFmt_;
        ivci.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
        if (t->vkCreateImageView(device, &ivci, nullptr, &ring[i].view) != VK_SUCCESS) return false;

        VkFramebufferCreateInfo fbci{};
        fbci.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        fbci.renderPass = renderPass;
        fbci.attachmentCount = 1;
        fbci.pAttachments = &ring[i].view;
        fbci.width = pW;
        fbci.height = pH;
        fbci.layers = 1;
        if (t->vkCreateFramebuffer(device, &fbci, nullptr, &ring[i].fb) != VK_SUCCESS) return false;
    }
    return true;
}

bool HostEffects::importInputAhb(AHardwareBuffer* ahb, uint32_t w, uint32_t h, ImportedInput& out) {
    VkAndroidHardwareBufferFormatPropertiesANDROID fmtProps{};
    fmtProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID;
    VkAndroidHardwareBufferPropertiesANDROID ahbProps{};
    ahbProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;
    ahbProps.pNext = &fmtProps;
    if (t->vkGetAndroidHardwareBufferPropertiesANDROID(device, ahb, &ahbProps) != VK_SUCCESS) return false;

    VkExternalMemoryImageCreateInfo ext{};
    ext.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO;
    ext.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;
    VkImageCreateInfo ici{};
    ici.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    ici.pNext = &ext;
    ici.imageType = VK_IMAGE_TYPE_2D;
    ici.format = vkFmt_;
    ici.extent = { w, h, 1 };
    ici.mipLevels = 1;
    ici.arrayLayers = 1;
    ici.samples = VK_SAMPLE_COUNT_1_BIT;
    ici.tiling = VK_IMAGE_TILING_OPTIMAL;
    ici.usage = VK_IMAGE_USAGE_SAMPLED_BIT;
    ici.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    ici.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    if (t->vkCreateImage(device, &ici, nullptr, &out.image) != VK_SUCCESS) return false;

    uint32_t memType = UINT32_MAX;
    for (uint32_t m = 0; m < memProps.memoryTypeCount; ++m)
        if (ahbProps.memoryTypeBits & (1u << m)) { memType = m; break; }
    if (memType == UINT32_MAX) { t->vkDestroyImage(device, out.image, nullptr); out.image = VK_NULL_HANDLE; return false; }

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
    if (t->vkAllocateMemory(device, &mai, nullptr, &out.mem) != VK_SUCCESS) {
        t->vkDestroyImage(device, out.image, nullptr); out.image = VK_NULL_HANDLE; return false;
    }
    if (t->vkBindImageMemory(device, out.image, out.mem, 0) != VK_SUCCESS) {
        destroyImportedInput(out); return false;
    }

    VkImageViewCreateInfo ivci{};
    ivci.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    ivci.image = out.image;
    ivci.viewType = VK_IMAGE_VIEW_TYPE_2D;
    ivci.format = vkFmt_;
    ivci.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
    if (t->vkCreateImageView(device, &ivci, nullptr, &out.view) != VK_SUCCESS) {
        destroyImportedInput(out); return false;
    }
    return true;
}

void HostEffects::destroyImportedInput(ImportedInput& inp) {
    if (inp.view) t->vkDestroyImageView(device, inp.view, nullptr);
    if (inp.image) t->vkDestroyImage(device, inp.image, nullptr);
    if (inp.mem) t->vkFreeMemory(device, inp.mem, nullptr);
    inp = {};
}

AHardwareBuffer* HostEffects::apply(AHardwareBuffer* in, uint32_t srcW, uint32_t srcH,
                                    const HostEffectParams& params) {
    if (!ready) return in;

    ImportedInput* input = nullptr;
    auto it = inputCache.find(in);
    if (it != inputCache.end()) {
        input = &it->second;
    } else {
        ImportedInput imp{};
        if (!importInputAhb(in, srcW, srcH, imp)) return in;
        auto [ins, _] = inputCache.emplace(in, imp);
        input = &ins->second;
    }

    RingSlot& slot = ring[ringIdx];
    ringIdx = (ringIdx + 1) % RING_SIZE;

    VkDescriptorImageInfo imgInfo{};
    imgInfo.sampler = sampler;
    imgInfo.imageView = input->view;
    imgInfo.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    VkWriteDescriptorSet write{};
    write.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    write.dstSet = descSet;
    write.dstBinding = 0;
    write.descriptorCount = 1;
    write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    write.pImageInfo = &imgInfo;
    t->vkUpdateDescriptorSets(device, 1, &write, 0, nullptr);

    t->vkResetCommandBuffer(cmd, 0);
    VkCommandBufferBeginInfo bi{};
    bi.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    t->vkBeginCommandBuffer(cmd, &bi);

    VkImageMemoryBarrier inputBarrier{};
    inputBarrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
    inputBarrier.srcAccessMask = 0;
    inputBarrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
    inputBarrier.oldLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    inputBarrier.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    inputBarrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_EXTERNAL;
    inputBarrier.dstQueueFamilyIndex = qfam;
    inputBarrier.image = input->image;
    inputBarrier.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
    t->vkCmdPipelineBarrier(cmd,
        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
        0, 0, nullptr, 0, nullptr, 1, &inputBarrier);

    VkClearValue clearVal{};
    VkRenderPassBeginInfo rpbi{};
    rpbi.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    rpbi.renderPass = renderPass;
    rpbi.framebuffer = slot.fb;
    rpbi.renderArea = {{0, 0}, {pW, pH}};
    rpbi.clearValueCount = 1;
    rpbi.pClearValues = &clearVal;
    t->vkCmdBeginRenderPass(cmd, &rpbi, VK_SUBPASS_CONTENTS_INLINE);

    t->vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
    t->vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
        pipelineLayout, 0, 1, &descSet, 0, nullptr);

    EffectPushConstants pc{};
    pc.ndcX0 = -1.0f; pc.ndcY0 = -1.0f;
    pc.ndcX1 =  1.0f; pc.ndcY1 =  1.0f;
    pc.useTexAlpha = 0;
    pc.effectId = params.effectId;
    pc.sharpness = params.sharpness;
    pc.resW = (float)srcW;
    pc.resH = (float)srcH;
    pc.effectMask = params.effectMask;
    pc.brightness = params.brightness;
    pc.contrast = params.contrast;
    pc.gamma = params.gamma;
    pc.outW = (float)pW;
    pc.outH = (float)pH;
    t->vkCmdPushConstants(cmd, pipelineLayout,
        VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
        0, sizeof(pc), &pc);

    t->vkCmdDraw(cmd, 4, 1, 0, 0);
    t->vkCmdEndRenderPass(cmd);

    VkImageMemoryBarrier outputBarrier{};
    outputBarrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
    outputBarrier.srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    outputBarrier.dstAccessMask = 0;
    outputBarrier.oldLayout = VK_IMAGE_LAYOUT_GENERAL;
    outputBarrier.newLayout = VK_IMAGE_LAYOUT_GENERAL;
    outputBarrier.srcQueueFamilyIndex = qfam;
    outputBarrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_EXTERNAL;
    outputBarrier.image = slot.image;
    outputBarrier.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
    t->vkCmdPipelineBarrier(cmd,
        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
        0, 0, nullptr, 0, nullptr, 1, &outputBarrier);

    t->vkEndCommandBuffer(cmd);

    t->vkResetFences(device, 1, &fence);
    VkSubmitInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    si.commandBufferCount = 1;
    si.pCommandBuffers = &cmd;
    t->vkQueueSubmit(queue, 1, &si, fence);
    t->vkWaitForFences(device, 1, &fence, VK_TRUE, UINT64_MAX);

    return slot.ahb;
}

void HostEffects::destroyRingSlots() {
    for (uint32_t i = 0; i < RING_SIZE; i++) {
        if (ring[i].fb) t->vkDestroyFramebuffer(device, ring[i].fb, nullptr);
        if (ring[i].view) t->vkDestroyImageView(device, ring[i].view, nullptr);
        if (ring[i].image) t->vkDestroyImage(device, ring[i].image, nullptr);
        if (ring[i].mem) t->vkFreeMemory(device, ring[i].mem, nullptr);
        if (ring[i].ahb) AHardwareBuffer_release(ring[i].ahb);
        ring[i] = {};
    }
}

void HostEffects::destroy() {
    if (!ready && !device) return;
    if (device) t->vkDeviceWaitIdle(device);
    for (auto& [_, inp] : inputCache) destroyImportedInput(inp);
    inputCache.clear();
    destroyRingSlots();
    if (fence) { t->vkDestroyFence(device, fence, nullptr); fence = VK_NULL_HANDLE; }
    if (pipeline) { t->vkDestroyPipeline(device, pipeline, nullptr); pipeline = VK_NULL_HANDLE; }
    if (pipelineLayout) { t->vkDestroyPipelineLayout(device, pipelineLayout, nullptr); pipelineLayout = VK_NULL_HANDLE; }
    if (descPool) { t->vkDestroyDescriptorPool(device, descPool, nullptr); descPool = VK_NULL_HANDLE; }
    if (descSetLayout) { t->vkDestroyDescriptorSetLayout(device, descSetLayout, nullptr); descSetLayout = VK_NULL_HANDLE; }
    if (sampler) { t->vkDestroySampler(device, sampler, nullptr); sampler = VK_NULL_HANDLE; }
    if (renderPass) { t->vkDestroyRenderPass(device, renderPass, nullptr); renderPass = VK_NULL_HANDLE; }
    cmd = VK_NULL_HANDLE;
    descSet = VK_NULL_HANDLE;
    ready = false;
    pW = pH = 0;
}
