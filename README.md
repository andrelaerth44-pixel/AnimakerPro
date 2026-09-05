# AnimakerPro

**Professional 2D animation studio for Android**

AnimakerPro is an Android-first animation application designed to combine the fast drawing workflow of Ibis Paint X, the approachable frame workflow of FlipaClip, and the professional animation controls of RoughAnimator, while evolving toward a powerful 2D production workstation.

## Product goal

This is not intended to be a simple rebrand of OpenToonz. AnimakerPro will have its own touch-first Android experience, interaction model and product architecture, using OpenToonz as an open-source technical foundation/reference where components can be adapted appropriately.

### Core capabilities

- High-quality raster drawing and painting
- Deep brush engine with pressure, opacity, dynamics, texture and stabilization
- Ibis-inspired ruler family: straight, circular, elliptical, radial, mirror, kaleidoscope, rotation, array and 1/2/3-point perspective
- Layers, groups, clipping, alpha lock and blend modes
- Frame-by-frame animation with fast add/clone/reorder operations
- Timeline, exposure and frame management
- Onion skin with configurable previous/next frame visibility
- Multi-frame copy/paste and Frames Viewer workflows
- Transform, selection, fill, color picker and shape tools
- Professional camera/workspace controls
- Audio track and synchronized playback
- Import/export of common image, video and animation formats
- Project autosave, recovery and version-safe project files
- Phone and tablet layouts
- Touch gestures plus optional keyboard/mouse support
- Hardware-accelerated rendering where available

## Research baseline

Detailed research notes are in [`docs/FEATURE_RESEARCH.md`](docs/FEATURE_RESEARCH.md). The design study covered official documentation, interface screenshots and community discussions for Ibis Paint X, FlipaClip and RoughAnimator.

The product synthesis is intentional:

1. **Ibis Paint X** → professional drawing, brush dynamics, stabilization and ruler depth.
2. **FlipaClip** → simple frame-first workflow, Frames Viewer, quick duplication/copy/paste and onion skin.
3. **RoughAnimator** → serious timeline, exposure, cycles, camera, audio, rotoscoping and production controls.

## Current implementation

The Android foundation is now in the repository. The first functional workspace includes:

- Android Gradle application shell
- Touch drawing canvas
- Stylus pressure-responsive brush foundation
- Brush size/opacity/pressure controls
- Onion-skin preview
- Pinch zoom foundation
- Ruler mode system with the planned ruler families and perspective guide overlays
- Frame document model
- Add/duplicate/remove frames
- Multi-frame copy/paste foundation
- Loop creation foundation
- Timeline/filmstrip and scrubbing
- Frame long-press actions
- Frames Viewer multi-selection
- Playback controls
- GitHub Actions debug APK build pipeline

This is **the first engineering slice, not the finished professional app**. The next layers are the real brush engine, editable ruler handles/snapping, multi-layer drawings, exposure editing, camera timeline, audio, project persistence, GPU rendering and the OpenToonz-derived core components that make sense on Android.

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
├── tools/         # Brush, eraser, fill, transform, selection, rulers
├── formats/       # Project and media import/export
├── third_party/   # External dependencies and notices
└── docs/          # Architecture, porting and licensing notes
```

## Development stages

1. ~~Android application shell and workspace~~
2. ~~Document/canvas/timeline first core~~
3. Build the production brush/ruler engine and multi-layer drawing model.
4. Port/adapt the required OpenToonz-compatible rendering and animation components instead of attempting to port the entire desktop UI stack.
5. Add camera, exposure, cycles, audio, rotoscoping and professional production workflows.
6. Add project persistence, autosave/recovery and import/export.
7. Profile on real Android hardware and optimize memory/GPU usage.
8. Produce signed Android releases and automated release checks.

## License and attribution

AnimakerPro must keep the licensing and attribution requirements of every upstream component it incorporates. OpenToonz and its bundled third-party components have separate notices/licenses; these will be tracked in `third_party/` as integration progresses.
