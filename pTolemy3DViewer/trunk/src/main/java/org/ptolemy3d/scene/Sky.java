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

package org.ptolemy3d.scene;

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import java.util.Random;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.view.Camera;

/**
 * Sky<BR>
 * ===<BR>
 * Earth atmosphere and stars
 */
public class Sky
{
	private static float[] horizonColor = { (float) 164 / 255, (float) 193 / 255, (float) 232 / 255};

	/** Instance of Ptolemy3D*/
	private Ptolemy3D ptolemy;
	/** Sky OpenGL Display List*/
	private int skyCallList = -1;

	/** Sky Texture ID */
	private int skyTexId = -1;
	/** Sky Rendering Enabled */
	public boolean drawSky = true;
	/** Fog Enabled */
	public boolean fogStateOn = true;
	/** Horizon altitude */
	public int horizonAlt = 10000;

	/* Local variable for rendering */
	private GL gl = null;

	private float[] botColor = new float[3];
	private float[] topColor = new float[3];
	private float[] zenithColor = { (float) 43 / 255, (float) 86 / 255, (float) 207 / 255};

	protected Sky(){}

	protected void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	protected void initGL(GL gl)
	{
		this.gl = gl;

		initSkyTexture();

		createSkyDisplayList();

		this.gl = null;
	}
	private final void createSkyDisplayList()
	{
		final int NUM_STARS = 200;

		final Ptolemy3DUnit unit = ptolemy.unit;

		float star_exp = (float) ((((horizonAlt * unit.coordSystemRatio) / 2) * 1.5) / Math.cos(30.0 * Math3D.degToRad));

		Random generator = new Random();
		skyCallList = gl.glGenLists(1);
		gl.glNewList(skyCallList, GL.GL_COMPILE);
		gl.glPointSize(1.0f);
		gl.glBegin(GL.GL_POINTS);

		float phi, theta, col, z;
		for (int i = 0; i < NUM_STARS; i++)
		{
			z = generator.nextFloat() * 2 - 1;
			phi = generator.nextFloat() * 2 * (float) Math.PI;
			theta = (float) Math.asin(z);
			col = generator.nextFloat() / 2 + 0.5f;
			gl.glColor3f(col, col, col);
			gl.glVertex3f(((float) Math.cos(theta) * (float) Math.cos(phi)) * star_exp,
					((float) Math.cos(theta) * (float) Math.sin(phi)) * star_exp, z * star_exp);
		}
		gl.glEnd();
		gl.glEndList();
	}


	private final void initSkyTexture()
	{
		int j, i, h, numLevels = 128;

		byte[] atm_tex = new byte[numLevels * 2 * 4];
		
		int offset = 0;
		// botColor is integer horizon color and topColor is the ratio...
		for (h = 0; h < 3; h++)
		{
			botColor[h] = (horizonColor[h] * 255);
			topColor[h] = ((zenithColor[h] * 255) - botColor[h]) / numLevels;
		}
		for (i = 0; i < numLevels; i++)
		{
			for (j = 0; j < 2; j++)
			{
				for (h = 0; h < 3; h++)
				{
					atm_tex[offset++] = (byte) (botColor[h] + (topColor[h] * i));
				}
				atm_tex[offset++] = (byte) ((float) (numLevels - i) / (numLevels) * 255);
			}
		}
		
		skyTexId = ptolemy.textureManager.load(gl, atm_tex, 2, numLevels, GL.GL_RGBA, false, -1);
	}

	protected void destroyGL(GL gl)
	{
		this.gl = gl;

		if (skyTexId != -1) {
			ptolemy.textureManager.unload(gl, skyTexId);
			skyTexId = -1;
		}
		if (skyCallList != -1) {
			gl.glDeleteLists(skyCallList, 1);
			skyCallList = -1;
		}

		this.gl = null;
	}

	protected void draw(GL gl)
	{
		if (!drawSky) {
			return;
		}

		final Camera camera = ptolemy.camera;

		int radius = 10000;
		if ((camera.getVerticalAltitudeMeters() > horizonAlt) || (camera.getLatAltLon().getAltitudeDD() > (radius - 1)))
		{
			this.gl = gl;

			if (horizonAlt != 0) {
				spaceAtmosphere();
			}
			this.gl = null;
			return;
		}

		this.gl = gl;

		gl.glDisable(GL.GL_FOG);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDepthMask(false);

		gl.glPushMatrix();
		{
			gl.glLoadIdentity();
			gl.glTranslated(0, -(EARTH_RADIUS / 500), -camera.getLatAltLon().getAltitudeDD());
			gl.glRotatef((float) (90 + ptolemy.camera.getPitchDegrees()), 1.0f, 0.0f, 0.0f);
		}

		if(skyCallList != -1) {
			gl.glCallList(skyCallList);
		}

		gl.glPopMatrix();
		gl.glDepthMask(true);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glShadeModel(GL.GL_FLAT);
		gl.glEnable(GL.GL_TEXTURE_2D);
		if (fogStateOn)
		{
			gl.glEnable(GL.GL_FOG);
		}

		this.gl = null;
	}

	private final void spaceAtmosphere()
	{
		final Camera camera = ptolemy.camera;

		int i;
		double r = EARTH_RADIUS;
		double cz = -camera.getLatAltLon().getAltitudeDD() - (Math.cos(camera.getPitchRadians()) * r);
		double cy = (Math.sin(camera.getPitchRadians()) * r);

		// find tangent lines
		double tls1 = Math.acos((-(cy * r) + (cz * Math.sqrt(cy * cy + cz * cz - r * r))) / (cy * cy + cz * cz));
		double tls2 = Math.acos((-(cy * r) - (cz * Math.sqrt(cy * cy + cz * cz - r * r))) / (cy * cy + cz * cz));

		double ypr1 = cy + r * Math.cos(tls1);
		double zpr1 = cz + r * Math.sin(tls1);
		double ypr2 = cy + r * Math.cos(tls2);
		double zpr2 = cz + r * Math.sin(tls2);

		double vec_y = ypr2 - ypr1;
		double vec_z = zpr2 - zpr1;
		double vmag = Math.sqrt(vec_y * vec_y + vec_z * vec_z);
		vec_y /= vmag;
		vec_z /= vmag;

		cy = ypr1 + vec_y * vmag / 2;
		cz = zpr1 + vec_z * vmag / 2;

		double theta = Math3D.angle3dvec(0, vec_y, vec_z, 0, 1, 0, false);

		double pt_x, pt_y, pt_z;

		int numSteps = 80;

		double theta2 = Math3D.twoPi, t3;

		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glColor3f(0, 0, 0);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, skyTexId);

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (i = 0; i <= numSteps; i++)
		{
			t3 = i * theta2 / numSteps;

			pt_x = (vmag / 2) * Math.sin(t3);
			pt_y = (vmag / 2) * Math.cos(t3);
			pt_z = pt_y * -Math.sin(theta) + cz;
			pt_y = pt_y * Math.cos(theta) + cy;

			gl.glTexCoord2f(((i % 2 == 0) ? 0 : 1), 0.95f);
			gl.glVertex3d(pt_x + (Math.sin(t3) * (r / 25)), pt_y + (Math.cos(t3) * (r / 25)), pt_z);
			gl.glTexCoord2f(((i % 2 == 0) ? 0 : 1), 0);
			gl.glVertex3d(pt_x, pt_y, pt_z);
		}
		gl.glEnd();

		//Stars
		gl.glLoadIdentity();
		gl.glRotated(camera.getPitchDegrees(), 1.0f, 0.0f, 0);
		gl.glMultMatrixd(Matrix16d.from(camera.vpMat).m, 0);	//FIXME Already calculated before ...
		gl.glDepthMask(false);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glCallList(skyCallList);
		gl.glDepthMask(true);
		gl.glEnable(GL.GL_DEPTH_TEST);

		gl.glPopMatrix();
	}
}
