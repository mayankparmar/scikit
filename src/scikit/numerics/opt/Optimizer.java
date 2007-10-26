package scikit.numerics.opt;

import static java.lang.Math.abs;

import java.util.ArrayList;

import scikit.numerics.fn.C1Function;

import static scikit.util.DoubleArray.dot;


abstract public class Optimizer {
	 // sqrt of double precision, see Numerical Recipes discussion
	protected static final double FTOL = 3e-8;
	protected static final double EPS = 1e-12;
	protected int _dim;
	protected C1Function _f;
	protected ArrayList<Constraint> _constraints = new ArrayList<Constraint>();
	protected boolean _finished;
	protected double[] p;
	
	/**
	 * Creates a new optimizer
	 * @param dim the dimensions of the configuration space to be searched
	 */
	public Optimizer(int dim) {
		_dim = dim;
	}
	
	/**
	 * Adds a constraint which reduces the dimensionality of the configuration
	 * space to be searched
	 * @param c a constraint on the search space
	 */
	public void addConstraint(Constraint c) {
		_constraints.add(c);
	}
	
	/**
	 * Sets the objective function to be minimized
	 * @param f the objective function
	 */
	public void setFunction(C1Function f) {
		_f = f;
	}
	
	/**
	 * Returns true when the point p has converged to a local minimum
	 * of the objective function
	 * @return true if optimization has converged
	 */
	public boolean isFinished() {
		return _finished;
	}
	
	/**
	 * Initializes the optimization with an initial guess p. The results
	 * of the optimization will be stored in p at each step.
	 * @param p guess for function minimum
	 */
	public void initialize(double[] p) {
		this.p = p;
		_finished = false;
	}
	
	/**
	 * Perform an optimization step, updating the contents of the array
	 * p passed to the initialize method.
	 * @return a boolean indicating whether convergence has been reached
	 */
	abstract public void step();
	
	/**
	 * Returns true when two values of the objective function are as close
	 * as numerical precision allows.
	 * @param f1 The first value of the objective function
	 * @param f2 The second value of the objective function
	 * @return true if f1 and f2 are close enough that further optimization is unnecessary
	 */
	public static boolean objectiveConverged(double f1, double f2) {
		return 2*abs(f1-f2) <= FTOL*(abs(f1)+abs(f2)+EPS);
	}
	
	/**
	 *  Returns the gradient of the objective function after applying the constraints
	 *  through appropriate Lagrange multipliers.
	 *  @return the constrained gradient of the objective function
	 */
	protected double[] df_constrained(double[] p) {
		double[] d_f = _f.grad(p);
		for (Constraint c : _constraints) {
			double[] d_g = c.grad(p);
			double lambda = - dot(d_g, d_f) / dot(d_g, d_g);
			for (int i = 0; i < p.length; i++)
				d_f[i] += lambda*d_g[i];
		}
		return d_f;
	}
}
