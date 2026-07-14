#pragma once

#include <android/hardware_buffer.h>
#include <volk.h>

#include <cstdint>
#include <unordered_map>

class HostCopier {
public:
    bool init(uint64_t wantUuid);
    bool initShared(VkPhysicalDevice phys, VkDevice device, VkQueue queue, uint32_t queueFamily);
    bool copy(AHardwareBuffer* src, AHardwareBuffer* dst,
              VkFormat format, uint32_t width, uint32_t height);
    struct CopyPair { AHardwareBuffer* src; AHardwareBuffer* dst; };
    bool submitCopies(const CopyPair* pairs, uint32_t count, uint32_t cmdSlot,
                      VkFormat format, uint32_t width, uint32_t height,
                      VkSemaphore sem, uint64_t waitValue, uint64_t signalValue);
    bool waitTimeline(VkSemaphore sem, uint64_t value);
    void destroy();
    bool ok() const { return ready; }

    VkDevice getDevice() const { return device; }
    VkQueue getQueue() const { return queue; }
    uint32_t getQueueFamily() const { return qfam; }
    const VolkDeviceTable& getTable() const { return t; }
    VkCommandPool getCommandPool() const { return pool; }
    const VkPhysicalDeviceMemoryProperties& getMemProps() const { return memProps; }
    VkPhysicalDevice getPhysicalDevice() const { return phys; }

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
    VkCommandBuffer cmds[2]{};
    bool sharedDevice = false;
    std::unordered_map<AHardwareBuffer*, Img> imgCache;
    VkPhysicalDeviceMemoryProperties memProps{};
    bool ready = false;
};
