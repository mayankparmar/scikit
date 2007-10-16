package scikit.numerics.opt;

import java.util.ArrayList;

abstract public class Optimizer {
	protected int _dim;
	protected C1Function _f;
	protected LinearOptimizer _linOpt;
	protected ArrayList<Constraint> _constraints;
	
	public Optimizer(int dim, LinearOptimizer linOpt) {
		_dim = dim;
		_linOpt = linOpt;
		_constraints = new ArrayList<Constraint>();
	}
	
	public void addConstraint(Constraint c) {
		_constraints.add(c);
	}
	
	public void setFunction(C1Function f) {
		_f = f;
	}
	
	/**
	 * Initialize the optimization with an initial guess p. The results
	 * of the optimization will be stored in p at each step.
	 * @param p guess for function minimum
	 */
	abstract public void initialize(double[] p);
	
	/**
	 * Perform an optimization step, updating the contents of the array
	 * p passed to the initialize method.
	 * @return A boolean indicating whether convergence has been reached
	 */
	abstract public boolean step();
}
