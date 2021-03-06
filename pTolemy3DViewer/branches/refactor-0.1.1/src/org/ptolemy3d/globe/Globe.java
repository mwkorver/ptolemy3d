/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ptolemy3d.globe;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
public class Globe {
	private Layer[] layers;

	/** Process visibility of eahc layers */
	public void processVisibility(DrawContext drawContext) {
		final Camera camera = drawContext.getCanvas().getCamera();

		final int lowestLayer = findLowestLayer(camera);
		for (int i = lowestLayer; i >= 0; i--) {
			final boolean areaFilled = isVisibleAreaCovered(camera, lowestLayer, i);
			if(areaFilled) {
				//Outside the view
				for (int j = i; j >= 0; j--) {
					layers[j].setVisible(false);
				}
				break;
			}
			else {
				final Layer layer = layers[i];
				layer.setVisible(true);
				layer.processVisibility(drawContext);
			}
		}
		
		processClipping();
		linkTiles();
	}
	/* Find the lowest visible layer (smaller tiles) */
	private final int findLowestLayer(Camera camera) {
		final double alt = camera.getVerticalAltitudeMeters();
		
		for (int i = layers.length - 1; i >= 0; i--) {
			final Layer layer = layers[i];
			layer.setVisible(false);	//Hide all non visible
			
			boolean visible = (alt <= layer.getMaxZoom());
			if(visible) {
				return i;
			}
		}
		return 0;
	}
	/*  */
	@Deprecated private final static int MAX_DIFFERENT_LAYER = 5;
	private final boolean isVisibleAreaCovered(Camera camera, int lowest, int current) {
		//FIXME Implement it
		int numActive = lowest - current + 1;
		if (numActive > MAX_DIFFERENT_LAYER) {
			return true;
		}
		return false;
	}

	/** Resolve tile clipping.
	 * Tile from different layer may override */
	private void processClipping() {
		for (int i = 0; i < layers.length; i++) {	//Don't change order (always from 0 to max)
			final Layer layer = layers[i];
			layer.processClipping();
		}
	}
	
	/** Link neighboor tiles */
	private void linkTiles() {
		for (int i = 0; i < layers.length - 1; i++) {	//Don't change order (always from 0 to max)
			final Layer layer = layers[i];
			final Layer layerBelow = layers[i+1];
			layer.linkTiles(layerBelow);
		}
	}
	
	public void draw(DrawContext drawContext) {
		for (Layer layer : layers) {
			layer.draw(drawContext);
		}
	}

	/** Landscape Picking: Pick tiles */
	public final boolean pick(Vector3d intersectPoint, Vector3d[] ray) {
		for (int i = layers.length - 1; i >= 0; i--) {
			final Layer layer = layers[i];
			if (layer.pick(intersectPoint, ray)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve the ground height.
	 * @param lon longitude in DD
	 * @param lat latitude in DD
	 * @param minLevel minimum level
	 * @return the ground height, 0 is the reference value (no elevation).
	 */
	public double groundHeight(double lon, double lat, int minLevel) {
		Vector3d[] ray = {
			new Vector3d(lon, CameraMovement.MAXIMUM_ALTITUDE, lat),
			new Vector3d(lon, CameraMovement.MAXIMUM_ALTITUDE - 1, lat)
		};

		for (int i = layers.length - 1; (i >= minLevel); i--) {
			final Layer layer = layers[i];

			try {
				Vector3d pickArr = layer.groundHeight(lon, lat, ray);
				if (pickArr != null) {
					return pickArr.y;
				}
			}
			catch (RuntimeException e) {
				IO.printStackRenderer(e);
			}
		}
		return 0;
	}
	
	/** */
	public MapDataKey getCloserMap(int layerID, int lon, int lat) {
		final int tileSize = getLayer(layerID).getTileSize();
		
		final int refLon = (lon / tileSize) * tileSize;
		final int refLat = (lat / tileSize) * tileSize;
		
		final int closeLon;
		if((lon >= refLon) && (lon < refLon+tileSize)) {
			closeLon = refLon;
		}
		else {
			closeLon = refLon - tileSize;
		}
		
		final int closeLat;
		if((lat <= refLat) && (lat > refLat-tileSize)) {
			closeLat = refLat;
		}
		else {
			closeLat = refLat + tileSize;
		}
		
		return new MapDataKey(layerID, closeLon, closeLat);
	}
	
	public double getMapDistanceFromCamera(MapDataKey mapDataKey, Camera camera) {
		final double camLon = camera.getCameraX();
		final double camLat = camera.getCameraY();
		
		final Layer layer = getLayer(mapDataKey.layer);
		double tileLon = mapDataKey.lon + (layer.getTileSize() / 2);
		double tileLat = mapDataKey.lat + (layer.getTileSize() / 2);
		
		double dist = Math3D.distance2D(camLon, tileLon, camLat, tileLat);

		if (camLon < 0) {
			tileLon = camLon + (Landscape.MAX_LONGITUDE - mapDataKey.lon) + (camLon         + Landscape.MAX_LONGITUDE);
		}
		else {
			tileLon = camLon + (Landscape.MAX_LONGITUDE - camLon        ) + (mapDataKey.lon + Landscape.MAX_LONGITUDE);
		}

		double dist2 = Math3D.distance2D(camLon, tileLon, camLat, tileLat);
		return ((dist < dist2) ? dist : dist2);
	}

	public int getTileSize(int layerID) {
		return layers[layerID].getTileSize();
	}

	/** @param layers the levels to set */
	public void setLevels(Layer[] layers) {
		this.layers = layers;
	}
	/** @return the levels */
	public int getNumLayers() {
		return layers.length;
	}
	/** @return the levels */
	public Layer getLayer(int layerID) {
		return layers[layerID];
	}
}
