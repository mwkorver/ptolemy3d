# Historical Wiki Branch Analysis

This repository also preserves historical documentation on a GitHub branch named [`wiki`](https://github.com/mwkorver/ptolemy3d/tree/wiki). The current worktree contains equivalent wiki exports under `wiki/*.wiki`; the `wiki` branch renders the same project documentation as Markdown pages.

This analysis reviewed the local wiki exports as the source corpus and maps them to their Markdown page names on the `wiki` branch.

## Page count

- Local wiki export files reviewed: **72** `.wiki` pages.
- Non-page helper files in `wiki/`: **1** file, `doit`.
- Expected Markdown pages on the `wiki` branch: **72** `.md` pages, corresponding to the `.wiki` page names below.

## High-level findings

The wiki confirms that Ptolemy3D was intended as more than a Java applet. It was documented as a toolkit for putting a dynamic 3D world into web pages, using a Java JOGL viewer, server-side components, JPEG2000 imagery, OpenGL, WMS, and WFS.

The pages most relevant to the repository's durable technical value are the data and tooling pages. They document a tile pyramid based on WGS84/EPSG:4326 decimal-degree coordinates, JPEG2000 image tiles, DEM/height data, TIN generation, applet configuration parameters, and supporting tools such as `ptconsole`, `DemCutter`, `DemMaker`, `MSToJp2`, and JP2 encoding workflows.

The plugin pages show that extensibility was also a major goal. Plugins cover POI labels, vectors, buildings, logos, XVRML, WFS/GML inputs, and custom plugin data loading.

## Most relevant pages for the JP2/TIN pipeline

| Wiki branch page | Why it matters |
| --- | --- |
| [`WikiStart.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/WikiStart.md) | Frames pTolemy3D as a web-page 3D world toolkit using Java applets, server-side components, JPEG2000, OpenGL, WMS, and WFS. |
| [`PtolemyDataOverview.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataOverview.md) | Explains that worlds are built from imagery plus DEM/height data, organized into tiled/layered LOD pyramids. |
| [`PtolemyDataTileSystem.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTileSystem.md) | Documents EPSG:4326 decimal-degree tiling, 8x8 subdivision between levels, and 1024-pixel JP2 tiles with internal layers. |
| [`PtolemyDataImagePrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataImagePrep.md) | Documents the historical imagery directory layout, `default.jp2`, tile-width directories, and upper-left-coordinate filenames. |
| [`PtolemyDataTinPrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTinPrep.md) | Describes TIN generation from DEM source data, including point reduction, triangulation, triangle-strip generation, and binary output layout. |
| [`PtolemyParameters.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyParameters.md) | Lists runtime parameters tying the viewer to JP2 stores, DEM stores, layer widths, dividers, TIN enablement, and plugins. |
| [`PtolemyToolEncoder.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolEncoder.md) | Describes JP2 encoding assumptions and the Kakadu-based encoder workflow. |
| [`PtolemyToolMsToJp2.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolMsToJp2.md) | Documents map-service to JP2 generation tooling. |
| [`PtolemyToolsDemCutter.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolsDemCutter.md) | Documents the DEM cutting tool used to create data ready for Ptolemy3D. |
| [`PtolemyToolDemMaker.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolDemMaker.md) | Documents DEM-maker tooling and tile generation concepts. |
| [`DataJpToBbox.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/DataJpToBbox.md) | Connects JP2/tile path naming to bounding-box calculations. |

## Complete page inventory

| Wiki branch page | Category | Summary |
| --- | --- | --- |
| [`DataJpToBbox.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/DataJpToBbox.md) | Data / tiles | Notes on deriving bounding boxes from JP2 directory and tile-coordinate naming. |
| [`DistribServArch.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/DistribServArch.md) | Server architecture | Multiple-server deployment architecture for JP2/DEM data, including synchronization and distributed serving concerns. |
| [`PageTemplatesPtolemy.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PageTemplatesPtolemy.md) | Stub | Placeholder page. |
| [`PtolemyContribute.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyContribute.md) | Contribution | Index page for contribution docs and license agreements. |
| [`PtolemyContributeCcla.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyContributeCcla.md) | Contribution / legal | Corporate contributor license agreement. |
| [`PtolemyContributeCla.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyContributeCla.md) | Contribution / legal | Contributor license agreement overview. |
| [`PtolemyContributeHowTo.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyContributeHowTo.md) | Contribution | How to contribute, including project process, code style expectations, issue tracking, and patch workflow. |
| [`PtolemyContributeIcla.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyContributeIcla.md) | Contribution / legal | Individual contributor license agreement. |
| [`PtolemyConventions.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyConventions.md) | Development | Naming and formatting conventions for packages, classes, methods, members, and constants. |
| [`PtolemyCreatePluginDataLoad.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyCreatePluginDataLoad.md) | Plugins | Explains plugin background data loading through the tile-loader callback model. |
| [`PtolemyData.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyData.md) | Data | Short index to image, DEM, tile-system, and data-ratio pages. |
| [`PtolemyDataDemPrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataDemPrep.md) | Data / terrain | Stub page for DEM preparation. |
| [`PtolemyDataIdealRatio.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataIdealRatio.md) | Data / tiles | Discusses ideal decimal-degree widths for JP2 and DEM generation. |
| [`PtolemyDataImagePrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataImagePrep.md) | Data / imagery | JP2 image preparation, directory layout, `default.jp2`, level-width directories, and coordinate-derived filenames. |
| [`PtolemyDataOverview.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataOverview.md) | Data | Overview of imagery plus DEM/height data and LOD tiling. |
| [`PtolemyDataTileSystem.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTileSystem.md) | Data / tiles | EPSG:4326 tile coordinate system, 8x8 level subdivision, and 1024-pixel JP2 tile model. |
| [`PtolemyDataTinPrep.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDataTinPrep.md) | Data / terrain | TIN preparation from DEM input, point reduction, triangulation, strip generation, and binary TIN output. |
| [`PtolemyDemo.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDemo.md) | Demo | Stub page for demo links. |
| [`PtolemyDemoWarFile.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDemoWarFile.md) | Demo | Demo WAR release/download note. |
| [`PtolemyDemoWarFileReleaseNotes.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDemoWarFileReleaseNotes.md) | Demo | Release notes for demo WAR versions. |
| [`PtolemyDevelopTestDesktop.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDevelopTestDesktop.md) | Development | Desktop test setup, dependencies, JAR signing context, configuration, and plugin notes. |
| [`PtolemyDeveloperGuide.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDeveloperGuide.md) | Development | Developer guide for source checkout, Maven builds, plugin builds, tools, docs, and tests. |
| [`PtolemyDownload.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyDownload.md) | Download | Download page for viewer, plugins, and demo WAR artifacts. |
| [`PtolemyEmailList.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyEmailList.md) | Community | Mailing-list signup page. |
| [`PtolemyEmailThanks.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyEmailThanks.md) | Community | Mailing-list confirmation page. |
| [`PtolemyFAQ.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyFAQ.md) | Overview / FAQ | FAQ covering motivation, relation to other virtual globes, Java/JOGL choice, applets, and extensibility. |
| [`PtolemyHistory.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyHistory.md) | History | Project history and intent, emphasizing client-side 3D internet maps and user-controlled data. |
| [`PtolemyHowToStart.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyHowToStart.md) | Development | Introduction to using the core, applet, desktop frame, and GLCanvas wiring. |
| [`PtolemyOrg.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyOrg.md) | Governance | Organization landing page. |
| [`PtolemyOrgSteeringCommittee.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyOrgSteeringCommittee.md) | Governance | Draft project steering committee document. |
| [`PtolemyOrgSteeringCommitteeMembers.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyOrgSteeringCommitteeMembers.md) | Governance | Steering committee member list. |
| [`PtolemyOverview.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyOverview.md) | Architecture | Package-level technical overview of the viewer, events, scene, landscape, plugins, and applet/frame wrappers. |
| [`PtolemyParameters.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyParameters.md) | Configuration | Comprehensive applet/XML parameter reference for servers, layers, position, JP2 tiles, TIN, and plugins. |
| [`PtolemyPlugin.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPlugin.md) | Plugins | Plugin overview and list of standard plugins. |
| [`PtolemyPluginBuilding.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginBuilding.md) | Plugins | Building plugin parameter and usage notes. |
| [`PtolemyPluginCreate.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginCreate.md) | Plugins | How to create plugins, including lifecycle methods and example implementation. |
| [`PtolemyPluginGmlToPoi.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginGmlToPoi.md) | Plugins / WFS | XSLT/GML-to-POI transformation examples. |
| [`PtolemyPluginGmlToVec.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginGmlToVec.md) | Plugins / WFS | GML-to-vector transformation examples. |
| [`PtolemyPluginLogo.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginLogo.md) | Plugins | Logo plugin parameters. |
| [`PtolemyPluginPoi.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginPoi.md) | Plugins | POI plugin parameters and behavior. |
| [`PtolemyPluginVector.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginVector.md) | Plugins | Vector plugin parameters and behavior. |
| [`PtolemyPluginWfsPoiVersion.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginWfsPoiVersion.md) | Plugins / WFS | WFS-based POI plugin variant. |
| [`PtolemyPluginXvrml.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyPluginXvrml.md) | Plugins | XVRML plugin parameters and behavior. |
| [`PtolemyProjectOverview.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyProjectOverview.md) | Architecture | Short index into Java-code overview docs. |
| [`PtolemyRefactor0.1.0.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyRefactor0.1.0.md) | Refactor | Notes for the 0.1.0 refactor, package organization, JP2 decoder, configuration, events, and plugin architecture. |
| [`PtolemyRefoundation.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyRefoundation.md) | Refoundation | Refoundation landing page and process links. |
| [`PtolemyRefoundationSpecification.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyRefoundationSpecification.md) | Refoundation | Draft virtual-globe SDK specification covering tile model, scene, camera, coordinate systems, input, plugins, and extensibility. |
| [`PtolemyRefoundationStepOne.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyRefoundationStepOne.md) | Refoundation | Basic class diagram and first refoundation step notes. |
| [`PtolemyServer.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyServer.md) | Server | Server docs index for Apache configuration and server-side data/plugins. |
| [`PtolemyServerConfigApache.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyServerConfigApache.md) | Server | Apache directory, CORS/header, and JP2/DEM serving configuration. |
| [`PtolemySettingUpApplet.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemySettingUpApplet.md) | Applet | Full applet setup instructions, archive dependencies, scriptable API, and parameter setup. |
| [`PtolemyTool.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyTool.md) | Tools | Stub page. |
| [`PtolemyToolDemCuttingScript.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolDemCuttingScript.md) | Tools / terrain | Example batch-cut script for preparing DEM data. |
| [`PtolemyToolDemData.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolDemData.md) | Tools / terrain | Source-data notes for DEM preparation and coordinate conventions. |
| [`PtolemyToolDemMaker.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolDemMaker.md) | Tools / terrain | DemMaker tool documentation for generating terrain tiles. |
| [`PtolemyToolEncoder.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolEncoder.md) | Tools / imagery | JP2 encoding page, Kakadu usage, and encoder options. |
| [`PtolemyToolHowSignJar.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolHowSignJar.md) | Tools / applet | JAR signing instructions for applet deployment. |
| [`PtolemyToolMsToJp2.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolMsToJp2.md) | Tools / imagery | MSToJp2 map-service-to-JP2 generation tool. |
| [`PtolemyToolPtconsole.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolPtconsole.md) | Tools / web console | ptconsole web application setup and WMS/job workflow. |
| [`PtolemyToolPtconsoleReleaseNotes.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolPtconsoleReleaseNotes.md) | Tools / web console | ptconsole release notes. |
| [`PtolemyToolSetDivider.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolSetDivider.md) | Tools / tiles | Tool for moving JP2/DEM tile files into divider-based directory layouts. |
| [`PtolemyToolsDemCutter.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyToolsDemCutter.md) | Tools / terrain | DemCutter command-line tool documentation. |
| [`PtolemyViewer.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyViewer.md) | Viewer | Short statement of the viewer goal: GPLv3 Java/JOGL, web-page and desktop use, easy extension. |
| [`PtolemyViewerControls.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyViewerControls.md) | Viewer | User controls for mouse, keyboard, and camera movement. |
| [`PtolemyViewerDebugMode.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/PtolemyViewerDebugMode.md) | Viewer | Debug version controls and profiling/rendering toggles. |
| [`SpatialCloud.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/SpatialCloud.md) | Organization | Short page about SpatialCloud sponsorship. |
| [`TableOfContents.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/TableOfContents.md) | Index | Short table-of-contents page. |
| [`WantToBuildVirtualGlobe.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/WantToBuildVirtualGlobe.md) | Background | General background on virtual-globe goals and SDK motivation. |
| [`WfsGmlToPoiPlugin.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/WfsGmlToPoiPlugin.md) | WFS / plugins | WFS request and GML-to-POI transformation notes. |
| [`WfsPoiPlugin.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/WfsPoiPlugin.md) | WFS / plugins | WFS-based POI plugin notes. |
| [`WikiStart.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/WikiStart.md) | Overview | Main wiki landing page describing toolkit goals, demos, quick starts, docs, developer links, and sponsorship. |
| [`pTolemy3d.md`](https://github.com/mwkorver/ptolemy3d/blob/wiki/pTolemy3d.md) | Overview | Short description of the viewer as a Java applet-based 3D map viewer with TMS-style JP2000 and TIN terrain support. |

## Implications for current documentation

The wiki branch supports the documentation emphasis on JP2 imagery and TIN/DEM terrain, but it also shows that the original project scope was broader:

1. A Java/JOGL applet and desktop viewer.
2. A tile pyramid in EPSG:4326 decimal-degree coordinates.
3. JPEG2000 image tile preparation and encoding tools.
4. DEM and TIN terrain preparation tools.
5. Server-side data hosting and Apache configuration.
6. WMS/WFS and GML transformation workflows.
7. A plugin system for vectors, POIs, buildings, logos, XVRML, WFS data, and custom loading.
8. Refoundation notes that already recognized design and maintainability problems in the old codebase.

Future documentation should keep the JP2/TIN pipeline as the primary preservation target, while linking back to the wiki branch for historical behavior, data-preparation details, and applet/plugin usage.
