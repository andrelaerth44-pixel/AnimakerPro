# Brush and asset policy

AnimakerPro uses a data-driven native brush engine so that brush behavior can be expanded without coupling the Android UI to a single renderer.

## OpenToonz

OpenToonz source code and bundled resources must be reviewed component-by-component before redistribution. AnimakerPro may reuse compatible OpenToonz code and resources only when their specific license and notice requirements permit it. Third-party resources and separately licensed brush libraries are kept isolated and credited rather than treated as automatically covered by the main OpenToonz license.

When OpenToonz-derived material is shipped, the corresponding copyright/license notices are included in the app's open-source notices.

## ibisPaint

AnimakerPro will reproduce useful *capabilities* (pressure curves, stabilization, rulers, symmetry, texture, wet media, taper, spacing and dynamic response) through our own implementation. We do not copy ibisPaint's proprietary brush assets, proprietary presets, source code, branding or artwork without a license that expressly permits redistribution.

Users may import brush assets they are independently licensed to use when an importer for that format is implemented.

## Built-in AnimakerPro presets

The default catalog in `BrushLibrary.kt` contains original parameter profiles. It is intentionally data-driven and can grow into a large professional library while remaining independent from proprietary third-party assets.

## Release checklist

Before a public APK/release:

- generate a complete third-party notices screen;
- record the exact source and license for every bundled brush/texture;
- keep incompatible or non-redistributable assets out of the release package;
- preserve required copyright and attribution notices;
- verify that every bundled texture, brush tip and sample is cleared for redistribution.
