# pTolemy3DTool

`pTolemy3DTool` contains historical tools that supported preparing data for the Ptolemy3D viewer. These tools are relevant because the viewer's main value is its ability to render tiled JP2 imagery over tiled TIN/DEM terrain, and those assets need preprocessing.

## Contents

| Path | Purpose |
| --- | --- |
| `tools/DemCutter/trunk` | Java/Maven terrain-processing tool. Includes DEM conversion/cutting code and geometry helpers related to triangulation/mesh generation. |
| `tools/jj2000-5.1` | Bundled JJ2000 JPEG2000 tooling source used historically for JP2 conversion workflows. |
| `ptconsole/` | CFML-based console for map-processing jobs, WMS queries, and conversion output management. |

## Relationship to the viewer

The viewer expects tiled assets with related file bases:

```text
<fileBase>.jp2          # imagery
tin/<fileBase>.tin      # TIN terrain
<fileBase>.bdm          # DEM fallback terrain
```

The tools in this directory are the best place to look when trying to understand how those files were generated or managed historically.

## DemCutter

`DemCutter` is a Java tool that appears to support cutting/converting DEM inputs and generating mesh-related terrain data. It includes:

- `org.ptolemy3d.demcutter` classes for DEM conversion/cutting,
- a `fortune` package with computational-geometry classes used for triangulation/mesh construction,
- a Maven build targeting the same legacy Java era as the viewer.

## ptconsole

`ptconsole` is a historical CFML web console. Its package README describes WMS query setup, job creation, job running, output directories for JP2/JPW files, scripts, images, and helper functions.

## Modernization notes

If the goal is to preserve the JP2-on-TIN value proposition, document and test the data formats produced by these tools before changing them. A useful future deliverable would be a small command-line workflow that converts source terrain/imagery into the exact `.jp2`, `.tin`, and `.bdm` files consumed by the viewer or a modern renderer.
