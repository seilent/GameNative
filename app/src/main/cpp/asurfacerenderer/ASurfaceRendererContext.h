#pragma once

#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/native_window.h>
#include <android/rect.h>
#include <jni.h>

#include <atomic>
#include <mutex>
#include <memory>
#include <thread>
#include <condition_variable>

#include <unordered_map>
#include <functional>
#include <string>

#include "VsyncClock.h"
#include "seifg_framegen.h"
#include "host_effects.h"

#define WLOG_TAG "asr_renderer"
#define RLOG(...) __android_log_print(ANDROID_LOG_INFO, WLOG_TAG, __VA_ARGS__)
#define RLOG_E(...) __android_log_print(ANDROID_LOG_ERROR, WLOG_TAG, __VA_ARGS__)
#define SCANOUT_LOG(...) __android_log_print(ANDROID_LOG_INFO, "asr_scanout", __VA_ARGS__)

// Strong Callback Reference target that automatically manages the JNI GlobalRef
struct CallbackTarget {
    JavaVM* vm = nullptr;
    jobject globalRef = nullptr;
    jmethodID methodId = nullptr;
    ~CallbackTarget();
};

class ASurfaceRendererContext {
public:
    ASurfaceRendererContext(ANativeWindow* window, int cWidth, int cHeight);
    ~ASurfaceRendererContext();

    bool reattachSurface(ANativeWindow* newWindow);

    // -------------------------------------------------------------------------
    // SurfaceControl / Scanout APIs
    // -------------------------------------------------------------------------
    void initScanout();
    void destroyScanout();

    void scanoutSetDst(int x, int y, int w, int h);
    void scanoutSetCursorImage(void* pixels, short w, short h, short stride);
    void scanoutSetCursorPos(short x, short y, short hotX, short hotY, bool cursorVisible);

    // SC Management
    void registerWindowSC(int64_t contentId, const char* debugName = "(x11_window)");
    void unregisterWindowSC(int64_t contentId);
    void setWindowBuffer(int64_t contentId, AHardwareBuffer* ahb, int fenceFd,
                         int64_t windowId = 0, int64_t serial = 0);
    void setScanoutPacing(int64_t intervalNs);
    void setHostFramegen(bool enabled, int quality, int multiplier);
    void setHostEffect(int effectId, float sharpness, int effectMask,
                       float brightness, float contrast, float gamma);

    void scanoutSetCursorVisibility(bool visible);
    void applyCursorGeometry(short x, short y, short hotX, short hotY, bool cursorVisible);

    // Surface Transactions
    void beginTransaction();
    void applyTransaction();
    void updateWindow(int64_t contentId, bool visible, int zOrder,
          int srcL, int srcT, int srcR, int srcB,
          int dstL, int dstT, int dstR, int dstB);

    void setSfCallbackTarget(JNIEnv* env, jobject rendererObj);

    // -------------------------------------------------------------------------
    // State Flags
    // -------------------------------------------------------------------------
    std::atomic<bool> scanoutActive{false};

    // -------------------------------------------------------------------------
    // JNI State
    // -------------------------------------------------------------------------
    JavaVM* javaVm = nullptr;

private:
    std::mutex windowScMutex;
    std::unordered_map<int64_t, void*> windowScMap;

    HostFramegen hostFg;
    std::atomic<bool> hostFgEnabled{false};
    std::mutex hostFgMutex;
    int hostFgQuality = 2;
    int hostFgMult = 2;

    HostEffects hostEffects;
    std::atomic<int> hostEffectId{0};
    std::atomic<float> hostEffectSharpness{0.0f};
    std::atomic<int> hostEffectMask{0};
    std::atomic<float> hostEffectBrightness{0.0f};
    std::atomic<float> hostEffectContrast{0.0f};
    std::atomic<float> hostEffectGamma{1.0f};
    bool hostEffectsGeometrySet = false;
    ARect lastGameSrcRect{};
    ARect lastGameDstRect{};
    bool lastGameGeoValid = false;
    void hostFramegenPresent(void* sc, AHardwareBuffer* ahb, int fenceFd,
                             int64_t windowId, int64_t serial);
    void presentOne(void* sc, AHardwareBuffer* ahb, int fenceFd,
                    int64_t windowId, int64_t serial, int64_t target);
    int64_t nextVsyncSlot();

    struct PendingHostFrame {
        int64_t contentId = 0;
        AHardwareBuffer* ahb = nullptr;
        int fenceFd = -1;
        int64_t windowId = 0;
        int64_t serial = 0;
    };
    PendingHostFrame hostFgPending;
    bool hostFgPendingValid = false;
    std::mutex hostFgQueueMutex;
    std::condition_variable hostFgQueueCv;
    std::thread hostFgThread;
    std::atomic<bool> hostFgThreadRunning{false};
    void enqueueHostFrame(int64_t contentId, AHardwareBuffer* ahb, int fenceFd,
                          int64_t windowId, int64_t serial);
    void hostFramegenThreadLoop();
    void startHostFramegenThread();
    void stopHostFramegenThread();

    void* currentTx = nullptr;

    ANativeWindow* window;
    int surfaceWidth, surfaceHeight, containerWidth, containerHeight;
    short lastRawCursorX=-1, lastRawCursorY=-1, lastRawHotX=0, lastRawHotY=0;

    // -------------------------------------------------------------------------
    // Scanout Handles & Geometry
    // -------------------------------------------------------------------------
    void* scanoutCursorSC = nullptr;

    AHardwareBuffer* scanoutCursorBuf = nullptr;
    int32_t scanoutCursorBufW = 0;
    int32_t scanoutCursorBufH = 0;
    int     scanoutCursorFence = -1;

    std::atomic<int64_t> scanoutDstXY{0}; // x<<32 | y (both as uint32)
    std::atomic<int64_t> scanoutDstWH{0}; // w<<32 | h (both as uint32)

    // Dynamic SurfaceControl functions
    bool  loadScanoutApi();
    bool  scanoutApiLoaded   = false;
    void* fnSCCreateFromWin  = nullptr;
    void* fnSCRelease        = nullptr;
    void* fnSTCreate         = nullptr;
    void* fnSTDelete         = nullptr;
    void* fnSTApply          = nullptr;
    void* fnSTSetBuffer      = nullptr;
    void* fnSTSetZOrder      = nullptr;
    void* fnSTSetVisibility  = nullptr;
    void* fnSTSetGeometry    = nullptr;
    void* fnSTSetBufferTransparency = nullptr;
    void* fnSTSetBufferTransform  = nullptr;
    void* fnSTReparent  = nullptr;
    void* fnSTSetDesiredPresentTime = nullptr;
    void* fnSTSetBackPressure = nullptr;
    std::atomic<int64_t> scanoutPaceIntervalNs{0};
    int64_t scanoutNextPresentNs = 0;
    VsyncClock vsyncClock;
    void oneShot(std::function<void(void*)> fill);

    // SurfaceFlinger Callbacks
    void loadSfCallbackApi();
    void attachOnCompleteCallback(void* transaction, int64_t windowId, int64_t serial);
    static void scanoutFrameCompleteCallback(void* ctx, void* stats);

    bool  sfCallbackSupported   = false;
    void* fnSTSetOnComplete     = nullptr;

    std::shared_ptr<CallbackTarget> callbackTarget;
};
