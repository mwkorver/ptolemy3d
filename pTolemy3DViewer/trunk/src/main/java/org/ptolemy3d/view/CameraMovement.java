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

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import java.awt.event.MouseEvent;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.math.Quaternion4d;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;

/**
 * <H1>Camera Movements from Inputs (Mouse/Keyboard)</H1>
 * <BR>
 * Default mouse and keyboard actions are bind to move in the coordinate system describe in the <a href="Camera.html">Camera documentation</a>.<BR>
 * Default binding is describe on the <a href="http://ptolemy3d.org/wiki/PtolemyViewerControls">Ptolemy3D wiki</a>.<BR>
 * <BR>
 * <B><U>FIXME EXPOSE THE INTERFACE FOR USER/CUSTOM KEY/MOUSE BINDING.</B></U><BR>
 * <BR>
 * <BR>
 * <H1>Camera Movements from Automatic Movement (Trajectory)</H1>
 * <BR>
 * Camera movement can also be controlled automatically with trajectory or a location.<BR>
 * Destination location is expressed on the coordinate system describe in the <a href="Camera.html">Camera documentation</a>.<BR>
 * <BR>
 * For that, the camera movement controller provide few utilities :<BR>
 * <ul>
 * <li><u>flyTo</a>:</u> Go to the desirate position throught a trajectory that start from the actual position to the final position.<BR>
 * The movement on the trajectory can be controlled with ???.</li>
 * <li><u>flyToPosition</a>:</u> Go to the desirate position throught a trajectory that start from the actual position to the final position.<BR>
 * The movement on the trajectory can be controlled with a speed or a time parameter.</li>
 * <li><u>setOrientation</a>:</u> Go instantly to the desirate position.</li>
 * </ul>
 * <BR>
 * @see Camera
 */
public class CameraMovement
{
	private final static short UPDATE_INC = 50;
	public final static double PITCH = -Math3D.degToRad * 30;

	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;
	/** Inputs */
	public InputListener inputs;

	/** Tells either or not the view is moving */
	public boolean isActive = false;
	/** Maximum altitude */
	public int maxAlt = 250000;

	/** Flight mode */
	protected short fmode = 1;
	/* Some variable modified by inputs.inputState */
	protected double mouse_lr_rot_velocity = 0, mouse_ud_rot_velocity = 0;
	protected double ud_rot_velocity = 0,  lr_rot_velocity = 0;

	/** Result of picking */
	protected double[] intersectPoint = new double[3];

	private Matrix9d rotMat = new Matrix9d();
	private Matrix9d cloneMat = new Matrix9d();    // these are used by outside events
	private Matrix9d evtclnMat = new Matrix9d();
	private Quaternion4d quat = null;

	private final double scaler = 0.001;

	private double[] a_vec = new double[3]; // this is used by the rendering loop
	private double[] a = new double[3];  // used for autopilot operations
	private double[] endpoint = new double[3];
	private final double[] out = { 0, 0, 1 };
	private final double[] dispout = { 0, 0, 1 };
	private double mmx = 8000.0f,  default_mmx = mmx;
	private int Accel = 160;
	private int accel = 160;
	private double rotAng = 0.03;
	protected double rot_accel = (float) (rotAng / 100);
	protected double ground_ht = 0;
	private double default_rot_accel = rot_accel;
	private double rot_vel_scaler = 1;
	private int velocity = 0;
	private int vert_velocity = 0;
	private int horz_velocity = 0;

	private boolean isPan;
	private short updatecounter = UPDATE_INC;
	private double[][] pickray = new double[2][3];
	private String[] jsArgs = new String[6];
	private boolean followDemOn = false;
	private double relspdmult = 1;

	private double desiredTilt;
	private double desiredTiltIncrement;

	public CameraMovement(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
		this.inputs = new InputListener(ptolemy);

		this.desiredTilt = -1;
		this.desiredTiltIncrement = 1;
	}

	public void init()
	{
		final Landscape landscape = ptolemy.scene.landscape;

		fmode = 0;//1;
		accel = (int) mmx;
		rot_accel = rotAng;

		//end 2 feb 06 changes
		maxAlt = 750000;
		landscape.farClip = maxAlt + (EARTH_RADIUS * 2);

		stopMovement();
	}

	protected final void setCamera(Matrix16d modelView) throws Exception
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		double vpPos_1_bak = camera.latLonAlt.alt, tilt_bak = camera.tilt;

		Matrix9d vpMat_bak = new Matrix9d();
		camera.vpMat.copyTo(vpMat_bak);

		isActive = (velocity != 0) || (vert_velocity != 0) ||
				   (ud_rot_velocity != 0) || (lr_rot_velocity != 0) || (horz_velocity != 0);

		if (inAutoPilot > 0)
		{
			switch (inAutoPilot)
			{
				case 1: moveAutopilot(); break;
				case 2: movePanorama(); break;
			}
		}
		else
		{

			int vh_accel = accel;
			double vh_mmx = mmx;

			if (fmode == 0)
			{
				// alter velocity and accel according to current altitude.
				mmx = accel = (int) ((2 * (Math.tan(0.5235987756) * (camera.latLonAlt.alt + 1))) * 25 * relspdmult);
				horz_velocity = vert_velocity = velocity = 0;
				lr_rot_velocity = mouse_lr_rot_velocity;
				ud_rot_velocity = mouse_ud_rot_velocity;
				mouse_lr_rot_velocity = mouse_ud_rot_velocity = 0;
				vh_accel = accel;
				vh_mmx = mmx;
			}
			else
			{
				vh_accel = 160;
				vh_mmx = 8000;

				if ((inputs.inputState.getStraightForward() <= 0) && (inputs.inputState.getStraightBack() <= 0))
				{
					if (velocity > 0) {
						velocity -= accel;
					}
					else if (velocity < 0) {
						velocity += accel;
					}
				}
				if ((inputs.inputState.getStrafeLeft() <= 0) && (inputs.inputState.getStrafeRight() <= 0))
				{
					if (vert_velocity > 0) {
						vert_velocity -= vh_accel;
					}
					else if (vert_velocity < 0) {
						vert_velocity += vh_accel;
					}
				}
				if ((inputs.inputState.getIncreaseAltitude() <= 0) && (inputs.inputState.getDecreaseAltitude() <= 0))
				{
					if (horz_velocity > 0) {
						horz_velocity -= vh_accel;
					}
					else if (horz_velocity < 0) {
						horz_velocity += vh_accel;
					}
				}
				if ((inputs.inputState.getTurnLeft() <= 0) && (inputs.inputState.getTurnRight() <= 0))
				{
					if (lr_rot_velocity > 0) {
						lr_rot_velocity -= rot_accel;
					}
					else if (lr_rot_velocity < 0) {
						lr_rot_velocity += rot_accel;
					}
					if (Math.abs(lr_rot_velocity) <= rot_accel) {
						lr_rot_velocity = 0;
					}
				}
				if ((inputs.inputState.getTiltUp() <= 0) && (inputs.inputState.getTiltDown() <= 0))
				{
					if (ud_rot_velocity > 0) {
						ud_rot_velocity -= rot_accel;
					}
					else if (ud_rot_velocity < 0) {
						ud_rot_velocity += rot_accel;
					}
					if (Math.abs(ud_rot_velocity) <= rot_accel) {
						ud_rot_velocity = 0;
					}
				}
			}

			a_vec[0] = a_vec[1] = a_vec[2] = 0.0f;

			/* Acceleration due to keys being down */
			if (((inputs.inputState.getStraightForward() | inputs.inputState.getStraightForwardConstAlt())) > 0 &&
				((inputs.inputState.getStraightBack() | inputs.inputState.getStraightBackConstAlt())) <= 0 &&
				(-velocity < mmx))
			{
				inputs.inputState.decStraightForward();
				inputs.inputState.decStraightForwardConstAlt();
				velocity -= (velocity > 0) ? accel + accel : accel;
				isPan = ((inputs.inputState.getStraightForwardConstAlt()) > 0) ? false : true;
			}
			else if(((inputs.inputState.getStraightForward() | inputs.inputState.getStraightForwardConstAlt())) <= 0 &&
					((inputs.inputState.getStraightBack() | inputs.inputState.getStraightBackConstAlt())) > 0 &&
					(velocity < mmx))
			{
				inputs.inputState.decStraightBack();
				inputs.inputState.decStraightBackConstAlt();
				velocity += (velocity < 0) ? accel + accel : accel;
				isPan = ((inputs.inputState.getStraightBackConstAlt()) > 0) ? false : true;
			}
			
			if ((inputs.inputState.getStrafeLeft() > 0) && (inputs.inputState.getStrafeRight() <= 0) && (-vert_velocity < vh_mmx))
			{
				inputs.inputState.decStrafeLeft();
				vert_velocity -= (vert_velocity > 0) ? (vh_accel + vh_accel) : vh_accel;
			}
			else if ((inputs.inputState.getStrafeLeft() <= 0) && (inputs.inputState.getStrafeRight() > 0) && (vert_velocity < vh_mmx))
			{
				inputs.inputState.decStrafeRight();
				vert_velocity += (vert_velocity < 0) ? vh_accel + vh_accel : vh_accel;
			}
			if ((inputs.inputState.getDecreaseAltitude() > 0) && (inputs.inputState.getIncreaseAltitude() <= 0) && (-horz_velocity < vh_mmx))
			{
				inputs.inputState.decDecreaseAltitude();
				horz_velocity -= (horz_velocity > 0) ? vh_accel + vh_accel : vh_accel;
			}
			else if ((inputs.inputState.getDecreaseAltitude() <= 0) && (inputs.inputState.getIncreaseAltitude() > 0) && (horz_velocity < vh_mmx))
			{
				inputs.inputState.decIncreaseAltitude();
				horz_velocity += (horz_velocity < 0) ? vh_accel + vh_accel : vh_accel;
			}
			if ((inputs.inputState.getDecreaseAltitude() > 0) || (inputs.inputState.getIncreaseAltitude() > 0))
			{
				inputs.inputState.decDecreaseAltitude();
				desiredTilt = -1;//cancel setPitchDegrees order
			}
			a_vec[2] = velocity * scaler;
			a_vec[0] = vert_velocity * scaler;

			/* TURN LEFT AND RIGHT */
			if ((inputs.inputState.getTurnLeft()) > 0 && (inputs.inputState.getTurnRight()) <= 0 && (-lr_rot_velocity < rotAng))
			{
				inputs.inputState.decTurnLeft();
				lr_rot_velocity -= (lr_rot_velocity > 0) ? rot_accel + rot_accel : rot_accel;//  *  ((JS.unit.UNITS <= 1) ? -1 : 1);
			}
			else if ((inputs.inputState.getTurnLeft()) <= 0 && (inputs.inputState.getTurnRight()) > 0 && (lr_rot_velocity < rotAng))
			{
				inputs.inputState.decTurnRight();
				lr_rot_velocity += (lr_rot_velocity < 0) ? rot_accel + rot_accel : rot_accel;// *  ((JSUNITS <= 1) ? -1 : 1);

				/*   YAW and PITCH      */
			}
			if ((inputs.inputState.getTiltDown()) > 0 && (inputs.inputState.getTiltUp()) <= 0 && (-ud_rot_velocity < rotAng))
			{
				inputs.inputState.decTiltDown();
				ud_rot_velocity -= (ud_rot_velocity > 0) ? rot_accel + rot_accel : rot_accel;
			}
			else if ((inputs.inputState.getTiltDown()) <= 0 && (inputs.inputState.getTiltUp()) > 0 && (ud_rot_velocity < rotAng))
			{
				inputs.inputState.decTiltUp();
				ud_rot_velocity += (ud_rot_velocity < 0) ? rot_accel + rot_accel : rot_accel;
			}

			/* Addition to existing orientation */
//			if (((lr_rot_velocity != 0.0) || (ud_rot_velocity != 0.0) || (vert_velocity != 0)) && (false))
//			{
//				camera.vpMat.invert(cloneMat);
//				if (lr_rot_velocity != 0.0)
//				{
//					direction -= lr_rot_velocity;
//					if (direction < 0) {
//						direction += Math3D.twoPi;
//					}
//					if (direction > (Math3D.twoPi)) {
//						direction -= Math3D.twoPi;
//					}
//					rotMat.rotY(lr_rot_velocity);
//					camera.vpMat.copyTo(cloneMat);
//					Matrix3x3d.multiply(cloneMat, rotMat, camera.vpMat);
//				}
//				if (((pitch - ud_rot_velocity) > -((Math3D.halfPI) - 0.005f)) && ((pitch - ud_rot_velocity) < (Math3D.halfPI)))
//				{
//					pitch -= ud_rot_velocity;
//					if (ud_rot_velocity != 0.0) {
//						rotMat.rotX(ud_rot_velocity);
//						camera.vpMat.copyTo(cloneMat);
//						Matrix3x3d.multiply(rotMat, cloneMat, camera.vpMat);
//					}
//				}
//				else
//				{
//					ud_rot_velocity = 0;
//					/* Rotation of distance vector */
//				}
//				camera.vpMat.invert(cloneMat);
//			}

			if (isPan) {
				rotMat.rotY(camera.direction);
				rotMat.transform(a_vec);
			}
			else {
				camera.vpMat.transform(a_vec);
			}
			a_vec[1] += (horz_velocity * scaler);

			{
				camera.latLonAlt.alt += velocity * scaler; // distance away from surface...

				double min_rot = 0.000002;

				if (camera.latLonAlt.alt < 1) {
					camera.latLonAlt.alt = 1;
				}
				if (camera.latLonAlt.alt > maxAlt) {
					camera.latLonAlt.alt = maxAlt;
				}
				double rot_scaler = 0.000005;
				double rot_y = (lr_rot_velocity * rot_vel_scaler / 10) * Math3D.degToRad;
				double rot_x = -(ud_rot_velocity * rot_vel_scaler / 10) * Math3D.degToRad;

				if (fmode == 0)
				{
					final double GL_WIDTH = ptolemy.events.drawWidth;
					final double GL_HEIGHT = ptolemy.events.drawHeight;

					// this is to cancel out the relative acceleration.
					horz_velocity /= (2 * (0.57735026919189393373967470078005 * camera.latLonAlt.alt));
					vert_velocity /= (2 * (0.57735026919189393373967470078005 * camera.latLonAlt.alt));
					rot_y = (((0.57735026919189393373967470078005 * camera.latLonAlt.alt) / GL_WIDTH) * lr_rot_velocity / 10) * Math.PI / 180 * relspdmult;
					if ((Math.abs(rot_y) < min_rot) && (rot_y != 0))
					{
						rot_y = min_rot * ((rot_y > 0) ? 1 : -1);
					}
					rot_x = -(((0.57735026919189393373967470078005 * camera.latLonAlt.alt) / GL_HEIGHT) * ud_rot_velocity / 10) * Math.PI / 180 * relspdmult;
					if ((Math.abs(rot_x) < min_rot) && (rot_x != 0))
					{
						rot_x = min_rot * ((rot_x > 0) ? 1 : -1);
					}
					rot_scaler = 0.001;
				}

				if (desiredTilt == -1)
				{
					camera.tilt += horz_velocity * rot_scaler;
				}
				else
				{
					double tiltDiff = desiredTilt - camera.tilt;
					if (Math.abs(tiltDiff) < desiredTiltIncrement) {
						camera.tilt = desiredTilt;
					}
					else {
						camera.tilt += (desiredTilt > camera.tilt ? desiredTiltIncrement : -desiredTiltIncrement);
					}
				}

				if (camera.tilt > 0) {
					camera.tilt = 0;
				}
				else if (camera.tilt < -Math3D.halfPI) {
					camera.tilt = -Math3D.halfPI;
				}
				rotMat.rotY(rot_y);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

				rotMat.rotX(rot_x);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

				rotMat.rotZ(vert_velocity * rot_scaler);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
			}
		} // end of !autopilot condition

		ground_ht = landscape.groundHeight(camera.latLonAlt.lon, camera.latLonAlt.lat, 0);
		setCameraMatrix(followDemOn);

		{
			if (!checkAlt() && (inAutoPilot == 0))
			{
				vpMat_bak.copyTo(camera.vpMat);
				camera.tilt = tilt_bak;
				if (vpPos_1_bak > camera.latLonAlt.alt) // always allow to zoom out
				{
					camera.latLonAlt.alt = vpPos_1_bak;
				}
				setCameraMatrix(followDemOn); //reset the camera matrix
			}

			modelView.identityMatrix();
			modelView.translate(0, 0, -camera.latLonAlt.alt);
			modelView.rotateX(camera.tilt);
			modelView.translate(0, 0, -EARTH_RADIUS - ((followDemOn) ? ground_ht : 0));
			modelView.multiply(camera.vpMat);

			//Old code
//			gl.glLoadIdentity();
//			gl.glTranslated(0, 0, -camera.latLonAlt.alt);
//			gl.glRotated(camera.tilt * JetMath3d.radToDegConst, 1.0f, 0.0f, 0);
//			gl.glTranslated(0, 0, -EARTH_RADIUS - ((followDemOn) ? ground_ht : 0));
//			gl.glMultMatrixd(Matrix16d.from(camera.vpMat).m, 0);

			/***  set coordinate information using current matrices ****/
			camera.latLonAlt.lat = Math.asin(camera.vpMat.m[1][2]) * Math3D.radToDeg;
			camera.latLonAlt.lon = Math3D.angle2dvec(-1, 0, camera.vpMat.m[2][2], camera.vpMat.m[0][2], true);
			if (camera.vpMat.m[0][2] >= 0)
			{
				camera.latLonAlt.lon = -camera.latLonAlt.lon;
				// set camera.direction, use yup angle of current matrix with yup of a north facing one.
				// north facing y up vector
			}
			double yup_x = (Math.sin((Math3D.degToRad * camera.latLonAlt.lon)) * Math.sin((camera.latLonAlt.lat * Math3D.degToRad)));
			double yup_y = Math.cos((Math3D.degToRad * camera.latLonAlt.lat));
			double yup_z = (Math.cos((Math3D.degToRad * camera.latLonAlt.lon)) * Math.sin((camera.latLonAlt.lat * Math3D.degToRad)));

			camera.latLonAlt.lon *= unit.DD;
			camera.latLonAlt.lat *= unit.DD;

			camera.direction = Math3D.angle3dvec(yup_x, yup_y, yup_z, camera.vpMat.m[0][1], camera.vpMat.m[1][1], camera.vpMat.m[2][1], false);
			if ((camera.latLonAlt.lon > (90 * unit.DD)) || (camera.latLonAlt.lon < (-90 * unit.DD)))
			{
				if (((yup_x * camera.vpMat.m[1][1]) - (yup_y * camera.vpMat.m[0][1])) < 0) {	//rotate left
					camera.direction = Math3D.twoPi - camera.direction;
				}
			}
			else
			{
				if (((yup_x * camera.vpMat.m[1][1]) - (yup_y * camera.vpMat.m[0][1])) > 0) {	//rotate left
					camera.direction = Math3D.twoPi - camera.direction;
				}
			}
		}

		// check for any bad numbers, if any unacceptable values set back to 0.
		{
			if (Double.isNaN(camera.latLonAlt.lon)) {
				camera.latLonAlt.lon = 0;
			}
			if (Double.isNaN(camera.latLonAlt.alt)) {
				camera.latLonAlt.alt = 5000;
			}
			if (Double.isNaN(camera.latLonAlt.lat)) {
				camera.latLonAlt.lat = 0;
			}
			if (Double.isNaN(camera.direction)) {
				camera.direction = 0;
			}
			if (Double.isNaN(camera.tilt)) {
				camera.tilt = 0;
			}
		}

		updatePosition(false);
	}

	private final void setCameraMatrix(boolean addGroundHeight)
	{
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		double groundH = addGroundHeight ? ground_ht : 0;

		// set cameraPos
		rotMat.rotX(-camera.tilt);
		Matrix9d.multiply(camera.vpMat, rotMat, camera.cameraMat);

		camera.cameraPos[0] = (camera.vpMat.m[0][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[0][2] * camera.latLonAlt.alt);
		camera.cameraPos[1] = (camera.vpMat.m[1][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[1][2] * camera.latLonAlt.alt);
		camera.cameraPos[2] = (camera.vpMat.m[2][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[2][2] * camera.latLonAlt.alt);

		camera.vertAltDD = Math.sqrt((camera.cameraPos[0] * camera.cameraPos[0]) + (camera.cameraPos[1] * camera.cameraPos[1]) + (camera.cameraPos[2] * camera.cameraPos[2])) - EARTH_RADIUS;
		camera.vertAlt = camera.vertAltDD / unit.coordSystemRatio;

		camera.cameraMatInv.copyFrom(camera.cameraMat);
		camera.cameraMatInv.invert3x3();
	}

	private final boolean checkAlt()
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		{
			double mag = camera.vertAltDD + EARTH_RADIUS;
			camera.cameraY = Math.asin(camera.cameraPos[1] / mag) * Math3D.radToDeg * unit.DD;
			camera.cameraX = Math3D.angle2dvec(-1, 0, camera.cameraPos[2] / mag, camera.cameraPos[0] / mag, true) * unit.DD;
			if ((camera.cameraPos[0] / mag) >= 0) {
				camera.cameraX *= -1;
			}
			double calt = landscape.groundHeight(camera.cameraX, camera.cameraY, 0);
			if ((mag - EARTH_RADIUS) < (calt + 25 * unit.coordSystemRatio)) {
				return false;
			}
		}
		return true;
	}

	protected synchronized final void updatePosition(boolean force)
	{
		final Camera camera = ptolemy.camera;

		if (inputs.inputState.hasInput())
		{
			force = false; // if user is still moving around no need to force.
		}
		if (!inputs.inputState.hasInput() && (inAutoPilot == 0) && (!force) && (updatecounter != (UPDATE_INC + 1)))
		{
			return; // user is not moving, no need to call an update.
		}
		updatecounter++;
		if ((updatecounter > UPDATE_INC) || (force))
		{
			if (ptolemy.javascript != null  && ptolemy.javascript.getJSObject() != null)
			{
				try
				{
					jsArgs[0] = String.valueOf(camera.cameraX);
					jsArgs[1] = String.valueOf(camera.cameraY);
					jsArgs[2] = String.valueOf(camera.direction * Math3D.radToDeg);
					jsArgs[3] = String.valueOf(camera.getVerticalAltitudeMeters());
					jsArgs[4] = String.valueOf(camera.latLonAlt.getLongitudeDD());
					jsArgs[5] = String.valueOf(camera.latLonAlt.getLatitudeDD());
					ptolemy.javascript.getJSObject().call("updatePosition", jsArgs);
				}
				catch (Exception e)
				{
				}
			}
			updatecounter = 0;
		}
	}

	private final void displayCoords()
	{
		Math3D.setMapCoord(intersectPoint, dispout);
		ptolemy.callJavascript("plotPosition", String.valueOf(dispout[0]), String.valueOf(dispout[1]), String.valueOf(dispout[2]));
	}

	public final void setOrientation(LatLonAlt latLonAlt, double dir, double pit)
	{
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		double lon = latLonAlt.lon;
		double alt = latLonAlt.alt;
		double lat = latLonAlt.lat;
		
		stopMovement();
		inAutoPilot = 0;

		try {
			camera.direction = dir * Math3D.degToRad;

			camera.vpMat.identity();
			rotMat.rotY(((lon / unit.DD) + 180) * Math3D.degToRad);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			rotMat.rotX((-lat / unit.DD) * Math3D.degToRad);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			rotMat.rotZ(camera.direction);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			camera.tilt = pit * Math3D.degToRad;

			camera.latLonAlt.lon = lon;
			camera.latLonAlt.lat = lat;
			camera.latLonAlt.alt = alt * unit.coordSystemRatio;
		} catch (NumberFormatException e){}

		updatecounter = UPDATE_INC + 1;

		if (ptolemy.tileLoader != null) {
			if (ptolemy.tileLoader.isSleeping) {
				ptolemy.tileLoaderThread.interrupt();
			}
		}
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setVelocity(double nv)
	{
		if ((nv % accel) == 0) {
			if ((nv >= 100) && (nv <= 640000)) {
				mmx = default_mmx = nv;
			}
		}
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setAccelleration(int na)
	{
		if ((mmx % na) == 0) {
			accel = na;
			Accel = na;
		}
	}

	/**
	 * Switch between two fly movement behavior. Key behavior and acceleration is dependsant of the mode choosed.
	 * Default value is 0.
	 */
	public void setRealisticFlight(int v)
	{
		if (v == 0) {
			fmode = (short) 0;
			accel = (int) mmx;
			rot_accel = rotAng;
		}
		else {
			fmode = (short) 1;
			accel = Accel;
			mmx = default_mmx;
			rot_accel = default_rot_accel;
		}
	}
	public int getRealisticFlight()
	{
		return fmode;
	}

	/**
	 * View rotation scaler.<BR>
	 * Default value is 1.
	 */
	public void setAngularSpeed(double v)
	{
		rot_vel_scaler = v / rotAng;
	}
	/** @return the rotation scaler. */
	public double getAngularSpeed()
	{
		return rot_vel_scaler * rotAng;
	}

	public void setPitchDegrees(double newPitchDegrees, double newPitchDegreesIncrement)
	{
		desiredTilt = newPitchDegrees * Math3D.degToRad;
		if (desiredTilt > 0) {
			desiredTilt = 0;
		}
		if (desiredTilt < -Math3D.halfPI) {
			desiredTilt = -Math3D.halfPI;
		}

		desiredTiltIncrement = newPitchDegreesIncrement;
		if (desiredTiltIncrement < 1) {
			desiredTiltIncrement = 1;
		}
		else if (desiredTiltIncrement > 45) {
			desiredTiltIncrement = 45;
		}
		desiredTiltIncrement *= Math3D.degToRad;
	}

	public final void stopMovement()
	{
		inputs.inputState.reset();
		velocity = 0;
		vert_velocity = 0;
		ud_rot_velocity = 0;
		lr_rot_velocity = 0;
		horz_velocity = 0;
	}

	/************************** AUTOPILOT ********************************/
	private  double angle;
	private  boolean isRight = true,  isUp = true;
	private  double setang;
	private  double uangle = 0.0;
	private  double halfway;
	public int inAutoPilot = 0;
	private  double cruise_alt;
	private  double max_rate,  current_speed,  dist_trav,  brake_dist,  current_lr_rot,  rot_lr_trav,  rot_lr_brake,  current_ud_rot,  rot_ud_trav,  rot_ud_brake;
	private  double ud_accel;
	private  double total_distance;
	private  double auto_x = 0,  auto_y = 0,  auto_z = 0;
	private  double start_y,  stopangle;

	private final void initAutopilot()
	{
		if (ptolemy.tileLoader.isSleeping) {
			ptolemy.tileLoaderThread.interrupt();
		}
		setFollowDem(false);
		current_speed = dist_trav = brake_dist = current_lr_rot = rot_lr_trav = rot_lr_brake = current_ud_rot = rot_ud_trav = rot_ud_brake = 0.0;
		stopMovement();
	}

	public final boolean getFollowDem()
	{
		return followDemOn;
	}
	public final void setFollowDem(boolean v)
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Camera camera = ptolemy.camera;

		boolean prev = followDemOn;
		followDemOn = v;

		if (prev && !followDemOn)
		{
			double ht = landscape.groundHeight(camera.latLonAlt.lon, camera.latLonAlt.lat, 0);

			camera.latLonAlt.alt += (ht / Math.cos(-camera.tilt));

			rotMat.rotX(-Math.atan((ht * Math.tan(-camera.tilt)) / EARTH_RADIUS));
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		}
		else if (!prev && followDemOn)
		{
			double ht = landscape.groundHeight(camera.latLonAlt.lon, camera.latLonAlt.lat, 0);

			camera.latLonAlt.alt -= (ht / Math.cos(-camera.tilt));

			rotMat.rotX(Math.atan((ht * Math.tan(-camera.tilt)) / EARTH_RADIUS));
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		}
	}

	/**
	 * Time flight
	 */
	public final void flyToPositionTime(LatLonAlt latLonAlt, double s_direction, double s_tilt, double flight_arc, double cruise_tilt, double time)
	{
		flyToPositionDD(latLonAlt, s_direction, s_tilt, flight_arc, cruise_tilt);
		max_rate = 0.2;
		ud_accel = angle / (time * 1000);

		inAutoPilot = 1;
	}

	/**
	 * Speed flight
	 */
	public final void flyToPositionSpeed(LatLonAlt latLonAlt, double s_direction, double s_tilt, double flight_arc, double cruise_tilt, double speed)
	{
		flyToPositionDD(latLonAlt, s_direction, s_tilt, flight_arc, cruise_tilt);
		// 1 km is approx 2.0 * Math.atan(/EARTH_RADIUS)
		max_rate = (2.0 * Math.atan(500.0 / EARTH_RADIUS) * speed * (0.05)) * scaler;
		ud_accel = max_rate / 20;

		inAutoPilot = 1;
	}
	/* flyTo Implementation */
	private final void flyToPositionDD(LatLonAlt latLonAlt, double s_direction, double s_tilt, double flight_arc, double cruise_tilt)
	{
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		double lon = latLonAlt.lon;
		double alt = latLonAlt.alt;
		double lat = latLonAlt.lat;
		
		initAutopilot();
		start_y = camera.latLonAlt.alt;

		if (Math3D.distance2D(camera.latLonAlt.lon, lon, camera.latLonAlt.lat, lat) < 10)
		{
			lon += 7;
			lat += 7;
		}

		auto_x = lon;
		auto_y = alt * unit.coordSystemRatio;
		auto_z = lat;

		if ((auto_x >= 180000000) || (auto_x <= -180000000) || (auto_z <= -90000000) || (auto_z >= 90000000))
		{
			inAutoPilot = 0;
			return;
		}

		Matrix9d tmpMat = new Matrix9d();

		stopangle = s_tilt * Math3D.degToRad;

		setCrossVert();

		if ((angle = Math3D.angle3dvec(0, 0, 1, endpoint[0], endpoint[1], endpoint[2], false)) == 0)
		{
			camera.latLonAlt.alt = auto_y;
		}
		if (quat == null)
		{
			quat = new Quaternion4d();        // predict y rotate
		}
		quat.set(angle, a);
		quat.setMatrix(rotMat);

		camera.vpMat.copyTo(tmpMat);
		tmpMat.copyTo(evtclnMat);
		Matrix9d.multiply(evtclnMat, rotMat, tmpMat);

		double[] yup = {0, 1, 0};
		double[] zout = {tmpMat.m[0][2], tmpMat.m[1][2], tmpMat.m[2][2]};

		Vector3d.cross(endpoint, yup, zout);
		Vector3d.normalize(endpoint);

		// find the angle
		uangle = Math3D.angle3dvec(endpoint[0], endpoint[1], endpoint[2], tmpMat.m[0][0], tmpMat.m[1][0], tmpMat.m[2][0], false);

		if (Double.isNaN(uangle) || Double.isNaN(angle))
		{
			inAutoPilot = 0;
			return;
		}

		if (tmpMat.m[1][0] < 0)
		{
			uangle = Math3D.twoPi - uangle;
		}
		s_direction *= Math3D.degToRad;

		// set camera.direction to look
		if (uangle < s_direction)
		{
			uangle = s_direction - uangle;
			isRight = true;
		}
		else if (uangle > s_direction)
		{
			uangle = uangle - s_direction;
			isRight = false;
		}
		else
		{
			uangle = 0;
		}
		// fix to see if its closer to go the other way.
		if (uangle > Math.PI)
		{
			uangle = Math3D.twoPi - uangle;
			isRight = (isRight) ? false : true;
		}

		total_distance = Math.abs(camera.tilt - stopangle);

		isUp = (camera.tilt < stopangle) ? true : false;

		halfway = angle / 2.0;

		if (flight_arc > 50)
		{
			flight_arc = 50;
		}
		if (flight_arc < 1)
		{
			flight_arc = 1;
		}
		cruise_alt = (((EARTH_RADIUS * angle) * Math.atan(flight_arc * Math3D.degToRad)) + auto_y);
		if (cruise_alt > 7000000)
		{
			cruise_alt = 7000000;
		}
		//stopdirection = 0;
	}

	private final void moveAutopilot()
	{
		if ((rot_lr_trav == angle) && (rot_ud_trav == uangle) && (dist_trav == total_distance)) {
			stopAuto();
			return;
		}

		final Camera camera = ptolemy.camera;
		// this block controls spin of the earth to our coordinates
		if (rot_lr_trav != angle)
		{
			boolean stop = false;
			double adjust = 0;
			if ((current_lr_rot < rotAng) && (rot_lr_trav < halfway))
			{ // accel
				current_lr_rot += ud_accel;
				rot_lr_brake += current_lr_rot;
			}
			else
			{ // decel
				if ((angle - rot_lr_trav) <= rot_lr_brake)
				{
					current_lr_rot -= ud_accel;
				}
				else if ((angle - rot_lr_trav - current_lr_rot) <= rot_lr_brake)
				{
					adjust = ((angle - rot_lr_trav) - rot_lr_brake) - current_lr_rot + 0.000000001;
				}
			}

			if (current_lr_rot > 0)
			{
				if ((rot_lr_trav + current_lr_rot + adjust) > angle)
				{
					current_lr_rot = angle - rot_lr_trav;
					adjust = 0;
					stop = true;
				}
			}
			else
			{
				current_lr_rot = angle - rot_lr_trav;
				adjust = 0;
				stop = true;
			}

			if (stop)
			{
				rot_lr_trav = angle;
			}
			else
			{
				rot_lr_trav += adjust + current_lr_rot;
			}
			quat.set(current_lr_rot + adjust, a);
			quat.setMatrix(rotMat);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			// control our y pos
			if (rot_lr_trav < halfway) {
				camera.latLonAlt.alt = ((Math.sin(((Math.PI * rot_lr_trav) / angle)) * (cruise_alt - start_y)) + start_y);
			}
			else {
				camera.latLonAlt.alt = ((Math.sin(((Math.PI * rot_lr_trav) / angle)) * (cruise_alt - auto_y)) + auto_y);
			}
			if (camera.latLonAlt.alt > maxAlt) {
				camera.latLonAlt.alt = maxAlt;
			}
		}

		// this block controls our z axis rotation, rotation to a north facing position
		if (rot_ud_trav != uangle)
		{
			if ((current_ud_rot < rotAng) && (rot_ud_trav < (uangle / 2)))
			{
				current_ud_rot += default_rot_accel;
				rot_ud_brake += current_ud_rot;
			}
			else if ((uangle - rot_ud_trav) <= rot_ud_brake)
			{
				current_ud_rot -= default_rot_accel;
			}
			if ((rot_ud_trav + current_ud_rot < uangle) && (current_ud_rot > 0))
			{
				rot_ud_trav += current_ud_rot;
			}
			else
			{
				current_ud_rot = uangle - rot_ud_trav;
				rot_ud_trav = uangle;
			}

			rotMat.rotZ(((isRight) ? current_ud_rot : -current_ud_rot));
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
			// reset twist axis
			setCrossVert();
		}

		// and this is camera.tilt.
		if (camera.tilt != stopangle)
		{
			if ((current_speed < rotAng) && (dist_trav < (total_distance / 2)))
			{
				current_speed += default_rot_accel;
				brake_dist += current_speed;
			}
			else if ((total_distance - dist_trav) <= brake_dist)
			{
				current_speed -= default_rot_accel;
			}
			if ((dist_trav + current_speed < total_distance) && (current_speed > 0))
			{
				dist_trav += current_speed;
			}
			else
			{
				current_speed = total_distance - dist_trav;
				dist_trav = total_distance;
				camera.tilt = stopangle;
			}
			camera.tilt += (current_speed * ((isUp) ? 1 : -1));
		}
	}

	/**
	 * @param type 0=LOOP, 1=LINE
	 */
	public final void flyTo(LatLonAlt latLonAlt,
			int speed, int ld_angle, int fly_angle, int type, int arc_angle)
	{
		final Camera camera = ptolemy.camera;

		initAutopilot();

		start_y = camera.latLonAlt.alt;

		flyToDD(/*lon, alt, lat*/latLonAlt, speed, ld_angle, fly_angle, type, arc_angle);

		inAutoPilot = 1;
	}
	/* flyTo implementation */
	private final void flyToDD(LatLonAlt latLonAlt, int speed, int ld_angle, int fly_angle, int type, int arc_angle)
	{
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		double lon = latLonAlt.lon;
		double alt = latLonAlt.alt;
		double lat = latLonAlt.lat;
		
		if (Math3D.distance2D(camera.latLonAlt.lon, lon, camera.latLonAlt.lat, lat) < 10) {
			lon += 7;
			lat += 7;
		}

		auto_x = lon;
		auto_y = alt * unit.coordSystemRatio;
		auto_z = lat;

		if ((auto_x >= 180000000) || (auto_x <= -180000000) || (auto_z <= -90000000) || (auto_z >= 90000000))
		{
			inAutoPilot = 0;
			return;
		}

		Matrix9d tmpMat = new Matrix9d();

		setCrossVert();

		if ((angle = Math3D.angle3dvec(0, 0, 1, endpoint[0], endpoint[1], endpoint[2], false)) == 0) {
			camera.latLonAlt.alt = auto_y;
		}
		if (quat == null) {
			quat = new Quaternion4d();        // predict y rotate
		}
		quat.set(angle, a);
		quat.setMatrix(rotMat);

		camera.vpMat.copyTo(tmpMat);
		tmpMat.copyTo(evtclnMat);
		Matrix9d.multiply(evtclnMat, rotMat, tmpMat);

		double[] yup = { 0, 1, 0 };
		double[] zout = { tmpMat.m[0][2], tmpMat.m[1][2], tmpMat.m[2][2] };

		Vector3d.cross(endpoint, yup, zout);
		Vector3d.normalize(endpoint);

		uangle = Math3D.angle3dvec(endpoint[0], endpoint[1], endpoint[2], tmpMat.m[0][0], tmpMat.m[1][0], tmpMat.m[2][0], false);
		if (Double.isNaN(uangle) || Double.isNaN(angle)) {
			inAutoPilot = 0;
			return;
		}
		isRight = true;

		if (tmpMat.m[1][0] > 0) {
			isRight = false;
		}
		stopangle = ld_angle * Math3D.degToRad;
		total_distance = Math.abs(camera.tilt - stopangle);
		isUp = (camera.tilt < stopangle) ? true : false;

		ud_accel = angle / 10000;
		max_rate = 0.2;

		halfway = angle / 2.0;
		cruise_alt = auto_y + ((type == 0) ? (EARTH_RADIUS * angle) * (Math.atan(arc_angle * Math3D.degToRad)) : 0);
		if (cruise_alt > 7000000) {
			cruise_alt = 7000000;
		}
		//stopdirection = 0;
	}

	private final void stopAuto()
	{
		updatePosition(true);
		inAutoPilot = 0;
		try {
			ptolemy.javascript.getJSObject().call("onAutoPilotStop", null);
		} catch (Exception e) {}
	}

	private final void setCrossVert()
	{
		final Camera camera = ptolemy.camera;

		Matrix9d pointerMat = new Matrix9d();

		Math3D.setSphericalCoord(auto_x, auto_z, endpoint);

		camera.vpMat.copyTo(pointerMat);
		pointerMat.invert(evtclnMat);
		pointerMat.transform(endpoint);
		Vector3d.normalize(endpoint);

		Vector3d.cross(a, out, endpoint);
		Vector3d.normalize(a);
	}

	/******************************   functions for panorama ************************************/
	public void panorama(LatLonAlt latLonAlt, int rtimes, double rspeed, double rpitch)
	{
		final Camera camera = ptolemy.camera;
		initAutopilot();
		
		setang = rpitch;
		total_distance = latLonAlt.alt;
		rot_lr_trav = (rtimes == 0) ? Integer.MAX_VALUE : rtimes; // use a var from autopilot...

		current_lr_rot = rspeed * Math.abs(rspeed) * 0.001;

		if (rspeed > 0) {
			rot_lr_trav--;
		}
		setFollowDem(false);
		setOrientation(latLonAlt, 0, rpitch);

		{
			setCameraMatrix(false);
			halfway = (Math.sqrt((camera.cameraPos[0] * camera.cameraPos[0]) + (camera.cameraPos[1] * camera.cameraPos[1]) + (camera.cameraPos[2] * camera.cameraPos[2])) - EARTH_RADIUS);
			cruise_alt = Math.atan((halfway * Math.tan(setang * Math3D.degToRad)) / EARTH_RADIUS);
			// move forward to center camera over point.
			rotMat.rotX(cruise_alt);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		}
		inAutoPilot = 2;
	}

	private void movePanorama()
	{
		if (rot_lr_trav < 0) {
			stopAuto();
			return;
		}

		final Camera camera = ptolemy.camera;
		camera.direction += current_lr_rot;

		if (camera.direction < 0) {
			camera.direction += Math3D.twoPi;
			rot_lr_trav--;
		}
		if (camera.direction > (Math3D.twoPi)) {
			camera.direction -= Math3D.twoPi;
			rot_lr_trav--;
		}

		{
			// move back
			rotMat.rotX(-cruise_alt);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			rotMat.rotZ(current_lr_rot);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			// move forward
			rotMat.rotX(cruise_alt);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
		}
	}

	/*****************************************************************************************/

	protected final synchronized void outputCoordinates(MouseEvent e)
	{
		setRays(e);
		if (!ptolemy.scene.plugins.pick(intersectPoint, pickray))
		{
			if (ptolemy.scene.landscape.pick(intersectPoint, pickray))
			{
				if (!ptolemy.scene.plugins.onPick(intersectPoint))
				{
					displayCoords();
				}
			}
		}
	}

	protected final void zoomToSelected(MouseEvent e, boolean zoomin)
	{
		setRays(e);
		if (!ptolemy.scene.landscape.pick(intersectPoint, pickray)) {
			return;
		}

		final Ptolemy3DUnit unit = ptolemy.unit;
		final Landscape landscape = ptolemy.scene.landscape;
		final Camera camera = ptolemy.camera;

		if ((intersectPoint[0] != -999) || (intersectPoint[1] != -999) || (intersectPoint[2] != -999))
		{
			double tx, tz, ty;
			{
				double[] intvec = {intersectPoint[0], intersectPoint[1], intersectPoint[2]};
				Vector3d.normalize(intvec);
				double o_lon = Math3D.angle2dvec(-1, 0, intvec[2], intvec[0], true);
				if (intvec[0] >= 0)
				{
					o_lon = -o_lon;
				}
				tz = Math.asin(intvec[1]) * Math3D.radToDeg * unit.DD;
				tx = o_lon * unit.DD;
				if (Double.isNaN(tx) || Double.isNaN(tz))
				{
					return;
				}
				ty = camera.latLonAlt.alt / unit.coordSystemRatio;
				if (zoomin)
				{
					ty /= 4;
					double efloor = landscape.groundHeight(tx, tz, 0) / unit.coordSystemRatio + 50;
					// floor of 100 meters
					if (ty < efloor)
					{
						ty = efloor;
					}
				}
				flyTo(LatLonAlt.fromDD(tz, tx, ty), -1, 0, 0, 0, 0);
				ud_accel = angle / 1000;
				max_rate = 2;
			}
		}

	}

	private final void setRays(MouseEvent e)
	{
		final Camera camera = ptolemy.camera;
		final View view = ptolemy.view;
		final int screenWidth = ptolemy.events.screenWidth;
		final int screenHeight = ptolemy.events.screenHeight;

		double pixelCenterX = screenWidth / 2.0, pixelCenterY = screenHeight / 2.0; //number of pixels from view center to top/right edge of viewport
		double worldFOVDistY = Math.tan(Math3D.degToRad * (view.fov / 2.0)); //world distance from view center to top edge of viewport (on near viewplane)
		double worldFOVDistX = worldFOVDistY * (screenWidth / (double) screenHeight); //distance from view center to right edge of viewport (on near viewplane)

		double rightDist = ((e.getX() - pixelCenterX) / pixelCenterX) * worldFOVDistX; //horiz distance from view center to clicked point (on near viewplane)
		double upDist = ((pixelCenterY - e.getY()) / pixelCenterY) * worldFOVDistY; //vert distance from view center to clicked point (on near viewplane)

		pickray[0][0] = camera.cameraMat.m[0][0] * rightDist + camera.cameraMat.m[0][1] * upDist - camera.cameraMat.m[0][2];
		pickray[0][1] = camera.cameraMat.m[1][0] * rightDist + camera.cameraMat.m[1][1] * upDist - camera.cameraMat.m[1][2];
		pickray[0][2] = camera.cameraMat.m[2][0] * rightDist + camera.cameraMat.m[2][1] * upDist - camera.cameraMat.m[2][2];

		pickray[1][0] = (camera.cameraPos[0] + pickray[0][0]);
		pickray[1][1] = (camera.cameraPos[1] + pickray[0][1]);
		pickray[1][2] = (camera.cameraPos[2] + pickray[0][2]);

		pickray[0][0] = camera.cameraPos[0];
		pickray[0][1] = camera.cameraPos[1];
		pickray[0][2] = camera.cameraPos[2];
	}

	public synchronized void setRelativeMotionSpeedMultiplier(double rsm)
	{
		if (rsm > 0) {
			relspdmult = rsm;
		}
	}
}