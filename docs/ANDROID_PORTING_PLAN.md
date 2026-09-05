# AnimakerPro — Android porting plan

## Why a direct desktop port is not enough

OpenToonz's documented build process is desktop-oriented and uses CMake plus Qt 5.x and a number of native dependencies. The official source tree contains desktop-specific application code and build assumptions. A successful Android product therefore needs a mobile architecture around the reusable animation/rendering technology rather than a direct copy of the desktop executable.

## Android-first workspace

The primary workspace should be designed for touch:

- compact top action bar
- canvas occupying the dominant area
- bottom timeline that can expand/collapse
- contextual tool properties
- layers panel as a drawer/sheet
- two-finger zoom and pan
- two-finger rotation
- stylus pressure/tilt where supported
- long-press/context actions
- optional keyboard shortcuts on tablets/Chromebooks

## MVP animation engine

The first functional milestone should support:

1. Create project and canvas.
2. Draw with a pressure-aware brush.
3. Erase and undo/redo.
4. Create and reorder layers.
5. Create frames and exposures.
6. Play the animation.
7. Onion skin.
8. Transform artwork.
9. Save/reopen a project.
10. Export a basic animated result.

## Professional expansion

After the MVP, add:

- brush engine improvements
- stabilization
- selection/masking
- clipping and blend modes
- vector workflows
- camera moves
- audio synchronization
- keyframe/tween helpers
- scene management
- reusable assets
- advanced export
- crash recovery
- background rendering/export jobs

## OpenToonz integration strategy

Do not port unrelated desktop UI code just because it exists upstream. Identify reusable algorithms and data structures, isolate them behind AnimakerPro interfaces, and replace desktop-only dependencies when Android equivalents are required.

Every imported upstream component must retain its attribution and applicable license information.
