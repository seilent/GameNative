<div align="center">

# GameNative (seilent fork)

Fork of [utkarshdalal/GameNative](https://github.com/utkarshdalal/GameNative) with frame generation, external storage pipeline, and glassmorphism UI.

[![GitHub Release](https://img.shields.io/github/v/release/seilent/GameNative?style=flat-square&logo=github&label=latest)](https://github.com/seilent/GameNative/releases/latest)
[![License](https://img.shields.io/badge/license-GPL%203.0-blue?style=flat-square)](https://github.com/seilent/GameNative/blob/master/LICENSE)

[**Download**](https://github.com/seilent/GameNative/releases/latest)

<video src="https://github.com/seilent/GameNative/releases/download/v1.1.0/glassui-demo.mp4" autoplay loop muted playsinline width="100%"></video>

</div>

---

## What this fork adds

### Frame generation ([libseifg](https://github.com/seilent/libseifg))

Host-side Vulkan compute frame interpolation. The game renders at a capped base FPS and the host generates intermediate frames to hit the configured target. Runs outside the container in the Android app process, so it works with any game without per-title patching.

- Configurable per-game in Graphics settings: target FPS (30-120), multiplier (2-4x), quality (0-4)
- Real-frame cap is derived automatically (target / multiplier)
- Vsync-aligned pacing via AChoreographer

### External storage pipeline

SD card installs stage on internal storage then sequential-copy completed files to external. Bypasses the exFAT/FUSE metadata storm that throttled direct SD writes to ~3 MB/s.

- Per-game storage target picker (Steam Deck-style)
- SD hot-plug detection with library availability reconciliation
- Bounded internal staging (~1-2 GB) so small-internal devices still work

### Glassmorphism UI

Full visual overhaul. Frosted glass panels over a single dynamic blurred backdrop driven by the focused game. Per-game accent color extracted from game art. Material Design surfaces eliminated.

---

## Install

Grab the APK from [releases](https://github.com/seilent/GameNative/releases/latest) and sideload it.

## Building

Same as upstream. Android Studio, JDK 17. Native libs (libasurface_renderer.so) are prebuilt in jniLibs - gradle does not rebuild them.

## License

[GPL 3.0](https://github.com/seilent/GameNative/blob/master/LICENSE). See [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) for attributions.
