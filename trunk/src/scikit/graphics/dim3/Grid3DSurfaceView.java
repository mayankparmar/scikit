package scikit.graphics.dim3;

import javax.media.opengl.GL;

public class Grid3DSurfaceView extends Grid3DView {
	private Grid3D _grid;
	private int[] _dim;
	private double _cutoff = 0.5;
	
	
	public Grid3DSurfaceView(Grid3D grid) {
		_grid = grid;
	}
	
	public void mouseDragged(double dx, double dy) {
	}
	
	public double getDisplayParam() {
		return _cutoff;
	}

	public void setDisplayParam(double x) {
		_cutoff = x;
	}
	
	public void draw(Gfx3D g) {
		_dim = _grid.getDimensions();
		for (int z = 0; z < _dim[0]; z++) { 
			for (int y = 0; y < _dim[1]; y++) {
				for (int x = 0; x < _dim[2]; x++) {
					if (_grid.getSample(x, y, z) >= _cutoff) {
						g.setColor(_grid.getColor(x, y, z));
						for (int dir = 0; dir < 6; dir++) {
							int xp = x+(int)_normals[dir][0];
							int yp = y+(int)_normals[dir][1];
							int zp = z+(int)_normals[dir][2];
							if (_grid.getSample(xp, yp, zp) < _cutoff)
								drawPanel(g, x, y, z, dir);
						}
					}
				}
			}
		}
	}

	private void drawPanel(Gfx3D g, double x, double y, double z, int dir) {
		GL gl = g.getGL();
		gl.glBegin(GL.GL_QUADS);
		gl.glNormal3dv(_normals[dir], 0);
		for (int i = 0; i < 4; i++) {
			gl.glVertex3d(
					(x+0.5*_dx[dir][i]+0.5)/_dim[0],
					(y+0.5*_dy[dir][i]+0.5)/_dim[1],
					(z+0.5*_dz[dir][i]+0.5)/_dim[2]);
		}
		gl.glEnd();
	}
}
