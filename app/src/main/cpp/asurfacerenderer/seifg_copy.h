#pragma once

#include <android/hardware_buffer.h>
#include <volk.h>

#include <cstdint>
#include <unordered_map>

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
    Img* getImg(AHardwareBuffer* ahb, VkFormat format, uint32_t w, uint32_t h);
    void destroyImg(Img& i);

    VkInstance instance{};
    VkPhysicalDevice phys{};
    VkDevice device{};
    VolkDeviceTable t{};
    VkQueue queue{};
    uint32_t qfam = 0;
    VkCommandPool pool{};
    VkCommandBuffer cmd{};
    std::unordered_map<AHardwareBuffer*, Img> imgCache;
    VkPhysicalDeviceMemoryProperties memProps{};
    bool ready = false;
};
