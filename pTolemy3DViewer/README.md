# pTolemy3DViewer

`pTolemy3DViewer` is the main Java implementation of Ptolemy3D. It contains the runtime engine, configuration loader, JOGL event loop, scene graph, camera controls, tile loader, custom JP2 decoder, DEM/TIN terrain readers, texture management, and rendering code.

The historical viewer could run as a Java applet, Swing/AWT panel, or frame. For modern reuse, the most important parts are the tile-data and terrain-rendering pipeline rather than the applet wrapper.

The original viewer wiki page is preserved on the repository's [`wiki` branch](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyViewer.md). It frames Ptolemy3D as an extensible GPLv3 Java/JOGL viewer for web-page and desktop use; this README keeps that context while highlighting the JP2/TIN/DEM pipeline as the most reusable part today.

## Main runtime flow

1. `Ptolemy3D` is the central runtime object. It owns configuration, camera state, scene, texture manager, tile loader, and JOGL events.
2. `Ptolemy3DConfiguration` reads XML or applet parameters for servers, layers, terrain, colors, initial camera position, and plugin configuration.
3. `Ptolemy3DEvents` connects the runtime to JOGL, initializes OpenGL resources, starts the render loop, and starts the tile-loader thread.
4. `Scene` coordinates drawing of the sky, landscape, plugins, lighting, and screenshots.
5. `Landscape` calculates visible tile levels and delegates tile rendering.
6. `Jp2TileLoader` loads JP2 imagery and TIN/DEM terrain for visible tiles.

## Core packages

| Package | Role |
| --- | --- |
| `org.ptolemy3d` | Core runtime, configuration, JavaScript bridge, unit system, and event loop. |
| `org.ptolemy3d.viewer` | Applet, canvas, panel, and frame wrappers. Mostly legacy runtime shell. |
| `org.ptolemy3d.scene` | Scene orchestration, landscape, sky, lighting, texture manager, and plugin manager. |
| `org.ptolemy3d.tile` | Tile model, tile loader, TIN/DEM readers, JP2 texture handling, and tile renderers. |
| `org.ptolemy3d.tile.jp2` | Custom JPEG2000/JP2 header, packet, entropy, subband, and reconstruction logic. |
| `org.ptolemy3d.view` | Camera, movement, input, and coordinate/view state. |
| `org.ptolemy3d.math` | Matrix, vector, quaternion, and math helpers. |
| `org.ptolemy3d.io` | Historical HTTP/socket communication abstraction. |
| `org.ptolemy3d.font` | Bitmap/font rendering support. |
| `org.ptolemy3d.debug` | Debug logging and profiling helpers. |

## JP2 imagery and terrain pipeline

For a concise repository-level summary of this flow, see [`../docs/JP2_TIN_PIPELINE.md`](../docs/JP2_TIN_PIPELINE.md).

The viewer's durable value is the path from tiled geospatial assets to rendered terrain:

```text
configuration -> visible levels -> Jp2Tile -> .jp2 imagery -> decoded RGB texture
                                      |
                                      +-> tin/<fileBase>.tin or <fileBase>.bdm terrain
                                      |
                                      +-> textured DEM/TIN rendering
```

`Jp2TileLoader` is the best starting point for understanding asset loading. It initializes data-server communicators, fetches `default.jp2` headers, locates visible JP2 tiles, loads matching terrain, and manages tile texture-resolution requests.

`TileDirectModeRenderer` is the best starting point for understanding how decoded imagery is rendered over terrain. It switches between plain tile rendering, DEM rendering, and TIN rendering based on the terrain data available for the current `Jp2Tile`.

## Applet and browser integration

`org.ptolemy3d.viewer.Ptolemy3DApplet` and `Ptolemy3DJavascript` are legacy browser-plugin integration points. They were useful for the original applet deployment but should not be treated as the modern architectural center of the project.

## Build notes

The Maven project under `trunk/` targets Java 1.5 and references old JOGL/app/plugin dependencies. It may not build on current JDKs without dependency and plugin modernization. Treat the existing POM as historical build metadata until it has been updated.

## Suggested modernization boundary

For preservation or reuse, extract these areas first:

- `org.ptolemy3d.tile.jp2` for JP2 decoding,
- `Jp2Tile`, `Jp2TileLoader`, and related tile metadata,
- `ElevationTin` and `ElevationDem` for terrain formats,
- the TIN/DEM branches of `TileDirectModeRenderer` for texture-to-terrain mapping.

Avoid starting with the applet shell unless the goal is specifically historical restoration.
