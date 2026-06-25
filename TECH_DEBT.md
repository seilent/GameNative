# Tech Debt

Running log of known shortcuts and things that should be revisited.

## LSFG frame generation — power-saving (real-frame cap + pacing)

Context: LSFG (Lossless Scaling Frame Generation) generates interpolated frames. To
actually save power, the **real** render rate must be capped below the displayed rate so
the GPU renders fewer frames while LSFG fills in the rest. Three changes implement this
(real-frame cap, frame pacing, honest HUD).

### 1. Hardcoded base render cap (30 fps)

- **What:** the real (pre-LSFG) render cap is hardcoded to `LsfgVkManager.LSFG_BASE_FPS_CAP = 30`.
  With multiplier M the displayed output is ~`30 * M` (e.g. 2x → 60).
- **Where:**
  - `app/src/main/java/app/gamenative/utils/LsfgVkManager.kt` — `LSFG_BASE_FPS_CAP`,
    `applyRealFrameCap()` (sets `DXVK_FRAME_RATE`/`VKD3D_FRAME_RATE` = 30).
  - `app/src/main/java/app/gamenative/ui/screen/xserver/XServerScreen.kt` —
    `scanoutPacingIntervalNs()` paces to `min(30 * multiplier, refreshHz)`.
- **Why:** quick first cut so the feature is testable without new UI.
- **Proper fix:** expose the base cap as a user-facing setting (a dedicated "LSFG base FPS"
  control, or treat the existing FPS-limiter target as the real cap under LSFG). Consider
  deriving it as `refreshHz / multiplier` so the displayed output always matches the panel
  refresh rate (e.g. 60 Hz → base 30 at 2x, base 20 at 3x, base 15 at 4x) instead of a fixed 30.

### 2. Real-frame cap is launch-time only (DXVK/VKD3D env)

- `DXVK_FRAME_RATE`/`VKD3D_FRAME_RATE` are read by DXVK/VKD3D at process start, so changing
  the LSFG multiplier in the in-game quick menu does **not** re-derive the real cap until the
  container is relaunched. (Pacing updates live; only the guest render cap is fixed at launch.)
- **Proper fix:** a hot-reloadable cap (e.g. enforce the base rate inside the lsfg-vk layer by
  throttling acknowledgement of the game's real presents), which would also make it
  API-agnostic (see below).

### 3. DXVK/VKD3D only

- The cap relies on `DXVK_FRAME_RATE`/`VKD3D_FRAME_RATE`, so it does **not** cap OpenGL or
  WineD3D (non-DXVK) titles. Acceptable for now (current target titles are DXVK/VKD3D).
- **Proper fix:** cap inside the lsfg-vk layer (covers all guest graphics APIs).

### 4. Frame pacing relies on ASurfaceTransaction back-pressure (API 31+)

- Pacing uses `ASurfaceTransaction_setDesiredPresentTime` (API 29) + `setEnableBackPressure`
  (API 31). Both are optional `dlsym`s. On API 29–30 there is no back-pressure, so bursted
  generated frames may still be dropped by SurfaceFlinger. Pacing degrades gracefully but is
  not guaranteed on those API levels.
- Pacing also assumes the panel refresh rate is >= `base * multiplier`; above that the output
  is clamped to refresh and some generated frames are dropped (a hardware limit).

### 5. `scanoutNowNs()` duplicated across both scanout TUs

- The monotonic-clock helper is duplicated (as `static inline`) in
  `winlator/VulkanRendererScanout.cpp` and `asurfacerenderer/ASurfaceRendererContext.cpp`.
  Fine for now (self-contained scanout files); could move to a shared header.

## Native build

- The native `.so` libraries (e.g. `libvulkan_renderer.so`, `libasurface_renderer.so`) are
  **prebuilt and committed** under `app/src/main/{,legacy/,modern/}jniLibs/`. The gradle
  `externalNativeBuild` for `src/main/cpp/CMakeLists.txt` is commented out, so
  `./gradlew assembleDebug` does **not** recompile native code — the C++ changes above must be
  rebuilt with the NDK/CMake and the resulting `.so` copied into `jniLibs/` before packaging.
