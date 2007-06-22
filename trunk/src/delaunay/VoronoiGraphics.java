package delaunay;

import javax.media.opengl.GL;

import scikit.graphics.Drawable;
import scikit.util.Bounds;

public class VoronoiGraphics implements Drawable {
	private Bounds _bds;
	private DelaunayTriangulation _dt;
	
	public VoronoiGraphics(Bounds bds) {
		_bds = bds;
		clear();
	}

	public void clear() {
		Simplex<Pnt> simplex = new Simplex<Pnt>(new Pnt[] {
				new Pnt(_bds.xmin, _bds.ymin),
				new Pnt(_bds.xmin, 2*_bds.ymax - _bds.ymin),
				new Pnt(2*_bds.ymax - _bds.ymin, _bds.ymin)
		});
		_dt = new DelaunayTriangulation(simplex);	
	}
	
	public void addPoint(double x, double y) {
		_dt.delaunayPlace(new Pnt(x, y));
	}
	
	public void setPoints(double[] state, int stride, int N0, int N1) {
		clear();
		for (int i = N0; i < N1; i++) {
			addPoint(state[(2*i+0)*stride], state[(2*i+1)*stride]);
		}		
	}
	
	public void draw(GL gl, Bounds bounds) {
		gl.glColor3d(1, 0, 0);
		gl.glBegin(GL.GL_LINES);
		
        // Loop through all the edges of the DT (each is done twice)
        for (Simplex<Pnt> triangle : _dt) {
        	for (Simplex<Pnt> other : _dt.neighbors(triangle)) {
        		Pnt p = Pnt.circumcenter(triangle.toArray(new Pnt[0]));
        		Pnt q = Pnt.circumcenter(other.toArray(new Pnt[0]));
        		gl.glVertex2d(p.coord(0), p.coord(1));
        		gl.glVertex2d(q.coord(0), q.coord(1));
        	}
        }
        
        gl.glEnd();
	}
	
	public Bounds getBounds() {
		return _bds;
	}

}
