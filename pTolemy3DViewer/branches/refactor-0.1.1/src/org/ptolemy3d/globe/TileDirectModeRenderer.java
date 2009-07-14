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

import static org.ptolemy3d.debug.Config.DEBUG;
import static org.ptolemy3d.Unit.EARTH_RADIUS;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.globe.Tile.TileBounds;
import org.ptolemy3d.globe.Tile.TileRenderer;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
class TileDirectModeRenderer implements TileRenderer {
	/* All of that are temporary datas that are used in drawSubsection.
	 * They must be restore in each entering */

	protected GL gl;
	protected MapData mapData;

	//From the Tile
	protected Tile tile;
	protected TileBounds subTile;
	protected int drawLevelID;
	protected int layerID;
	protected Tile leftTile;
	protected Tile aboveTile;
	protected Tile rightTile;
	protected Tile belowTile;
	protected int refLeftLon, refUpLat;
	protected int refRightLon, refLowLat;
	protected int tileSize;
	protected Landscape landscape;
	protected float[] tileColor;
	protected float[] colratios;
	protected double terrainScaler;
	protected double oneOverDDToRad;
	
	protected void fillLocalVariables(Tile tile, TileBounds subTile) {
		gl = tile.gl;

		landscape = Ptolemy3D.getScene().getLandscape();
		tileColor = landscape.getTileColor();
		colratios = landscape.getColorRatios();
		terrainScaler = landscape.getTerrainScaler();
		
		oneOverDDToRad = Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();
		
		this.tile = tile;
		this.mapData = tile.mapData;
		this.subTile = subTile;
		
		drawLevelID = tile.drawLevelID;
		layerID = tile.getLayerID();
		tileSize = tile.getTileSize();
		leftTile = tile.left;
		aboveTile = tile.above;
		rightTile = tile.right;
		belowTile = tile.below;
		refLeftLon = tile.getReferenceLeftLongitude();
		refUpLat = tile.getReferenceUpperLatitude();
		refRightLon = tile.getReferenceRightLongitude();
		refLowLat = tile.getReferenceLowerLatitude();
	}

	public void renderSubTile(Tile tile, TileBounds subTile) {
		final int x1 = subTile.ulx;
		final int z1 = subTile.ulz;
		final int x2 = subTile.lrx;
		final int z2 = subTile.lrz;
		if ((x1 == x2) || (z1 == z2)) {
			return;
		}

		if (DEBUG) {
			if (!ProfilerUtil.renderTiles) {
				return;
			}
			ProfilerUtil.tileSectionCounter++;
		}
		fillLocalVariables(tile, subTile);

		boolean lookForElevation = (mapData != null) && (landscape.isTerrainEnabled());
		boolean useDem = lookForElevation && (mapData.dem != null);
		boolean useTin = lookForElevation && (mapData.tin != null);

		boolean texture = loadGLState(tile.texture);
		if (texture) {
			if (useDem) {
				renderSubTile_DemTextured(x1, z1, x2, z2);
			}
			else if (useTin) {
				renderSubTile_Tin(true, x1, z1, x2, z2);
			}
			else {
				renderSubTile_Textured();
			}
		}
		else {
			if (useDem) {
				renderSubTile_Dem(x1, z1, x2, z2);
			}
			else if (useTin) {
				renderSubTile_Tin(false, x1, z1, x2, z2);
			}
			else {
				renderSubTile();
			}
		}
	}

	protected void renderSubTile_DemTextured(int x1, int z1, int x2, int z2) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final byte[] dem = mapData.dem.demDatas;
		final int numRows = mapData.dem.size;

		final int rowWidth = numRows * 2;	// assuming we have a square tile
		final float tex_inc = 1.0f / (numRows - 1);

		final boolean xsinterpolate, xeinterpolate, zsinterpolate, zeinterpolate;
		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int startx, startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.getTileSize();

			startxcoord = ((x1 - refLeftLon) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((z1 - refUpLat) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((x2 - refLeftLon) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) {
				endx++;
			}

			endzcoord = ((z2 - refUpLat) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) {
				endz++;
			}
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (x1 == refLeftLon) && (x2 == refRightLon) && (z1 == refUpLat) && (z2 == refLowLat)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[     0] << 8) + (dem[     1] & 0xFF);
			ur_corner = (dem[rowWidthMinusTwo] << 8) + (dem[rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1] << 8) + (dem[i1 + 1] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.mapData == null || leftTile.mapData.dem == null) || (leftTile.mapData.key.layer != drawLevelID)) {
				left_dem_slope = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.mapData == null || rightTile.mapData.dem == null) || (rightTile.mapData.key.layer != drawLevelID)) {
				right_dem_slope = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.mapData == null || aboveTile.mapData.dem == null) || (aboveTile.mapData.key.layer != drawLevelID)) {
				top_dem_slope = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.mapData == null || belowTile.mapData.dem == null) || (belowTile.mapData.key.layer != drawLevelID)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;

			theta1 = x1 * oneOverDDToRad;
			theta2 = x2 * oneOverDDToRad;

			phi1 = z1 * oneOverDDToRad;
			phi2 = z2 * oneOverDDToRad;

			dTetaOverN = (theta2 - theta1) * oneOverNrowsX;
			dPhiOverN = (phi2 - phi1) * oneOverNrowsZ;
		}

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		double dz = phi1;
		double cosZ = Math.cos(dz);
		double sinZ = Math.sin(dz);
		double cos2Z, sin2Z;

		int pos_d1 = (startz * rowWidth) + (startx * 2);
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++) //startz can be equal to (endz-1)
		{
			final double d2z = dz + dPhiOverN;

			cos2Z = Math.cos(d2z);
			sin2Z = Math.sin(d2z);

			final float tex_z, tex_z2;
			final double t_sz_wt, t_ez_wt, b_sz_wt, b_ez_wt;
			if ((i == startz) && (zsinterpolate)) {
				tex_z = (float) (tex_inc * startzcoord);
				t_ez_wt = startzcoord - startz;
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				tex_z = tex_inc * i;
				t_ez_wt = 0;
				t_sz_wt = 1;
			}
			if ((i == (endz - 1)) && (zeinterpolate)) {
				tex_z2 = (float) (tex_inc * endzcoord);
				b_sz_wt = endz - endzcoord;
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				tex_z2 = tex_inc * (i + 1);
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			double dx = theta1;
			for (int j = startx; j < endx; j++) {
				final int pos_d2 = pos_d1 + rowWidth;

				final float tex_x;
				final double sx_wt,  ex_wt;
				final int pos_r1,  pos_r2;
				if (xsinterpolate && (j == startx)) {
					ex_wt = startxcoord - startx;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((startx + 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float) (tex_inc * startxcoord);
				}
				else if (xeinterpolate && (j == (endx - 1))) {
					ex_wt = endx - endxcoord;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((j - 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float) (tex_inc * (endxcoord - 1));
				}
				else {
					ex_wt = 0.0;
					sx_wt = 1;

					pos_r1 = pos_d1;
					pos_r2 = pos_d2;

					tex_x = j * tex_inc;
				}

				double dy1, dy2;
				if (eqZLevel) {
					if ((j == startx) && (left_dem_slope != -1)) {
						dy1 = ul_corner + (i * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((j == (endx - 1)) && (right_dem_slope != -1)) {
						dy1 = ur_corner + (i * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((i == startz) && (top_dem_slope != -1)) {
						dy1 = ul_corner + (j * top_dem_slope);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
					else if ((i == (endz - 1)) && (bottom_dem_slope != -1)) {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = ll_corner + (j * bottom_dem_slope);
					}
					else {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
				}
				else {
					double f1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt;
					double f2 = ((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt;
					double f3 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt;
					double f4 = ((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt;

					dy1 = (f1 * t_sz_wt) + (f2 * t_sz_wt) + (f3 * t_ez_wt) + (f4 * t_ez_wt);
					dy2 = (f1 * b_sz_wt) + (f2 * b_sz_wt) + (f3 * b_ez_wt) + (f4 * b_ez_wt);
				}

				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double dy1E = (dy1 * dyScaler) + EARTH_RADIUS;
					final double dy2E = (dy2 * dyScaler) + EARTH_RADIUS;

					final double cosX = Math.cos(dx);
					final double sinX = Math.sin(dx);

					cx1 = dy1E * cosZ * sinX;
					cx2 = dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 = dy1E * cosZ * cosX;
					cz2 = dy2E * cos2Z * cosX;
				}

				gl.glTexCoord2f(tex_x, tex_z);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tex_x, tex_z2);
				gl.glVertex3d(cx2, cy2, cz2);

				pos_d1 += 2;
				dx += dTetaOverN;
			}
			gl.glEnd();

			dz = d2z;
			cosZ = cos2Z;
			sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * (endx - startx) * (endz - startz);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected void renderSubTile_Dem(int x1, int z1, int x2, int z2) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final byte[] dem = mapData.dem.demDatas;
		final int numRows = mapData.dem.size;
		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);

		final int rowWidth = numRows * 2;	// assuming we have a square tile

		final boolean xsinterpolate,  xeinterpolate,  zsinterpolate,  zeinterpolate;
		final double startxcoord,  startzcoord,  endxcoord,  endzcoord;
		final int startx,  startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.getTileSize();

			startxcoord = ((x1 - refLeftLon) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((z1 - refUpLat) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((x2 - refLeftLon) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) {
				endx++;
			}

			endzcoord = ((z2 - refUpLat) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) {
				endz++;
			}
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (x1 == refLeftLon) && (x2 == refRightLon) && (z1 == refUpLat) && (z2 == refLowLat)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[0] << 8) + (dem[   1] & 0xFF);
			ur_corner = (dem[rowWidthMinusTwo] << 8) + (dem[rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1] << 8) + (dem[i1 + 1] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.mapData == null) || (leftTile.mapData.key.layer != drawLevelID)) {
				left_dem_slope = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.mapData == null) || (rightTile.mapData.key.layer != drawLevelID)) {
				right_dem_slope = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.mapData == null) || (aboveTile.mapData.key.layer != drawLevelID)) {
				top_dem_slope = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.mapData == null) || (belowTile.mapData.key.layer != drawLevelID)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;

			theta1 = x1 * oneOverDDToRad;
			theta2 = x2 * oneOverDDToRad;

			phi1 = z1 * oneOverDDToRad;
			phi2 = z2 * oneOverDDToRad;

			dTetaOverN = (theta2 - theta1) * oneOverNrowsX;
			dPhiOverN = (phi2 - phi1) * oneOverNrowsZ;
		}

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		double dz = phi1;
		double cosZ = Math.cos(dz);
		double sinZ = Math.sin(dz);
		double cos2Z, sin2Z;

		int pos_d1 = (startz * rowWidth) + (startx * 2);
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++) {
			final double d2z = dz + dPhiOverN;

			cos2Z = Math.cos(d2z);
			sin2Z = Math.sin(d2z);

			final double t_sz_wt,  t_ez_wt,  b_sz_wt,  b_ez_wt;
			if ((i == startz) && (zsinterpolate)) {
				t_ez_wt = startzcoord - startz;
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				t_ez_wt = 0;
				t_sz_wt = 1;
			}
			if ((i == (endz - 1)) && (zeinterpolate)) {
				b_sz_wt = endz - endzcoord;
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			double dx = theta1;
			for (int j = startx; j < endx; j++) {
				final int pos_d2 = pos_d1 + rowWidth;

				final double sx_wt,  ex_wt;
				final int pos_r1,  pos_r2;
				if (xsinterpolate && (j == startx)) {
					ex_wt = startxcoord - startx;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((startx + 1) * 2);
					pos_r2 = pos_r1 + rowWidth;
				}
				else if (xeinterpolate && (j == (endx - 1))) {
					ex_wt = endx - endxcoord;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((j - 1) * 2);
					pos_r2 = pos_r1 + rowWidth;
				}
				else {
					ex_wt = 0.0;
					sx_wt = 1;

					pos_r1 = pos_d1;
					pos_r2 = pos_d2;
				}

				double dy1, dy2;
				if (eqZLevel) {
					if ((j == startx) && (left_dem_slope != -1)) {
						dy1 = ul_corner + (i * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((j == (endx - 1)) && (right_dem_slope != -1)) {
						dy1 = ur_corner + (i * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((i == startz) && (top_dem_slope != -1)) {
						dy1 = ul_corner + (j * top_dem_slope);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
					else if ((i == (endz - 1)) && (bottom_dem_slope != -1)) {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = ll_corner + (j * bottom_dem_slope);
					}
					else {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
				}
				else {
					double f1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt;
					double f2 = ((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt;
					double f3 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt;
					double f4 = ((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt;

					dy1 = (f1 * t_sz_wt) + (f2 * t_sz_wt) + (f3 * t_ez_wt) + (f4 * t_ez_wt);
					dy2 = (f1 * b_sz_wt) + (f2 * b_sz_wt) + (f3 * b_ez_wt) + (f4 * b_ez_wt);
				}

				dy1 *= dyScaler;	//dy1 used later
				dy2 *= dyScaler;	//dy2 used later

				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double dy1E = dy1 + EARTH_RADIUS;
					final double dy2E = dy2 + EARTH_RADIUS;

					final double cosX = Math.cos(dx);
					final double sinX = Math.sin(dx);

					cx1 = dy1E * cosZ * sinX;
					cx2 = dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 = dy1E * cosZ * cosX;
					cz2 = dy2E * cos2Z * cosX;
				}

				if (useColor) {
					setColor((float) dy1);
				}
				gl.glVertex3d(cx1, cy1, cz1);

				if (useColor) {
					setColor((float) dy2);
				}
				gl.glVertex3d(cx2, cy2, cz2);

				pos_d1 += 2;
				dx += dTetaOverN;
			}
			gl.glEnd();

			dz = d2z;
			cosZ = cos2Z;
			sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * (endx - startx) * (endz - startz);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * ((useColor ? 3 * 4 : 0) + 3 * 8);
		}
	}

	protected void renderSubTile_Textured() {
		// Elevation
		final ElevationNone elevation = new ElevationNone(tile, subTile);
		
		final int nLon = elevation.numPolyLon;
		final int nLat = elevation.numPolyLat;
		
		final double startLon = elevation.polyLonStart * oneOverDDToRad;
		final double startLat = elevation.polyLatStart * oneOverDDToRad;
		
		final double dLon = elevation.polySizeLon * oneOverDDToRad;
		final double dLat = elevation.polySizeLat * oneOverDDToRad;
		
		final double lonOffsetStart = elevation.polySizeLonOffsetStart * oneOverDDToRad;
		final double lonOffsetEnd = elevation.polySizeLonOffsetEnd * oneOverDDToRad;
		final double latOffsetStart = elevation.polySizeLatOffsetStart * oneOverDDToRad;
		final double latOffsetEnd = elevation.polySizeLatOffsetEnd * oneOverDDToRad;
		
		// Texture
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final float oneOverTileWidth = 1.0f / drawLevel.getTileSize();
		
		float uvLonStart = (subTile.ulx - refLeftLon) * oneOverTileWidth;
		float uvLatStart = (subTile.ulz - refUpLat) * oneOverTileWidth;

		float dUvLon = elevation.polySizeLon * oneOverTileWidth;
		float dUvLat = elevation.polySizeLat * oneOverTileWidth;
		
		double t1 = startLat;
		double cosT1_E =  Math.cos(t1) * EARTH_RADIUS;
		double sinT1_E = -Math.sin(t1) * EARTH_RADIUS;

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		float ty1 = uvLatStart;
		for (int j = 0; j < nLat; j++) {
			double t2;
			float ty2;
			{
				t2 = t1 + dLat;
				ty2 = ty1 + dUvLat;
				if(j == 0) {
					t2 += latOffsetStart;
					ty2 += elevation.polySizeLatOffsetStart * oneOverTileWidth;
				}
				if(j == nLat-1) {
					t2 += latOffsetEnd;
					ty2 += elevation.polySizeLatOffsetEnd * oneOverTileWidth;
				}
			}

			final double cosT2_E, sinT2_E;
			cosT2_E =  Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = -Math.sin(t2) * EARTH_RADIUS;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			double t3 = startLon;
			float tx = uvLonStart;
			for (int i = 0; i <= nLon; i++) {
				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double cosT3 = Math.cos(t3);
					final double sinT3 = Math.sin(t3);

					cx1 = cosT1_E * sinT3;
					cy1 = sinT1_E;
					cz1 = cosT1_E * cosT3;

					cx2 = cosT2_E * sinT3;
					cy2 = sinT2_E;
					cz2 = cosT2_E * cosT3;
				}

				gl.glTexCoord2f(tx, ty1);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tx, ty2);
				gl.glVertex3d(cx2, cy2, cz2);
				
				t3 += dLon;
				tx += dUvLon;
				if(i == 0) {
					t3 += lonOffsetStart;
					tx += elevation.polySizeLonOffsetStart * oneOverTileWidth;
				}
				if(i == nLon-1) {
					t3 += lonOffsetEnd;
					tx += elevation.polySizeLonOffsetEnd * oneOverTileWidth;
				}
			}
			gl.glEnd();
			
			t1 = t2;
			ty1 = ty2;
			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * nLat * (nLon + 1);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected void renderSubTile() {
		// Elevation
		final ElevationNone elevation = new ElevationNone(tile, subTile);
		
		final int nLon = elevation.numPolyLon;
		final int nLat = elevation.numPolyLat;
		
		final double startLon = elevation.polyLonStart * oneOverDDToRad;
		final double startLat = elevation.polyLatStart * oneOverDDToRad;
		
		final double dLon = elevation.polySizeLon * oneOverDDToRad;
		final double dLat = elevation.polySizeLat * oneOverDDToRad;
		
		final double lonOffsetStart = elevation.polySizeLonOffsetStart * oneOverDDToRad;
		final double lonOffsetEnd = elevation.polySizeLonOffsetEnd * oneOverDDToRad;
		final double latOffsetStart = elevation.polySizeLatOffsetStart * oneOverDDToRad;
		final double latOffsetEnd = elevation.polySizeLatOffsetEnd * oneOverDDToRad;
		
		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);
		if (useColor) {
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}

		double t1 = startLat;
		double cosT2_E =  Math.cos(t1) * EARTH_RADIUS;
		double sinT2_E = -Math.sin(t1) * EARTH_RADIUS;
		
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int j = 0; j < nLat; j++) {
			double t2 = t1 + dLat;
			if(j == 0) {
				t2 += latOffsetStart;
			}
			if(j == nLat-1) {
				t2 += latOffsetEnd;
			}

			final double cosT1_E, sinT1_E;
			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
			cosT2_E = Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = -Math.sin(t2) * EARTH_RADIUS;

			double t3 = startLon;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= nLon; i++) {
				final double cosT3 = Math.cos(t3);
				final double sinT3 = Math.sin(t3);

				double cx1, cy1, cz1, cx2, cy2, cz2;
				cx1 = cosT1_E * sinT3;
				cx2 = cosT2_E * sinT3;
				cy1 = sinT1_E;
				cy2 = sinT2_E;
				cz1 = cosT1_E * cosT3;
				cz2 = cosT2_E * cosT3;

				gl.glVertex3d(cx1, cy1, cz1);
				gl.glVertex3d(cx2, cy2, cz2);

				t3 += dLon;
				if(i == 0) {
					t3 += lonOffsetStart;
				}
				if(i == nLon-1) {
					t3 += lonOffsetEnd;
				}
			}
			gl.glEnd();
			
			t1 = t2;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * nLat * (nLon + 1);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
		}
	}

	protected void renderSubTile_Tin(boolean texture, int x1, int z1, int x2, int z2) {
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;

		// corners clockwise, from ul
		if ((leftTile == null) || (rightTile.mapData != null && leftTile.mapData.key.layer != drawLevelID)) {
			left_dem_slope = (double) (mapData.tin.positions[3][1] - mapData.tin.positions[0][1]) / mapData.tin.w;
		}
		if ((rightTile == null) || (rightTile.mapData != null && rightTile.mapData.key.layer != drawLevelID)) {
			right_dem_slope = (double) (mapData.tin.positions[2][1] - mapData.tin.positions[1][1]) / mapData.tin.w;
		}
		if ((aboveTile == null) || (aboveTile.mapData != null && aboveTile.mapData.key.layer != drawLevelID)) {
			top_dem_slope = (double) (mapData.tin.positions[1][1] - mapData.tin.positions[0][1]) / mapData.tin.w;
		}
		if ((belowTile == null) || (belowTile.mapData != null && belowTile.mapData.key.layer != drawLevelID)) {
			bottom_dem_slope = (double) (mapData.tin.positions[2][1] - mapData.tin.positions[3][1]) / mapData.tin.w;
		}

		double theta1, dThetaOverW;
		double phi1, dPhiOverW;
		{
			double phi2, theta2;
			
			theta1 = x1 * oneOverDDToRad; // startx
			theta2 = x2 * oneOverDDToRad; // endx

			phi1 = z1 * oneOverDDToRad; // starty
			phi2 = z2 * oneOverDDToRad; // endy

			double oneOverW = 1.0 / mapData.tin.w;
			dThetaOverW = (theta2 - theta1) * oneOverW;
			dPhiOverW = (phi2 - phi1) * oneOverW;
		}

		float oneOverW = 1.0f / mapData.tin.w;
		for (int i = 0; i < mapData.tin.indices.length; i++) {
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int j = 0; j < mapData.tin.indices[i].length; j++) {
				int v = mapData.tin.indices[i][j];

				double dx, dy, dz;
				{
					final float[] p = mapData.tin.positions[v];
					final float tinW = mapData.tin.w;

					dy = p[1];
					if ((left_dem_slope != -1) && (p[0] == 0)) {
						dy = mapData.tin.positions[0][1] + (p[2] * left_dem_slope);
					}
					if ((right_dem_slope != -1) && (p[0] == tinW)) {
						dy = mapData.tin.positions[1][1] + (p[2] * right_dem_slope);
					}
					if ((top_dem_slope != -1) && (p[2] == 0)) {
						dy = mapData.tin.positions[0][1] + (p[0] * top_dem_slope);
					}
					if ((bottom_dem_slope != -1) && (p[2] == tinW)) {
						dy = mapData.tin.positions[3][1] + (p[0] * bottom_dem_slope);
					}
					dy *= terrainScaler;

					dx = theta1 + p[0] * dThetaOverW;
					dz = phi1 + p[2] * dPhiOverW;
				}

				double tx, ty, tz;
				{
					double dyE = EARTH_RADIUS + dy;
					double sinZ = Math.sin(dz);
					double cosZ = Math.cos(dz);
					double sinX = Math.sin(dx);
					double cosX = Math.cos(dx);

					tx =  dyE * cosZ * sinX;
					ty = -dyE * sinZ;
					tz =  dyE * cosZ * cosX;
				}

				if (texture) {
					gl.glTexCoord2f((mapData.tin.positions[v][0] * oneOverW), (mapData.tin.positions[v][2] * oneOverW));
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy);
				}
				gl.glVertex3d(tx, ty, tz);
			}
			gl.glEnd();

			if (DEBUG) {
				int numVertices = mapData.tin.indices[i].length;
				ProfilerUtil.vertexCounter += numVertices;
				if (texture) {
					ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 4 + 3 * 8);
				}
				else {
					ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
				}
			}
		}
	}

	protected final void setColor(float y) {
		if (y > landscape.getMaxColorHeight()) {
			y = landscape.getMaxColorHeight();
		}

		gl.glColor3f(tileColor[0] + y * colratios[0],
				tileColor[1] + y * colratios[1],
				tileColor[2] + y * colratios[2]);
	}

	protected final void setJP2ResolutionColor() {
		if (DEBUG) {
			int tileRes = (mapData == null) ? -1 : mapData.mapResolution;
			switch (tileRes) {
				case 0:
					gl.glColor3f(1.0f, 0.0f, 0.0f);
					break;
				case 1:
					gl.glColor3f(0.0f, 1.0f, 0.0f);
					break;
				case 2:
					gl.glColor3f(0.0f, 0.0f, 1.0f);
					break;
				case 3:
					gl.glColor3f(1.0f, 1.0f, 1.0f);
					break;
				default:
					gl.glColor3f(0.5f, 0.5f, 0.5f);
				break;
			}
		}
	}

	protected final void setLevelIDColor() {
		if (DEBUG) {
			int levelID = (mapData == null) ? 0 : mapData.getLevel();
			float scaler = 1 - (levelID / 3) / 3.0f;
			switch (1 + levelID % 3) {
				default:
					gl.glColor3f(0.25f, 0.25f, 0.25f);
				break;
				case 1:
					gl.glColor3f(scaler, 0.0f, 0.0f);
					break;
				case 2:
					gl.glColor3f(0.0f, scaler, 0.0f);
					break;
				case 3:
					gl.glColor3f(0.0f, 0.0f, scaler);
					break;
			}
		}
	}

	protected final void setTileIDColor() {
		if (DEBUG) {
			final float C0 = 0.0f;
			final float C1 = 1.0f;
			final float C2 = 0.8f;
			final float C3 = 0.6f;
			final float C4 = 0.4f;

			int tileID = ProfilerUtil.tileCounter;

			float scaler = 1 - (tileID / 27) / 8.0f;
			tileID = tileID % 27;
			switch (tileID) {
				case 0:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 1:
					gl.glColor3f(scaler * C1, scaler * C2, scaler * C0);
					break;
				case 2:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C2);
					break;
				case 3:
					gl.glColor3f(scaler * C2, scaler * C0, scaler * C1);
					break;
				case 4:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 5:
					gl.glColor3f(scaler * C0, scaler * C2, scaler * C1);
					break;
				case 6:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C2);
					break;
				case 7:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 8:
					gl.glColor3f(scaler * C2, scaler * C1, scaler * C0);
					break;

				case 9:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 10:
					gl.glColor3f(scaler * C1, scaler * C3, scaler * C0);
					break;
				case 11:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C3);
					break;
				case 12:
					gl.glColor3f(scaler * C3, scaler * C0, scaler * C1);
					break;
				case 13:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 14:
					gl.glColor3f(scaler * C0, scaler * C3, scaler * C1);
					break;
				case 15:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C3);
					break;
				case 16:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 17:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;

				case 18:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 19:
					gl.glColor3f(scaler * C1, scaler * C4, scaler * C0);
					break;
				case 20:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C4);
					break;
				case 21:
					gl.glColor3f(scaler * C4, scaler * C0, scaler * C1);
					break;
				case 22:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 23:
					gl.glColor3f(scaler * C0, scaler * C4, scaler * C1);
					break;
				case 24:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C4);
					break;
				case 25:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 26:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
			}
		}
	}

	protected final boolean loadGLState(boolean texture) {
		if (landscape.getDisplayMode() == Landscape.DISPLAY_JP2RES) {
			setJP2ResolutionColor();
			return false;
		}
		else if (landscape.getDisplayMode() == Landscape.DISPLAY_TILEID) {
			setTileIDColor();
			return false;
		}
		else if (landscape.getDisplayMode() == Landscape.DISPLAY_LEVELID) {
			setLevelIDColor();
			return false;
		}
		else if (landscape.getDisplayMode() == Landscape.DISPLAY_MESH ||
				landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
			return false;
		}
		if (texture) {
			gl.glEnable(GL.GL_TEXTURE_2D);
			return true;
		}
		else {
			gl.glDisable(GL.GL_TEXTURE_2D);
			return false;
		}
	}
}
