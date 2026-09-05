# AnimakerPro — feature research

Research baseline for the first Android implementation. This document describes what AnimakerPro should learn from each reference product without copying proprietary code or branding.

## Ibis Paint X — drawing system

### Rulers
The Ibis Paint ruler family is unusually broad and is a priority for AnimakerPro:

- Straight ruler
- Circular ruler
- Elliptical ruler
- Radial ruler
- Mirror/symmetry ruler
- Kaleidoscope ruler
- Rotation ruler
- Array ruler
- Perspective Array Ruler
  - one-point perspective
  - two-point perspective
  - three-point perspective
  - adjustable divisions and phase
  - face/surface selection and positioning
  - combinations with straight, circular and radial rulers

### Brush engine lessons
The brush model should expose professional parameters rather than only size/color:

- thickness and opacity
- start/end taper and fade
- brush pattern and spacing
- angle/follow rotation/aspect
- jitter for position, thickness, opacity, spacing and angle
- scatter/particles
- texture
- blend mode
- speed response
- stylus pressure response
- per-brush pressure curves
- high-quality antialiasing
- watercolor/wet mixing concepts
- custom brush presets

### Stabilization
Support both real-time stabilization and after-stroke correction, with adjustable strength and optional force-fade controls.

## FlipaClip — simplicity and frame manipulation

The most important lesson is that frame operations should be visible and fast:

- timeline/filmstrip with immediate frame selection
- add blank frame
- add frame before/after current position
- clone/duplicate frame
- hold a drawing for multiple exposures
- onion skin
- playback and scrubbing
- Frames Viewer for selecting many frames together
- multi-frame copy and paste before/after a selected position
- drag to reorder frames
- delete multiple frames
- copy individual layers or lasso selections
- audio track for timing
- image/video references for rotoscoping

The frame data model therefore treats a frame as a collection of drawing layers, while the timeline controls frame order and exposure. Multi-frame clipboard operations are first-class operations, not an afterthought.

## RoughAnimator — professional animation workflow

The professional baseline should include:

- multi-layer timeline
- adjustable drawing exposure length
- pose-to-pose and straight-ahead workflows
- onion skinning
- playback preview and timeline scrubbing
- labels for drawings
- drawing duplication and empty drawing insertion
- swap previous/next drawing
- make-cycle/repeating drawing ranges
- multi-drawing editing
- camera tool for pans and zooms
- import audio for lip sync
- import video for rotoscoping
- custom brushes and pressure-sensitive stylus support
- configurable frame rate and resolution
- animation export to video/GIF/image sequence

For the first implementation, camera transforms, exposure, cycles and edit-multiple operations are part of the timeline architecture rather than separate unrelated screens.

## AnimakerPro product synthesis

The product should combine these strengths into one touch-first workflow:

1. **Draw** — Ibis-level brush/ruler depth.
2. **Animate** — FlipaClip-level immediacy for adding, duplicating and rearranging frames.
3. **Produce** — RoughAnimator-level timeline, exposure, camera, audio and professional controls.
4. **Stay simple** — advanced tools remain contextual so the canvas remains the dominant workspace.

## First implementation already started

The repository now contains an Android Gradle application with:

- native Android application shell
- touch drawing canvas
- pressure-aware input foundation
- onion-skin preview
- zoom gesture
- ruler mode overlay system
- frame document model
- add/duplicate/remove frames
- multi-frame copy/paste foundation
- loop creation foundation
- timeline/filmstrip
- playback controls
- Frames Viewer multi-selection
- automated debug APK CI workflow

The current ruler implementation is a functional foundation/visual guide layer. The final engine will replace the simple guide geometry with production-grade snapping, editable handles and GPU-accelerated rendering.

## Sources researched

- Ibis Paint official tutorials and feature history
- FlipaClip official product/support documentation and Frames Viewer/copy-paste documentation
- RoughAnimator official feature page and user guide
- Community discussions on Reddit for practical workflow pain points and interface behavior
- Interface screenshots of all three products for layout study

No proprietary source code is copied into AnimakerPro. Product names are used only to describe the design research baseline.
