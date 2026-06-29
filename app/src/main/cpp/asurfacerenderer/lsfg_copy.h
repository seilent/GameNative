#pragma once

#include <android/hardware_buffer.h>
#include <volk.h>

#include <cstdint>

// Minimal standalone Vulkan device used by the host renderer to copy an
// incoming real-frame AHardwareBuffer into the LSFG engine's fixed input
// AHardwareBuffers (vkCmdCopyImage). Uses a per-device volk table so it does
// not disturb the framegen engine's own (global) volk dispatch.
class HostCopier {
public:
    bool init(uint64_t wantUuid);
    bool copy(AHardwareBuffer* src, AHardwareBuffer* dst,
              VkFormat format, uint32_t width, uint32_t height);
    void destroy();
    bool ok() const { return ready; }

private:
    struct Img { VkImage image{}; VkDeviceMemory mem{}; };
    bool importImage(AHardwareBuffer* ahb, VkFormat format,
                     uint32_t w, uint32_t h, VkImageUsageFlags usage, Img& out);
    void destroyImg(Img& i);

    VkInstance instance{};
    VkPhysicalDevice phys{};
    VkDevice device{};
    VolkDeviceTable t{};
    VkQueue queue{};
    uint32_t qfam = 0;
    VkCommandPool pool{};
    VkPhysicalDeviceMemoryProperties memProps{};
    bool ready = false;
};
