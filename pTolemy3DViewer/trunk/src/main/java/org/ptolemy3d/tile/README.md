# Tile, JP2 Texture, and Terrain System

This package contains the core tiled terrain-imagery pipeline. It is one of the most important areas of the repository because it connects JP2 imagery, terrain data, texture buffers, and rendering.

## Important classes

| Class | Role |
| --- | --- |
| `Jp2Tile` | Per-tile state for JP2 imagery, current resolution, TIN/DEM terrain, source server metadata, and file naming. |
| `Jp2TileLoader` | Background loader that finds visible tiles, requests JP2 imagery, loads terrain, decodes imagery, and manages texture data. |
| `ElevationTin` | Parser for the binary TIN tile format. Reads tile origin/width, vertices, and triangle-strip arrays. |
| `ElevationDem` | Parser/storage for raster DEM terrain fallback data. |
| `Tile` | Landscape tile wrapper that connects a visible tile section to JP2/terrain data and rendering. |
| `TileDirectModeRenderer` | Immediate-mode OpenGL renderer with branches for textured, DEM, and TIN rendering. |
| `TileDefaultRenderer` | Alternate tile renderer path. |
| `QuadBufferPool` | Reusable byte-buffer pool for decoded JP2 texture data. |
| `Level` | Tile level metadata such as tile size, zoom range, and visibility state. |

## Asset relationship

A visible tile is represented by a shared file base. The loader uses that base to request related assets:

```text
<fileBase>.jp2          # JPEG2000 imagery tile
tin/<fileBase>.tin      # TIN terrain tile, when TIN terrain is enabled
<fileBase>.bdm          # DEM terrain fallback
```

The configured map-data locations provide JP2 files. The configured DEM locations provide TIN and DEM files.

## Loader flow

At a high level, `Jp2TileLoader` does the following:

1. Creates one communicator per configured data server.
2. Loads `default.jp2` headers for each JP2 store location.
3. Watches visible landscape levels and tiles.
4. Requests the `.jp2` header/data for the closest needed tile.
5. Attempts to load matching TIN terrain from `tin/<fileBase>.tin` when TIN is enabled.
6. Falls back to matching DEM terrain from `<fileBase>.bdm` when TIN is unavailable or disabled.
7. Decodes JP2 data into RGB texture bytes and marks tile resolutions as ready.
8. Frees old texture data and coordinates with the texture manager.

## JP2 texture data

`Jp2Tile` stores four resolution states by default. The tile loader decodes JP2 data progressively and writes RGB bytes into buffers managed by `QuadBufferPool`. The active decoded resolution becomes the tile's current texture resolution.

This is useful because a tile can be rendered quickly at a lower JP2 resolution and upgraded as higher-resolution code blocks become available.

## TIN terrain data

`ElevationTin` reads a compact binary terrain representation:

- tile origin `x`,
- tile origin `y`,
- tile width `w`,
- vertex positions `p`,
- triangle-strip/index arrays `nSt`.

`TileDirectModeRenderer.drawTinSubsection` iterates over the TIN triangle strips, converts local TIN coordinates into globe coordinates, applies elevation scaling, emits texture coordinates from local TIN X/Z coordinates, and renders the resulting triangles.

## DEM fallback data

When TIN terrain is not enabled or cannot be loaded, the loader requests `.bdm` files and stores them as `ElevationDem`. The direct-mode renderer has separate branches for textured and untextured DEM terrain rendering.

## Modernization notes

This package is a strong candidate for extraction into a standalone, testable library. A modern API could expose:

```text
JP2 bytes + TIN bytes -> texture bytes + terrain vertices + indices + UVs
```

Before replacing the renderer, add tests that confirm the decoded texture dimensions, TIN vertex/index layout, and generated UV coordinates for known sample tiles.
