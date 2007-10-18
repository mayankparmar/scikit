package scikit.numerics.opt;

import scikit.util.Pair;

abstract public class C1Function1D {
	abstract public Pair<Double,Double> calculate(double x);
	
	public double eval(double x) {
		return calculate(x).fst();
	}
	
	public double deriv(double x) {
		return calculate(x).snd();
	}
}
