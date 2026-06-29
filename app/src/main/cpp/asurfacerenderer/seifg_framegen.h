#pragma once

#include <android/hardware_buffer.h>
#include <cstdint>

#include "seifg_copy.h"

class HostFramegen {
public:
    bool init(uint32_t width, uint32_t height, uint32_t ahbFormat,
              float flowScale, bool performanceMode);
    AHardwareBuffer* submit(AHardwareBuffer* incoming);
    void destroy();
    bool ok() const { return ready; }
    uint32_t width() const { return w; }
    uint32_t height() const { return h; }

private:
    HostCopier copier;
    int32_t ctxId = -1;
    AHardwareBuffer* in0 = nullptr;
    AHardwareBuffer* in1 = nullptr;
    AHardwareBuffer* out = nullptr;
    AHardwareBuffer* presentBuf[2] = { nullptr, nullptr };
    uint64_t frameIdx = 0;
    uint32_t w = 0;
    uint32_t h = 0;
    uint32_t ahbFmt = 0;
    int vkFmt = 0;
    float flowScale = 0.30F;
    bool perf = false;
    bool ready = false;
};
