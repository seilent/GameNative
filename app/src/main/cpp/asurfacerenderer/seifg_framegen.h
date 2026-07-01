#pragma once

#include <android/hardware_buffer.h>
#include <cstdint>

#include "seifg_copy.h"

class HostFramegen {
public:
    static constexpr uint32_t MAX_INTERPS = 3;
    bool init(uint32_t width, uint32_t height, uint32_t ahbFormat,
              uint32_t quality, uint32_t multiplier);
    uint32_t submit(AHardwareBuffer* incoming, AHardwareBuffer** outInterps);
    void destroy();
    bool ok() const { return ready; }
    uint32_t width() const { return w; }
    uint32_t height() const { return h; }
    HostCopier& getCopier() { return copier; }
    uint32_t ahbFormat() const { return ahbFmt; }
    VkFormat vkFormat() const { return static_cast<VkFormat>(vkFmt); }

private:
    HostCopier copier;
    int32_t ctxId = -1;
    AHardwareBuffer* in0 = nullptr;
    AHardwareBuffer* in1 = nullptr;
    AHardwareBuffer* outAhb[MAX_INTERPS] = { nullptr, nullptr, nullptr };
    AHardwareBuffer* presentBuf[MAX_INTERPS][2] = {};
    uint64_t frameIdx = 0;
    uint32_t w = 0;
    uint32_t h = 0;
    uint32_t ahbFmt = 0;
    int vkFmt = 0;
    uint32_t quality = 2;
    uint32_t numInterps = 1;
    bool ready = false;
};
