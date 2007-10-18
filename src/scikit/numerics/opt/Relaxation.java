package scikit.numerics.opt;

public class Relaxation extends Optimizer {
	double dt;
	
	public Relaxation(int dim, double dt) {
		super(dim);
		this.dt = dt;
	}
	
	public void initialize(double[] p) {
		super.initialize(p);
	}

	public void step() {
		double[] d_f = df_constrained(p);
		for (int i = 0; i < _dim; i++) {
			p[i] += - dt * d_f[i];
		}
	}

}
