package scikit.numerics.opt;

public interface C1Function1D {
	public double eval(double x);
	public double deriv(double x);
}
