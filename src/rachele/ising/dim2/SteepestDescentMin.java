package rachele.ising.dim2;

import scikit.dataset.PointSet;

abstract public class SteepestDescentMin {
	public int N;
	public int Lp;
	public double t;
	public double[] point;
	
	// free energy
	public double freeEnergy;
	public double [] direction;
	private double horizontalSlice;
	private double verticalSlice;
	private double dx;
	
	
	public abstract double freeEnergyCalc(double[] point);
	public abstract double[] steepestAscentCalc(double[] point);
	
	
	public SteepestDescentMin(double[] point, double horizontalSlice, double verticalSlice, double dx) {
		N = point.length;
		Lp =  (int)Math.sqrt((double)N);
		this.point = point;
		this.horizontalSlice = horizontalSlice;
		this.verticalSlice = verticalSlice;
		this.dx = dx;
		direction = new double [N];
	}
	
	public void step() {
		// Find direction of steepest Descent
		direction = steepestAscentCalc(point);
		for (int i = 0; i < N; i ++){
			direction[i] *= -1;
		}
		
		//Replace point with the minimum point
		LineMin linemin = new LineMin(point, direction) {
			double freeEnergyCalc(double[] point) {
				return SteepestDescentMin.this.freeEnergyCalc(point);
			}
		};
		freeEnergy = linemin.minValue;
		
	}
	
	public PointSet get_delHslice(){
		int y = (int) (horizontalSlice * Lp);
		double slice[] = new double[Lp];
		for (int x = 0; x < Lp; x++) {
			slice[x] = direction[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}	

	public PointSet get_delVslice(){
		int x = (int) (verticalSlice * Lp);
		double slice[] = new double[Lp];
		for (int y = 0; y < Lp; y++) {
			slice[y] = direction[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}
	
}
