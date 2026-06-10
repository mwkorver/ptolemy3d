# Ptolemy3D

Ptolemy3D is a legacy Java/JOGL geospatial viewer that was built as a browser applet before WebGL. It was built in Tokyo by a team a Alchemedia Inc before Google bought Keyhole to implement 3D maps in the browser. Its primary innovation was it's tiled geospatial rendering pipeline: it can request JPEG2000 imagery tiles, decode JP2 data at multiple resolutions, load matching TIN or DEM terrain tiles, and render the decoded imagery over terrain geometry in the browser.

The applet runtime, JOGL 1.x bindings, and browser integration are historical. The most important parts of the repository for preservation or modernization are the JP2 imagery decoder, tiled terrain model, TIN/DEM readers, texture generation path, and renderer logic that drapes imagery over terrain.

## Primary value proposition

This repository is most useful as a reference implementation for:

1. Reading tiled JPEG2000/JP2 imagery.
2. Decoding JP2 tiles into RGB texture data.
3. Loading matching terrain tiles from TIN or DEM files.
4. Mapping JP2 imagery onto DEM/TIN terrain meshes.
5. Managing visible geospatial tiles around a moving 3D camera.

In short:

```text
JP2 imagery tile -> decoded texture -> TIN/DEM terrain tile -> textured terrain rendering
```

## Start here for the core pipeline

The historical project wiki is preserved on the repository's [`wiki` branch](https://github.com/mwkorver/ptolemy3d/tree/wiki). Those pages provide additional background on the original Java/JOGL viewer goals, tiled JP2 image preparation, EPSG:4326 tile system, TIN generation, and applet parameters.

For a focused explanation of the repository's main value proposition, see [`docs/JP2_TIN_PIPELINE.md`](docs/JP2_TIN_PIPELINE.md). It describes how Ptolemy3D loads JP2/JPEG2000 imagery, pairs it with TIN/DEM terrain, and renders textured terrain tiles. For a page-by-page review of the historical wiki corpus, see [`docs/WIKI_ANALYSIS.md`](docs/WIKI_ANALYSIS.md).

## Repository layout

| Path | Purpose |
| --- | --- |
| `pTolemy3DViewer/` | Main Java viewer engine, tile loader, JP2 decoder, TIN/DEM terrain readers, and JOGL renderer. |
| `pTolemy3DPlugins/` | Optional plugin implementations loaded by the viewer at runtime. |
| `pTolemy3DTool/` | Historical preprocessing and console tools, including DEM/TIN-related utilities. |
| `pTolemy3D-Demo-Web/` | Browser-side demo code for controlling the Java applet. |
| `PtolemyJS/` | Early JavaScript/WebGL experiment separate from the Java applet viewer. |
| `wiki/` and `www.ptolemy3d.org/` | Archived project documentation and website content. |

## Core data pipeline

The viewer organizes map content as geospatial tiles. A `Jp2Tile` keeps track of tile coordinates, level, current JP2 resolution, imagery state, optional TIN elevation, optional DEM elevation, source server metadata, and the file-base path used to request tile assets.

The tile loader fetches imagery and terrain in parallel with rendering. For a visible tile, it requests the matching `.jp2` imagery file, parses the tile header, decodes imagery at progressively useful resolutions, and uploads decoded RGB bytes as texture data. If TIN terrain is enabled, it tries to load `tin/<fileBase>.tin`; if that is unavailable, it falls back to `<fileBase>.bdm` DEM terrain.

The renderer then chooses the best available path for each tile section:

- flat textured geometry when no terrain is available,
- DEM-based textured/elevation-shaded terrain when DEM data is available,
- TIN-based textured/elevation-shaded terrain when TIN data is available.

## Important implementation areas

### JPEG2000 / JP2 decoding

The custom JP2 implementation lives under:

```text
pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/jp2/
```

It includes header parsing, packet/code-block bookkeeping, entropy decoding, subband traversal, and wavelet reconstruction. This code appears tailored to the project's tiled JP2 data and should be validated against any new dataset before assuming it is a general-purpose JPEG2000 decoder.

### TIN and DEM terrain

Terrain support lives under:

```text
pTolemy3DViewer/trunk/src/main/java/org/ptolemy3d/tile/
```

`ElevationTin` reads the binary TIN tile format into vertex and triangle-strip arrays. `ElevationDem` handles the raster DEM fallback path. The direct-mode tile renderer contains the clearest rendering logic for draping JP2 textures over TIN geometry.

### Preprocessing tools

Historical data preparation tools live under:

```text
pTolemy3DTool/
```

These tools are likely relevant if you need to understand how `.tin`, `.bdm`, and tiled JP2 assets were generated for the viewer.

## Legacy runtime notes

The original application was built for an old Java browser-plugin environment:

- Java applets and `netscape.javascript.JSObject` are used for browser integration.
- JOGL 1.x-era packages such as `javax.media.opengl` and `com.sun.opengl.util` are used.
- Maven builds target Java 1.5 and reference old dependencies/plugins.
- The original applet deployment path involved signed JARs.

Modern browsers no longer support Java applets, and the Maven build is expected to need modernization before it works on current JDKs.

## Modernization guidance

A practical modernization should preserve the data pipeline before replacing the UI/runtime shell:

1. Extract JP2, TIN, DEM, and tile metadata code into a small testable library.
2. Add tests around known JP2/TIN/DEM samples.
3. Replace applet-specific code with a desktop, service, or browser-native runtime.
4. Replace the old HTTP communicator with a modern client that supports HTTPS, timeouts, range requests, and robust EOF/error handling.
5. Port rendering logic to a modern renderer only after the tile decoding and terrain-mapping behavior is captured by tests.

A useful extraction target would be an API that accepts a JP2 tile and matching TIN tile and returns texture bytes, vertices, indices/triangle strips, and UV coordinates.

## Historical links

The original README referenced these demonstration videos:

- https://www.youtube.com/watch?v=v7wl5tZkuUQ
- https://www.youtube.com/watch?v=oFUDl2KQ1dA
