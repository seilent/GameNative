#pragma once

#include <android/hardware_buffer.h>

#include <cstdint>
#include <string>

#include "lsfg_copy.h"

// Host-side LSFG frame generation. Holds the engine context + ping-pong input
// AHBs, copies each incoming real frame into the engine inputs, and generates
// one interpolated frame between the two most recent real frames.
//
// Usage per real frame:
//   AHardwareBuffer* interp = fg.submit(incomingRealAhb);
//   if (interp) { present interp, then present incomingRealAhb (serialized) }
//   else        { present incomingRealAhb only (first frame) }
class HostFramegen {
public:
    bool init(const std::string& dllPath, uint32_t width, uint32_t height, uint32_t ahbFormat);
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
    bool ready = false;
};
