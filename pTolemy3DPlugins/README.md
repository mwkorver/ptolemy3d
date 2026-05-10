# pTolemy3DPlugins

`pTolemy3DPlugins` contains optional rendering and data plugins for the Ptolemy3D viewer. Plugins are not the core JP2/TIN terrain pipeline, but they demonstrate how the viewer was extended with overlays, labels, vectors, rasters, buildings, HUD elements, and other scene features.

## Plugin loading model

The viewer reads plugin class names and parameters from configuration. The plugin manager dynamically loads each class with reflection, casts it to the viewer's `Plugin` interface, initializes it, assigns an index, and passes the configured parameter string.

Because plugin classes are loaded by name, plugin configuration should be treated as trusted input unless a modern allow-list or sandboxing mechanism is added.

## Plugin lifecycle

The viewer plugin interface supports hooks for:

- non-OpenGL initialization,
- OpenGL initialization and destruction,
- per-frame drawing,
- camera-motion-stop updates,
- picking/intersection handling,
- tile-loader background work,
- command/action dispatch,
- data reloads when landscape status changes.

## Included examples

The plugin source tree includes examples such as:

- `AxisPlugin`,
- `BuildingPlugin`,
- `HudPlugin`,
- `IconPlugin`,
- `LogoPlugin`,
- `POILabelPlugin`,
- `RasterPlugin`,
- `VectorPlugin`,
- `XVRMLPlugin`.

`AxisPlugin` is a useful simple example because it implements the plugin lifecycle, parses a parameter string, exposes a `status` command, and renders basic coordinate axes.

## Build notes

The plugin Maven project depends on `org.ptolemy3d:ptolemy3d:1.0-SNAPSHOT`, which is produced by the viewer project. Like the viewer, this build targets a legacy Java/JOGL runtime and will likely need modernization before building on current JDKs.

## Modernization notes

If the renderer is modernized, keep the plugin API concept but consider replacing direct reflection loading with one of the following:

- an explicit plugin registry,
- a class-name allow-list,
- Java `ServiceLoader`,
- separate process/plugin isolation for untrusted extensions.
