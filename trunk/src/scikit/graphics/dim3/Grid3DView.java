package scikit.graphics.dim3;

abstract public class Grid3DView {
	// arrays for drawing cube panels
	protected static final double[][] _normals = new double[][] {
		{-1, 0, 0}, {+1, 0, 0},
		{0, -1, 0}, {0, +1, 0},
		{0, 0, -1}, {0, 0, +1}
	};
	protected static final double[][] _dx = new double[][]{
		{-1, -1, -1, -1},
		{+1, +1, +1, +1},
		{+1, -1, -1, +1},
		{+1, -1, -1, +1},
		{+1, +1, -1, -1},
		{-1, -1, +1, +1},
	};
	protected static final double[][] _dy = new double[][]{
		{+1, +1, -1, -1},
		{-1, -1, +1, +1},
		{-1, -1, -1, -1},
		{+1, +1, +1, +1},
		{+1, -1, -1, +1},
		{+1, -1, -1, +1}
	};
	protected static final double[][] _dz = new double[][]{
		{+1, -1, -1, +1},
		{+1, -1, -1, +1},
		{+1, +1, -1, -1},
		{-1, -1, +1, +1},
		{-1, -1, -1, -1},
		{+1, +1, +1, +1}
	};
	protected static final double[][] _texCoord = new double[][]{
		{0, 0}, {1, 0}, {1, 1}, {0, 1} // ???
	};
	
	abstract public void draw(Gfx3D g);
	abstract public void mouseDragged(double dx, double dy);
	abstract public void setDisplayParam(double x);
	abstract public double getDisplayParam();
}
