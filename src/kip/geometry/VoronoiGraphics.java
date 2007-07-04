package kip.geometry;

import javax.media.opengl.GL;

import scikit.graphics.Graphics;
import scikit.util.Bounds;
import scikit.util.Point;

public class VoronoiGraphics implements Graphics {
	private Bounds _bds;
	private QHull _geom;
	private Point[][] _faces;
	
	public VoronoiGraphics(Bounds bds) {
		_bds = bds;
		_geom = new QHull("/sw/bin/qhull");
	}

	public void clear() {
		_faces = null;
	}
	
	public void construct(double[] state, int stride, int N0, int N1) {
		_faces = _geom.constructVoronoi2D(state, stride, N0, N1);
	}
	
	public void draw(GL gl, Bounds bounds) {
		if (_faces == null)
			return;
		
		gl.glColor3d(1, 0, 0);
		gl.glBegin(GL.GL_LINES);
		
		for (Point[] face : _faces) {
			int n = face.length;
			for (int i = 0; i < n; i++) {
				Point v1 = face[(i+0)%n];
				Point v2 = face[(i+1)%n];
				if (v1 != null && v2 != null) {
					gl.glVertex2d(v1.x, v1.y);
					gl.glVertex2d(v2.x, v2.y);
				}
			}
		}
        
        gl.glEnd();
	}
	
	public Bounds getBounds() {
		return _bds;
	}

}
