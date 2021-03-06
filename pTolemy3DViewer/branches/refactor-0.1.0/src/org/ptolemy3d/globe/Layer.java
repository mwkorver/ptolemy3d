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

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.manager.Jp2TileLoader;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;

/**
 * A resolution level.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome Jouvie
 */
public class Layer {
    // Number of tiles in longitude direction

    public final static int LEVEL_NUMTILE_LON = 8;
    // Number of tiles in latitude direction
    public final static int LEVEL_NUMTILE_LAT = 8;
    // Total number of tiles in the level.
    public final static int LEVEL_NUMTILES = LEVEL_NUMTILE_LON * LEVEL_NUMTILE_LAT;

    // Level id
    private final int levelID;

    // List of tiles.
    private Tile[/* LEVEL_NUMTILES */] tiles;

    // Upper left corner latitude
    private int upLeftLon;
    // Upper left corner longitude
    private int upLeftLat;
    // Upper right corner longitude
    private int lowRightLon;
    // Upper right corner latitude
    private int lowRightLat;
    // Tile's size
    private final int tileSize;
    // Layer visibility
    private boolean visible;
    // Altitude is in the range minZoom/maxZoom
    private boolean status;
    // Maximum altitude to render the level
    private final int maxZoom;
    // Minimum altitude to render the level
    private final int minZoom;
    private final int divider;

    /**
     * Creates a new instance.
     *
     * @param levelId
     * @param minZoom
     * @param maxZoom
     * @param tileSize
     * @param divider
     */
    public Layer(int levelId, int minZoom, int maxZoom, int tileSize, int divider) {
        this.levelID = levelId;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.tileSize = tileSize;
        this.divider = divider;

        this.upLeftLon = 0;
        this.upLeftLat = 0;
        this.lowRightLon = 0;
        this.lowRightLat = 0;

        visible = false;
        status = false;

        tiles = new Tile[LEVEL_NUMTILES];
        for (int i = 0; i < LEVEL_NUMTILES; i++) {
            tiles[i] = new Tile(i);
        }
    }

    /** Used internally */
    public void correctTiles(int toptile, int minlat, int maxlat, int minlon,
                             int maxlon, int lon_move, int ysgn, boolean wraps) {
        final Landscape landscape = Ptolemy3D.getScene().landscape;

        upLeftLon = minlon;
        upLeftLat = -minlat;
        lowRightLon = maxlon;
        lowRightLat = -maxlat;

        int k = 0;
        int row = 0;
        int vlat = minlat;
        while (row < LEVEL_NUMTILE_LAT) {
            int vlon = minlon;

            int col = 0;
            while (col < LEVEL_NUMTILE_LON) {
                if (vlon >= landscape.getMaxLongitude()) {
                    vlon = (-landscape.getMaxLongitude() / tileSize) * tileSize;
                    if (vlon != landscape.getMaxLongitude()) {
                        vlon -= tileSize;
                    }
                }

                if ((vlon == minlon) && (col != 0)) {
                    for (; col < LEVEL_NUMTILE_LON; col++) {
                        tiles[k++].status = false;
                    }
                }
                else {
                    tiles[k++].setTile(this, vlon, -vlat);
                    vlon += tileSize;
                    col++;
                }
            }
            vlat += tileSize * ysgn;

            if (vlat <= -landscape.getMaxLatitude()) {
                if (!wraps) {
                    vlat += tileSize;
                    if (minlon > 0) {
                        minlon -= lon_move;
                    }
                    else {
                        minlon += lon_move;
                    }
                    ysgn *= -1;
                }
                else {
                    for (; k < LEVEL_NUMTILE_LON * LEVEL_NUMTILE_LAT; k++) {
                        tiles[k].status = false;
                    }
                    break;
                }
            }

            if (vlat > toptile) {
                vlat = toptile;
                if (minlon > 0) {
                    minlon -= lon_move;
                }
                else {
                    minlon += lon_move;
                }
                ysgn *= -1;
            }
            row++;
        }
    }

    /**
     * Process level visibility.
     */
    public void processVisibility() {
        for (int i = 0; i < LEVEL_NUMTILES; i++) {
            final Tile tile = tiles[i];
            tile.processVisibility();
        }
    }

    /**
     * Draw Level geometry
     */
    public void draw(DrawContext drawContext) {
        GL gl = drawContext.getGL();

        if (!visible) {
            return;
        }

        // Render layer tiles
        for (int i = 0; i < LEVEL_NUMTILE_LAT; i++) {
            for (int j = 0; j < LEVEL_NUMTILE_LON; j++) {
                final int tileID = (i * LEVEL_NUMTILE_LON) + j;
                final Tile tile = tiles[tileID];

                // Bind texture - FIXME Move
                boolean hasTexture = false;
                if (tile != null) {
                    if (tile.isImageDataReady(0)) {
                        int textureId = tile.getCurTextureID();
                        if (textureId == -1) {
                        	final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
                        	byte[] datas = mapDataManager.getImageData(tile.mapData, tile.mapData.curRes);
                            if (datas != null) {
                                final int curRes = tile.getCurRes();
                                final int width = tile.getWidth();
                                final int height = tile.getHeight();

                                textureId = Ptolemy3D.getTextureManager().load(gl, datas, width, height, GL.GL_RGB, true, curRes);

                                tile.setCurTextureID(textureId);
                            }
                        }
                        else {
                            gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
                        }
                        hasTexture = true;
                    }
                }

                // Display
                try {
                	tile.display(gl, hasTexture,
                		(j == (LEVEL_NUMTILE_LON - 1)) ? null : tiles[(i * LEVEL_NUMTILE_LON) + j + 1],
                		(i == (LEVEL_NUMTILE_LAT - 1)) ? null : tiles[((i + 1) * LEVEL_NUMTILE_LON) + j],
                		(j == 0) ? null : tiles[(i * LEVEL_NUMTILE_LON) + j - 1], (i == 0) ? null : tiles[((i - 1) * LEVEL_NUMTILE_LON) + j]);
                }
                catch (Exception e) {
                    IO.printStackRenderer(e);
                }
            }
        }
    }

    /** Level Picking */
    public boolean pick(double[] intersectPoint, double[][] ray) {
        // Start with highest res tiles
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].pick(intersectPoint, ray)) {
                return true;
            }
        }
        return false;
    }

    /** @return true if found */
    public double[] groundHeight(final double lon, final double lat,
                                 final double[][] ray) {

        if (!visible) {
            return null;
        }

        final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();

        MapData mapDataHeader = tileLoader.getHeader(lon, lat, levelID);
        if (mapDataHeader == null) {
            return null;
        }

        final double[] pickArr = {-999, -999, -999};
        if (mapDataHeader.dem != null) {
            /*
             * DEM Elevation
             */
            final byte[] dem = mapDataHeader.dem.demDatas;
            final int numRows = mapDataHeader.dem.numRows;
            final int rowWidth = numRows * 2; // assuming we have a square tile

            float dw = (float) (numRows - 1) / tileSize;
            int xpos = (int) ((lon - mapDataHeader.getLon()) * (dw));
            int ypos = (int) ((mapDataHeader.getLat() - lat) * (dw));
            float sw = (float) tileSize / (numRows - 1);
            double gminx = mapDataHeader.getLon() + xpos * sw;
            double gmaxx = mapDataHeader.getLon() + (xpos + 1) * sw;
            double gminy = mapDataHeader.getLat() - ypos * sw;
            double gmaxy = mapDataHeader.getLat() - (ypos + 1) * sw;

            if (lon <= gminx) {
                gminx = lon - 1;
            }
            if (lon >= gmaxx) {
                gminx = lon + 1;
            }
            if (lat <= gminy) {
                gminy = lat - 1;
            }
            if (lat >= gmaxy) {
                gmaxy = lat + 1;
            }

            Math3D.setTriVert(
                    0,
                    gminx,
                    (dem[(ypos * rowWidth) + (xpos * 2)] << 8) + (dem[((ypos * rowWidth) + (xpos * 2)) + 1] & 0xFF),
                    gminy);
            Math3D.setTriVert(1, gmaxx, (dem[(ypos * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[((ypos * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
                              gminy);
            Math3D.setTriVert(2, gminx, (dem[((ypos + 1) * rowWidth) + (xpos * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + (xpos * 2)) + 1] & 0xFF),
                              gmaxy);
            if (Math3D.rayIntersectTri(pickArr, ray) != 1) {
                // to avoid overlap for ray through tri...
                Math3D.setTriVert(
                        0,
                        gmaxx,
                        (dem[(ypos * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[((ypos * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
                        gminy);
                Math3D.setTriVert(
                        1,
                        gminx,
                        (dem[((ypos + 1) * rowWidth) + (xpos * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + (xpos * 2)) + 1] & 0xFF),
                        gmaxy);
                Math3D.setTriVert(
                        2,
                        gmaxx,
                        (dem[((ypos + 1) * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
                        gmaxy);
                if (Math3D.rayIntersectTri(pickArr, ray) != 1) {
                    pickArr[1] = 0;
                }
            }

            pickArr[1] *= Unit.getCoordSystemRatio();
            return pickArr; // FIXME Must be just over ?
        }
        else if (mapDataHeader.tin != null) {
            /*
             * TIN Elevation
             */
            for (int k = 0; (k < mapDataHeader.tin.nSt.length); k++) {
                if (mapDataHeader.tin.nSt[k] == null) {
                    continue;
                }
                for (int j = 2; (j < mapDataHeader.tin.nSt[k].length); j++) {
                    Math3D.setTriVert(0, mapDataHeader.getLon() + (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 2]][0]),
                                      mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 2]][1], mapDataHeader.getLat() - (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 2]][2]));
                    Math3D.setTriVert(1, mapDataHeader.getLon() + (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 1]][0]),
                                      mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 1]][1], mapDataHeader.getLat() - (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j - 1]][2]));
                    Math3D.setTriVert(2, mapDataHeader.getLon() + (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j]][0]),
                                      mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j]][1], mapDataHeader.getLat() - (mapDataHeader.tin.p[mapDataHeader.tin.nSt[k][j]][2]));
                    if (Math3D.rayIntersectTri(pickArr, ray) == 1) {
                        pickArr[1] *= Unit.getCoordSystemRatio();
                        return pickArr;
                    }
                }
            }
        }
        return null;
    }

    // Getters
    /**
     * Level ID
     *
     * @return the level id (first is 0)
     */
    public int getLevelID() {
        return levelID;
    }

    /**
     * Upper left corner longitude
     *
     * @return the upper left longitude
     */
    public int getUpLeftLon() {
        return upLeftLon;
    }

    /**
     * Upper left corner latitude
     *
     * @return the upper left latitude
     */
    public int getUpLeftLat() {
        return upLeftLat;
    }

    /**
     * Lower right corner longitude
     *
     * @return the lower right longitude
     */
    public int getLowRightLon() {
        return lowRightLon;
    }

    /**
     * Lower right corner latitude
     *
     * @return the lower right latitude
     */
    public int getLowRightLat() {
        return lowRightLat;
    }

    /**
     * Get the Tile at a specified index.
     *
     * @return the Tile at the specified index
     */
    public Tile getTiles(int index) {
        return tiles[index];
    }

    /**
     * Tile size.
     *
     * @return tile width and height
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * @param tiles
     *            the tiles to set
     */
    public void setTiles(Tile[/* LEVEL_NUMTILES */] tiles) {
        this.tiles = tiles;
    }

    /**
     * @return the tiles
     */
    public Tile[/* LEVEL_NUMTILES */] getTiles() {
        return tiles;
    }

    /**
     * @param upLeftLon
     *            the upLeftLon to set
     */
    public void setUpLeftLon(int upLeftLon) {
        this.upLeftLon = upLeftLon;
    }

    /**
     * @param upLeftLat
     *            the upLeftLat to set
     */
    public void setUpLeftLat(int upLeftLat) {
        this.upLeftLat = upLeftLat;
    }

    /**
     * @param lowRightLon
     *            the lowRightLon to set
     */
    public void setLowRightLon(int lowRightLon) {
        this.lowRightLon = lowRightLon;
    }

    /**
     * @param lowRightLat
     *            the lowRightLat to set
     */
    public void setLowRightLat(int lowRightLat) {
        this.lowRightLat = lowRightLat;
    }

    /**
     * @param visible
     *            the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * @return the status
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * @return the maxZoom
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * @return the minZoom
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * @return the divider
     */
    public int getDivider() {
        return divider;
    }
}
