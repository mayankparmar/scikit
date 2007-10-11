package scikit.numerics.opt;

import static java.lang.Math.*;

public class LinearOptimizer {
	protected Function1D _fn;
	static final double GOLDEN = (1+sqrt(5))/2; // = 1.618...
	static final double TOL = 3e-8; // sqrt of double precision, see N.R. discussion
	
	public LinearOptimizer(Function1D fn) {
		_fn = fn;
	}
	
	public double optimize(double x1, double x2) {
		double f1 = _fn.eval(x1);
		double f2 = _fn.eval(x2);
		assert(f1 != f2);
		double[] bracket = f1 > f2 ? bracket(x1, f1, x2, f2) : bracket(x2, f2, x1, f1);
		return optimize(bracket);
	}
	
	public double optimize(double[] bracket) {
		double x0 = bracket[0];
		double xm = bracket[1];
		double x3 = bracket[2];
		double x1, x2;
		if (abs(xm-x0) < abs(xm-x3)) {
			x1 = xm;
			x2 = (xm-x3)/GOLDEN + x3;
		}
		else {
			x1 = (xm-x0)/GOLDEN + x0; 
			x2 = xm;
		}
		double f1 = _fn.eval(x1);
		double f2 = _fn.eval(x2);
		while(abs(x3-x0) > TOL*(abs(x1)+abs(x2))) {
			if (f2 < f1) {
				x0 = x1;
				x1 = x2;
				x2 = (x2-x3)/GOLDEN + x3;
				f1 = f2;
				f2 = _fn.eval(x2);
			}
			else {
				x3 = x2;
				x2 = x1;
				x1 = (x1-x0)/GOLDEN + x0;
				f2 = f1;
				f1 = _fn.eval(f1);
			}
		}
		return f1 < f2 ? x1 : x2;
	}
	
	protected double[] bracket(double x1, double f1, double x2, double f2) {
		assert (f1 > f2);
		while (abs(x2-x1) < 1e10) {
			double x3 = x2 + GOLDEN*(x2-x1);
			double f3 = _fn.eval(x3);
			if (f2 < f3) {
				return new double[] {min(x1, x3), x2, max(x1, x3)};
			}
			else {
				assert(f3 < f2);
				x1 = x2;
				f1 = f2;
				x2 = x3;
				f2 = f3;
			}
		}
		throw new IllegalArgumentException("Doh");
	}
}
