# JP2/JPEG2000 Imagery on TIN/DEM Terrain

This document summarizes the part of Ptolemy3D that is most worth preserving: reading tiled JP2/JPEG2000 imagery and rendering it over tiled TIN or DEM terrain.

## Primary value

Ptolemy3D is historically a Java applet/JOGL globe viewer, but the durable technical value is the data pipeline:

```text
visible map tile
  -> load <fileBase>.jp2 imagery
  -> decode JPEG2000 data into RGB texture bytes
  -> load tin/<fileBase>.tin terrain when available
  -> otherwise load <fileBase>.bdm DEM terrain
  -> render the imagery as a texture over the terrain mesh
```

The applet shell and old JOGL runtime are legacy infrastructure. The JP2/TIN/DEM tile pipeline is the part to study first for reuse, restoration, or modernization.

## Where the pipeline lives

| Concern | Location |
| --- | --- |
| Tile state and JP2/TIN/DEM association | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/Jp2Tile.java` |
| Background tile loading | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/Jp2TileLoader.java` |
| JP2/JPEG2000 decoder | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/jp2/` |
| TIN terrain reader | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/ElevationTin.java` |
| DEM terrain fallback | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/ElevationDem.java` |
| Textured TIN/DEM rendering | `pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/TileDirectModeRenderer.java` |
| Terrain/imagery preprocessing tools | `pTolemy3DTool/` |

## Historical wiki context

The repository also has a GitHub branch named `wiki` that contains the converted project wiki. The branch is useful historical context for the data contract described above:

- [`PtolemyViewer.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyViewer.md) describes the original viewer goal as a GPLv3 Java/JOGL viewer for web-page and desktop use.
- [`PtolemyDataOverview.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataOverview.md) explains that Ptolemy3D worlds are built from imagery plus DEM/height data and use tiled LOD pyramids to reduce data transfer.
- [`PtolemyDataTileSystem.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTileSystem.md) records the expected tile system: WGS84 longitude/latitude in EPSG:4326, tiled/layered data at increasing resolution, 8x8 subdivision between levels, and 1024-pixel JP2 tiles with internal layers.
- [`PtolemyDataImagePrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataImagePrep.md) documents the historical imagery layout with a JP2 root, `default.jp2`, tile-width directories, and filenames derived from the tile's upper-left coordinate.
- [`PtolemyDataTinPrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTinPrep.md) describes the historical TIN-generation process from DEM input, including point reduction, triangulation, triangle-strip generation, and the big-endian binary TIN file layout.
- [`PtolemyParameters.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyParameters.md) lists configuration parameters that connect the viewer to JP2 stores, DEM stores, layer widths, tile naming, TIN enablement, and plugin setup.

The local `wiki/` directory in this repository contains related `.wiki` exports, but the `wiki` branch is the GitHub-rendered Markdown version and should be reviewed when reconstructing historical behavior.

## Runtime data relationship

Each visible map tile has a shared `fileBase`. The loader uses that base to find related assets:

```text
<fileBase>.jp2          # imagery tile
tin/<fileBase>.tin      # preferred TIN terrain tile when TIN is enabled
<fileBase>.bdm          # DEM fallback terrain tile
```

This convention is the practical contract between the preprocessing tools, tile stores, tile loader, JP2 decoder, and terrain renderer.

## Why this matters

Many old applet-era projects are only useful as historical artifacts. Ptolemy3D is different because it contains working domain logic for a specialized geospatial rendering problem:

- tiled JPEG2000 imagery instead of ordinary PNG/JPEG web tiles,
- progressive/resolution-aware JP2 texture generation,
- tiled terrain from compact TIN files,
- DEM fallback terrain,
- texture-coordinate generation that drapes imagery over irregular terrain geometry.

That combination is the strongest reason to keep the repository understandable.

## Recommended modernization approach

Modernization should preserve behavior before replacing the runtime:

1. Capture sample `.jp2`, `.tin`, and `.bdm` tiles.
2. Add tests that decode JP2 tiles and verify texture dimensions/content.
3. Add tests that parse TIN and DEM terrain and verify mesh/elevation data.
4. Extract JP2, TIN, DEM, and tile metadata into a renderer-independent library.
5. Expose a simple output model: texture bytes, vertices, indices or triangle strips, and UV coordinates.
6. Only then port rendering to a modern target such as JOGL 2, LWJGL, WebGL, WebGPU, or another engine.

A useful extraction goal is:

```text
JP2 bytes + TIN/DEM bytes -> textured terrain tile data
```

where the output can be consumed by any modern renderer.

## What not to start with

Do not start modernization with the Java applet integration, browser JavaScript bridge, signed JAR deployment, or old JOGL event loop unless the goal is historical restoration. Those parts are useful context, but they are not the core value proposition.
