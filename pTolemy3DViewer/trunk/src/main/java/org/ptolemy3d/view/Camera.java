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
package org.ptolemy3d.view;

import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Matrix9d;

/**
 * <H1>Camera Coordinate System</H1>
 * <BR>
 * The view position is defined with the position (longitude, latitude, altitude, direction, tilt).<BR>
 * <BR>
 * The longitude and latitude components are angles in the ranges respectively [-180�;180�] and [-90�;90�].
 * Those two angles define every position on the globe.<BR>
 * <BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-LongitudeLatitude.jpg" width=256 height=256><BR>
 * Longitude / Latitude angles</div><BR>
 * <BR>
 * The view always look towards that position, but from any possible orientation.
 * The view orientation is describe with the two angles (direction, tilt).<BR>
 * <BR>
 * The reference view orientation is as follow: the view up direction is tangent to the longitude line, the view forward is in direction to the globe center.<BR>
 * The view vectors are represented on the next picture:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-RefDirectionPitch.jpg" width=256 height=256></div><BR>
 * <BR>
 * The first angle <code>direction</code> defines the rotation around the vertical (upward) direction:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Direction.jpg" width=256 height=256></div><BR>
 * <BR>
 * The second angle <code>tilt</code> is the rotation around the side vector:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Tilt.jpg" width=256 height=256></div><BR>
 * <BR>
 * Finally the altitude tell how far we are from the position on the globe, along the view forward vector.<BR>
 * This last picture represents the view vectors and the view position:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Altitude.jpg" width=256 height=256></div><BR>
 * <BR>
 * <BR>
 * <H1>Camera & Cartesian Coordinate System Relation</H1>
 * <BR>
 * Here are relations between cartesian axis and longitude/latitude:<BR>
 * <ul>
 * <li>X axis: longitude=180�, latitude=0�</li>
 * <li>Y axis: longitude=90�, latitude=any angles</li>
 * <li>Z axis: longitude=90�, latitude=0�</li>
 * </ul>
 * From that, you can find easily those relations :<BR>
 * <ul>
 * <li>-X axis: longitude=0�, latitude=0�</li>
 * <li>-Y axis: longitude=-90�, latitude=any angles</li>
 * <li>-Z axis: longitude=-90�, latitude=0�</li>
 * </ul>
 * <BR>
 * <i><u>Remark:</u> All angles are expressed modulos 360� (180� equals to -180� and 540� ...)</i><BR>
 * <BR>
 * <BR>
 * <H1>Positioning</H1>
 * <BR>
 * View movements are controlled in the coordinate system describe in the previous paragraph.<BR>
 * View movements is controlled from CameraMovement, for more information, see the <a href="CameraMovement.html">CameraMovement documentation</a>.<BR>
 * <BR>
 * Sometimes you'll may find usefull to convert that position into another coordinate system.<BR>
 * You can get the equivalent cartesian coordinates (x,y,z), using the method <code>getCartesianPosition</code>.<BR>
 * Axis system of cartesian coordinate system is shown in the final picture:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Cartesian.jpg" width=256 height=256></div><BR>
 * <BR>
 * @see CameraMovement
 * @see #getCartesianPosition
 */
public class Camera
{
	protected final LatLonAlt latLonAlt;
	/** Direction defined with a rotation around the vertical axis <i>(axis that goes from the earth center to the longitude/latitude position)</i>. */
	protected double direction;
	/** Tilt defined with a rotation around the side axis. */
	protected double tilt;
	/** Vertical altitude in meters. 0 is the ground reference (ground with no elevation). */
	protected double vertAlt, vertAltDD;

	public double cameraX = 0, cameraY = 0;

	/** Cartesian position */
	public double[] cameraPos = new double[3];

	public Matrix9d vpMat = new Matrix9d();
	public Matrix9d cameraMat = new Matrix9d();
	public Matrix16d cameraMatInv = Matrix16d.identity();

	public Camera()
	{
		this.latLonAlt = LatLonAlt.fromDD(0, 0, 5000);
		this.vertAlt = 0;
		this.direction = 0.0;
		this.tilt = 0;
	}

	/** @return the altitude in world space (vertical altitude), zero is the altitude of the ground with no elevation. */
	public final double getVerticalAltitudeMeters()
	{
		return vertAlt;
	}
	
	public final LatLonAlt getLatAltLon()
	{
		return latLonAlt;
	}

	/** @return direction: unit is degrees. */
	public final double getDirectionDegrees()
	{
		return direction * Math3D.radToDeg;
	}
	/** @return direction: unit is radians. */
	public final double getDirectionRadians()
	{
		return direction;
	}
	/** @return pitch (tilt): unit is degrees. */
	public final double getPitchDegrees()
	{
		return tilt * Math3D.radToDeg;
	}
	/** @return pitch (tilt): unit is radians. */
	public final double getPitchRadians()
	{
		return tilt;
	}

	/** @param position destination array to write cartesian position in. */
	public final void getCartesianPosition(double[] position)
	{
		position[0] = cameraPos[0];
		position[1] = cameraPos[1];
		position[2] = cameraPos[2];
	}
	
	/** @param left destination vector to store left vector */
	public void getLeft(double[] left)
	{
		left[0] = -cameraMat.m[0][2];
		left[1] = -cameraMat.m[1][2];
		left[2] = -cameraMat.m[2][2];
	}
	/** @param up destination vector to store up vector */
	public void getUp(double[] up)
	{
		up[0] = cameraMat.m[0][1];
		up[1] = cameraMat.m[1][1];
		up[2] = cameraMat.m[2][1];
	}
	/** @param forward destination vector to store forward vector */
	public void getForward(double[] forward)
	{
		forward[0] = -cameraMat.m[0][2];
		forward[1] = -cameraMat.m[1][2];
		forward[2] = -cameraMat.m[2][2];
	}

	/** @return the position in the view coordinate system */
	public String toString()
	{
		return String.format("lon:%f, lat:%f, alt:%f, dir:%f, tilt:%f, x:%f, y:%f, z:%f",
				(float)latLonAlt.lon, (float)latLonAlt.lat, (float)latLonAlt.alt, (float)direction, tilt,
				(float)cameraPos[0], (float)cameraPos[1], (float)cameraPos[2]);
	}
}