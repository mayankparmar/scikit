package scikit.graphics.dim3;

import static java.lang.Math.rint;
import static java.lang.Math.sqrt;

import java.awt.Color;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import scikit.vecmath.Quat4d;
import scikit.vecmath.VecHelper;
import scikit.vecmath.Vector3d;

import com.sun.opengl.util.BufferUtil;

public class Grid3DSliceView extends Grid3DView {
	private final int CUBE_SIDES=6;
	private final int PANELS=7;
	
	private double _depth = 0.5;
	private Quat4d _rotation = new Quat4d(0, 0, 0, 1);
	private Grid3D _grid;
	private int[] _dim;
	
	public Grid3DSliceView(Grid3D grid) {
		_grid = grid;
	}
	
	public void mouseDragged(double dx, double dy) {
	}
	
	public double getDisplayParam() {
		return _depth;
	}

	public void setDisplayParam(double x) {
		_depth = x;
	}

	public void draw(Gfx3D g) {
		_dim = _grid.getDimensions();
		g.setColor(Color.WHITE);
		
		GL gl = g.getGL();
		gl.glEnable(GL.GL_TEXTURE_2D);
		int[] textures = buildTextures(g);
		
		for (int side = 0; side < CUBE_SIDES; side++) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, textures[side]);
			gl.glBegin(GL.GL_QUADS);
			gl.glNormal3dv(_normals[side], 0);
			for (int i = 0; i < 4; i++) {
				gl.glTexCoord2d(_texCoord[i][0], _texCoord[i][1]);
				gl.glVertex3d(
						0.5*_dx[side][i]+0.5,
						0.5*_dy[side][i]+0.5,
						0.5*_dz[side][i]+0.5);
			}
			gl.glEnd();
		}
		
		/*
		Vector3d n = new Vector3d(0, 0, 1);
		Vector3d v1 = new Vector3d(-0.5*sqrt(3), -0.5*sqrt(3), 0);
		Vector3d v2 = new Vector3d(+0.5*sqrt(3), -0.5*sqrt(3), 0);
		Vector3d v3 = new Vector3d(+0.5*sqrt(3), +0.5*sqrt(3), 0);
		Vector3d v4 = new Vector3d(-0.5*sqrt(3), +0.5*sqrt(3), 0);
		Quat4d q = new Quat4d(g.rotation());
		q.inverse();
		q.rotate(n);
		q.rotate(v1);
		q.rotate(v2);
		q.rotate(v3);
		q.rotate(v4);
		gl.glBegin(GL.GL_QUADS);
		gl.glNormal3d(n.x, n.y, n.z);
		gl.glTexCoord2d(0, 0);
		gl.glVertex3d(0.5+v1.x, 0.5+v1.y, 0.5+v1.z);
		gl.glTexCoord2d(1, 0);
		gl.glVertex3d(0.5+v2.x, 0.5+v2.y, 0.5+v2.z);
		gl.glTexCoord2d(1, 1);
		gl.glVertex3d(0.5+v3.x, 0.5+v3.y, 0.5+v3.z);
		gl.glTexCoord2d(0, 1);
		gl.glVertex3d(0.5+v4.x, 0.5+v4.y, 0.5+v4.z);
		gl.glEnd();
		*/
		gl.glDeleteTextures(PANELS, textures, 0);
		gl.glDisable(GL.GL_TEXTURE_2D);		
	}
	
	private void putColor(ByteBuffer buffer, Color c) {
		buffer.put((byte)c.getRed());
		buffer.put((byte)c.getGreen());
		buffer.put((byte)c.getBlue());
		buffer.put((byte)c.getAlpha());
	}
	
	private void writePixels(int side, int npix, ByteBuffer buffer) {
		if (side < 6) {
			Vector3d v0 = new Vector3d((_dx[side][0]+1)/2, (_dy[side][0]+1)/2, (_dz[side][0]+1)/2);
			Vector3d v1 = new Vector3d((_dx[side][1]+1)/2, (_dy[side][1]+1)/2, (_dz[side][1]+1)/2);
			Vector3d v3 = new Vector3d((_dx[side][3]+1)/2, (_dy[side][3]+1)/2, (_dz[side][3]+1)/2);

			for (int py = 0; py < npix; py++) {
				for (int px = 0; px < npix; px++) {
					double gx = v0.x + ((v1.x-v0.x)*px+(v3.x-v0.x)*py)/(npix-1);
					double gy = v0.y + ((v1.y-v0.y)*px+(v3.y-v0.y)*py)/(npix-1);
					double gz = v0.z + ((v1.z-v0.z)*px+(v3.z-v0.z)*py)/(npix-1);
					int ix = (int)rint(gx*(_dim[0]-1));
					int iy = (int)rint(gy*(_dim[1]-1));
					int iz = (int)rint(gz*(_dim[2]-1));
					putColor(buffer, _grid.getColor(ix, iy, iz));
//					putColor(buffer, Color.RED);
				}
			}
		}
		else {
			Vector3d v = new Vector3d();
			for (int py = 0; py < npix; py++) {
				for (int px = 0; px < npix; px++) {
					v.set(sqrt(3)*(px/(npix-0.)-0.5), sqrt(3)*(py/(npix-0.)-0.5), 0);
					VecHelper.rotate(_rotation, v);
					int x = (int)(_dim[0]*(v.x+0.5));
					int y = (int)(_dim[1]*(v.y+0.5));
					int z = (int)(_dim[2]*(v.z+0.5));
					Color c = _grid.getColor(x, y, z);
					putColor(buffer, c);
				}
			}
		}
		buffer.flip();
	}
	
	private int[] buildTextures(Gfx3D g) {
		GL gl = g.getGL();

		int npix = 64;
		ByteBuffer buffer = BufferUtil.newByteBuffer(npix*npix*4);

		int[] textures = new int[PANELS];
		gl.glGenTextures(PANELS, textures, 0);
		for (int side = 0; side < PANELS; side++) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, textures[side]);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			buffer.clear();
			writePixels(side, npix, buffer);
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, npix, npix, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
		}
		
		return textures;
	}
}
