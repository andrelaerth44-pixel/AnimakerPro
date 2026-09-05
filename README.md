# AnimakerPro

**Professional 2D animation studio for Android**

AnimakerPro is an Android-first animation application designed to combine the fast drawing workflow of Ibis Paint X, the approachable frame workflow of FlipaClip, and the professional animation controls of RoughAnimator, while evolving toward a powerful 2D production workstation.

## Product goal

This is not intended to be a simple rebrand of OpenToonz. AnimakerPro will have its own touch-first Android experience, interaction model and product architecture, using OpenToonz as an open-source technical foundation/reference where components can be adapted appropriately.

### Core capabilities

- High-quality raster drawing and painting
- Vector/line-art workflows where appropriate
- Pressure-sensitive stylus input
- Custom brushes, stabilization and brush dynamics
- Layers, groups, clipping, alpha lock and blend modes
- Frame-by-frame animation
- Timeline, exposure and frame management
- Onion skin with configurable previous/next frame visibility
- Transform, selection, fill, color picker and shape tools
- Camera/workspace controls and canvas rotation/zoom
- Audio track and synchronized playback
- Import/export of common image, video and animation formats
- Project autosave, recovery and version-safe project files
- Phone and tablet layouts
- Touch gestures plus optional keyboard/mouse support
- Hardware-accelerated rendering where available

## OpenToonz foundation

Upstream project: https://github.com/opentoonz/opentoonz

OpenToonz is a full-featured open-source 2D animation application. Its current source tree is organized around a C++/CMake/Qt desktop application, so it cannot simply be copied into an Android Gradle project unchanged. The Android version therefore requires a deliberate port/architecture layer rather than a cosmetic rename.

All upstream copyright notices, license texts and third-party attributions must be preserved for any code that is actually incorporated into distributed builds.

## Target architecture

```text
AnimakerPro/
├── android/       # Android app, lifecycle, UI, input, permissions
├── core/          # Document, scene, frame and project model
├── render/        # GPU/CPU rendering and compositing
├── timeline/      # Frames, exposure, playback, onion skin
├── tools/         # Brush, eraser, fill, transform, selection, etc.
├── formats/       # Project and media import/export
├── third_party/   # External dependencies and notices
└── docs/          # Architecture, porting and licensing notes
```

## Development stages

1. Establish Android application shell and tablet/phone workspace.
2. Build the document/canvas/timeline core with touch and stylus input.
3. Port/adapt the required OpenToonz-compatible rendering and animation components instead of attempting to port the entire desktop UI stack.
4. Add professional drawing and animation tools.
5. Add audio, import/export, project recovery and production workflows.
6. Profile on real Android hardware and optimize memory/GPU usage.
7. Produce signed Android builds and automated release checks.

## Current status

Repository initialized. The immediate engineering task is the Android foundation and a technical OpenToonz porting assessment. The source must be adapted for Android rather than pretending the existing desktop Qt stack is already Android-compatible.
