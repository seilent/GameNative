#pragma once

#include <android/hardware_buffer.h>
#include <volk.h>
#include <cstdint>
#include <unordered_map>

class HostCopier;

struct HostEffectParams {
    int effectId = 0;
    float sharpness = 0.0f;
    int effectMask = 0;
    float brightness = 0.0f;
    float contrast = 0.0f;
    float gamma = 1.0f;
};

class HostEffects {
public:
    static constexpr uint32_t RING_SIZE = 4;

    bool init(HostCopier& copier, uint32_t panelW, uint32_t panelH, uint32_t ahbFormat, VkFormat vkFormat);
    AHardwareBuffer* apply(AHardwareBuffer* in, uint32_t srcW, uint32_t srcH,
                           const HostEffectParams& params);
    void destroy();

    bool isActive(const HostEffectParams& params) const;

    uint32_t panelWidth() const { return pW; }
    uint32_t panelHeight() const { return pH; }

private:
    struct RingSlot {
        AHardwareBuffer* ahb = nullptr;
        VkImage image = VK_NULL_HANDLE;
        VkDeviceMemory mem = VK_NULL_HANDLE;
        VkImageView view = VK_NULL_HANDLE;
        VkFramebuffer fb = VK_NULL_HANDLE;
    };

    struct ImportedInput {
        VkImage image = VK_NULL_HANDLE;
        VkDeviceMemory mem = VK_NULL_HANDLE;
        VkImageView view = VK_NULL_HANDLE;
    };

    bool createRenderPass();
    bool createPipeline();
    bool createSampler();
    bool createDescriptorPool();
    bool createRingSlots();
    bool importInputAhb(AHardwareBuffer* ahb, uint32_t w, uint32_t h, ImportedInput& out);
    void destroyImportedInput(ImportedInput& inp);
    void destroyRingSlots();

    HostCopier* cop = nullptr;
    VkDevice device = VK_NULL_HANDLE;
    VkQueue queue = VK_NULL_HANDLE;
    uint32_t qfam = 0;
    VkCommandPool pool = VK_NULL_HANDLE;
    VkCommandBuffer cmd = VK_NULL_HANDLE;
    VkFence fence = VK_NULL_HANDLE;
    const VolkDeviceTable* t = nullptr;
    VkPhysicalDeviceMemoryProperties memProps{};

    VkRenderPass renderPass = VK_NULL_HANDLE;
    VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
    VkPipeline pipeline = VK_NULL_HANDLE;
    VkSampler sampler = VK_NULL_HANDLE;
    VkDescriptorPool descPool = VK_NULL_HANDLE;
    VkDescriptorSetLayout descSetLayout = VK_NULL_HANDLE;
    VkDescriptorSet descSet = VK_NULL_HANDLE;

    RingSlot ring[RING_SIZE]{};
    uint32_t ringIdx = 0;

    std::unordered_map<AHardwareBuffer*, ImportedInput> inputCache;

    uint32_t pW = 0;
    uint32_t pH = 0;
    uint32_t ahbFmt_ = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
    VkFormat vkFmt_ = VK_FORMAT_R8G8B8A8_UNORM;
    bool ready = false;
};
