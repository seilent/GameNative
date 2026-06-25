#pragma once

#include <atomic>
#include <cstdint>
#include <mutex>
#include <thread>

struct ALooper;

// Tracks the host display's real vsync timeline via AChoreographer on a
// dedicated Looper thread. Vortek presents asynchronously inside the container
// (no vsync visible to the guest), so frame pacing must happen host-side and be
// anchored to the true vsync grid. This exposes the latest vsync timestamp and
// the measured period so the scanout path can align ASurfaceTransaction
// desiredPresentTime to actual vsync slots instead of a free-running clock.
class VsyncClock {
public:
    VsyncClock() = default;
    ~VsyncClock();

    void start();
    void stop();

    // Latest real vsync timestamp (CLOCK_MONOTONIC ns); 0 until warmed up.
    int64_t lastVsyncNs() const { return lastVsync.load(std::memory_order_relaxed); }
    // Measured vsync period (ns); 0 until warmed up.
    int64_t periodNs() const { return period.load(std::memory_order_relaxed); }

    VsyncClock(const VsyncClock&) = delete;
    VsyncClock& operator=(const VsyncClock&) = delete;

private:
    void threadMain();
    static void frameCallback(int64_t frameTimeNanos, void* data);
    void onVsync(int64_t frameTimeNanos);

    std::thread thread;
    std::mutex lifecycleMutex;
    std::atomic<bool> running{false};
    std::atomic<ALooper*> looper{nullptr};

    std::atomic<int64_t> lastVsync{0};
    std::atomic<int64_t> period{0};
    int64_t prevVsync = 0;
    int64_t vsyncCount = 0;

    void* fnGetInstance = nullptr;
    void* fnPostFrameCallback64 = nullptr;
    void* choreographer = nullptr;
};
