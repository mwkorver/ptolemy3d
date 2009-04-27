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

import org.ptolemy3d.manager.Texture;

/**
 * This class holds info for our tile data
 */
public class MapData {
	/** Number of resolution per jp2 tiles. */
	public final static int MAX_NUM_RESOLUTION = 4;

	/** Longitude in DD. */
	public final MapDataKey key;
	/** Jp2 resolutions */
	public final Texture[] wavelets;
	/** Current resolution */
	public int curRes = 0;
	/** TIN Elevation */
	public ElevationTin tin;
	/** DEM Elevation */
	public ElevationDem dem;

	/** We must go through manager to create a MapData, to avoid duplication.
	 * @deprecated Use MapDataManager instead */
	public MapData(MapDataKey mapDataKey) {
		key = mapDataKey;
		wavelets = new Texture[MAX_NUM_RESOLUTION];
		
		tin = null;
		dem = null;
		
		curRes = 0;
	}

	@Override
	public boolean equals(Object obj) {
		return key.equals(obj);
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	@Override
	public String toString() {
		return super.toString()+"["+key.toString()+"]";
	}

	/** @return true if the map has elevation data */
	public boolean hasElevation() {
		return (tin != null) || (dem != null);
	}

	/** */
	public int getLon() { return key.lon; }
	/** */
	public int getLat() { return key.lat; }
	/** */
	public int getLevel() { return key.level; }
	/** */
	public int getMapSize() { return key.mapSize; }
	/** */
	public Texture getWavelet(int res) { return wavelets[res]; }
}