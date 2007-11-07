package scikit.graphics.dim3;

import scikit.numerics.vecmath.Quat4d;
import scikit.numerics.vecmath.Vector3d;

abstract public class Grid3DView {
	// normal vectors for each cube panel
	protected static final Vector3d[] _normal = new Vector3d[] {
		new Vector3d(-1, 0, 0),
		new Vector3d(+1, 0, 0),
		new Vector3d(0, -1, 0),
		new Vector3d(0, +1, 0),
		new Vector3d(0, 0, -1),
		new Vector3d(0, 0, +1),
	};
	// the four points of each cube panel
	protected static final Vector3d[][] _panel = new Vector3d[][] {
		{   new Vector3d(-1, +1, +1),
			new Vector3d(-1, +1, -1),
			new Vector3d(-1, -1, -1),
			new Vector3d(-1, -1, +1)
		},
		{   new Vector3d(+1, -1, +1),
			new Vector3d(+1, -1, -1),
			new Vector3d(+1, +1, -1),
			new Vector3d(+1, +1, +1)
		},
		{   new Vector3d(+1, -1, +1),
			new Vector3d(-1, -1, +1),
			new Vector3d(-1, -1, -1),
			new Vector3d(+1, -1, -1)
		},
		{   new Vector3d(+1, +1, -1),
			new Vector3d(-1, +1, -1),
			new Vector3d(-1, +1, +1),
			new Vector3d(+1, +1, +1)
		},
		{   new Vector3d(+1, +1, -1),
			new Vector3d(+1, -1, -1),
			new Vector3d(-1, -1, -1),
			new Vector3d(-1, +1, -1)
		},
		{   new Vector3d(-1, +1, +1),
			new Vector3d(-1, -1, +1),
			new Vector3d(+1, -1, +1),
			new Vector3d(+1, +1, +1)
		}
	};
	
	abstract public void draw(Gfx3D g);
	abstract public void rotateStructure(Quat4d q);
	abstract public void setDisplayParam(double x);
	abstract public double getDisplayParam();
}
