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
package org.ptolemy3d.manager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataManager {
	/** Keep reference of all MapData */
	private final HashMap<MapDataKey, MapData> mapDatas;
	/** Map decoder queue */
	private final MapDecoderQueue decoderQueue;
	
	public MapDataManager() {
		mapDatas = new HashMap<MapDataKey, MapData>();
		decoderQueue = new MapDecoderQueue();
	}
	
	public MapData get(int level, int mapSize, int lon, int lat) {
		final MapDataKey key = new MapDataKey(level, mapSize, lon, lat);
		return get(key);
	}
	public MapData get(MapDataKey key) {
		MapData mapData = mapDatas.get(key);
		if (mapData == null) {
			mapData = new MapData(key.level, key.mapSize, key.lon, key.lat);
			mapDatas.put(mapData.key, mapData);
		}
		return mapData;
	}
	
	public void remove(MapData mapData) {
		// Remove from table
		final MapData removed = mapDatas.remove(mapData.key);
		if (removed == mapData) {
			// Unload texture
		}
		else {
			IO.println("ERROR: fail to remove MapData ("+removed+")");
		}
	}

	// FIXME rename
	public final MapData getMapDataHeader(int level, double lon, double lat) {
		final Iterator<Entry<MapDataKey,MapData>> i = mapDatas.entrySet().iterator();
		while (i.hasNext()) {
			final Entry<MapDataKey,MapData> entry = i.next();
			final MapDataKey key = entry.getKey();
			if ((key.lon <= lon) && ((key.lon + key.mapSize) > lon) &&
				(key.lat >= lat) && ((key.lat - key.mapSize) < lat)) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	public int getTextureID(GL gl, MapData mapData) {
		final TextureManager textureManager = Ptolemy3D.getTextureManager();
		int textureID = textureManager.get(mapData);
		if (textureID == 0) {
			final Texture texture = decoderQueue.request(mapData, 0);	//FIXME resolution
			if(texture != null) {
				textureID = textureManager.load(gl, mapData, texture, true);
			}
		}
		if(textureID == 0) {
			return getAlternateTextureID(gl);
		}
		return textureID;
	}
	public int getAlternateTextureID(GL gl) {
		return 0;
	}
}