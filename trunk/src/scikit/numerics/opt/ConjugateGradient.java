package scikit.numerics.opt;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;


public class ConjugateGradient extends Optimizer {
	static final double FTOL = 3e-8; // sqrt of double precision, see N.R. discussion
	static final double EPS = 1e-12;
	double[] temp1, temp2;
	double[] p, g, h, d_f;
	double fp;
	int iter;
	
	public ConjugateGradient(int dim, LinearOptimizer linOpt) {
		super(dim, linOpt);
		temp1 = new double[_dim];
		temp2 = new double[_dim];
		g = new double[_dim];
		h = new double[_dim];
		d_f = new double[_dim];
	}
	
	public void initialize(double[] p) {
		this.p = p;
		fp = _f.eval(p);
		_f.grad(p, d_f);
		for (int i = 0; i < _dim; i++) {
			g[i] = h[i] = -d_f[i];
		}
		iter = 0;
		_finished = false;
	}
	
	public void step() {
		// double fp2 = linmin(p, d_f); // uncomment for steepest descent
		double fp2 = linmin(p, h);
		if (2*abs(fp2-fp) <= FTOL*(abs(fp2)+abs(fp)+EPS)) {
			_finished = true;
			return;
		}
		fp = fp2;
		_f.grad(p, d_f);
		double gg = dotProduct(g, g);
		if (gg == 0) {
			_finished = true;
			return;
		}
		double gamma = 0;
		for (int i = 0; i < _dim; i++)
			gamma += (-d_f[i]-g[i])*(-d_f[i]);
		gamma /= gg;
		for (int i = 0; i < _dim; i++) {
			g[i] = -d_f[i];
			h[i] = -d_f[i] + gamma*h[i];
		}
		iter++;
	}
	
	// given a n-dimensional point p and a direction dir, moves and resets p
	// to where the function fn is takes a minimum along the direction dir from
	// p. returns the value of fn at the returned location p.
	double linmin(final double[] p, final double[] dir) {
		C1Function1D f_lin = new C1Function1D() {
			public double eval(double x) {
				for (int i = 0; i < _dim; i++)
					temp1[i] = p[i] + x*dir[i];
				return _f.eval(temp1);
			}
			public double deriv(double x) {
				for (int i = 0; i < _dim; i++)
					temp1[i] = p[i] + x*dir[i];
				_f.grad(temp1, temp2);
				return dotProduct(temp2, dir)/sqrt(dotProduct(dir,dir));
			}
		};
		_linOpt.setFunction(f_lin);
		double[] ret = _linOpt.optimize(0);
		for (int i = 0; i < _dim; i++)
			p[i] += ret[0]*dir[i];
		return ret[1];
	}
	
	double dotProduct(double[] a, double[] b) {
		double ret = 0;
		for (int i = 0; i < _dim; i++)
			ret += a[i]*b[i];
		return ret;
	}
}
