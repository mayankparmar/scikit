package scikit.numerics.opt;

public interface C1Function {
	public double eval(double[] p);
	public void grad(double[] p, double[] dir);
}
