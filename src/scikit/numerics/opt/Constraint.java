package scikit.numerics.opt;

import scikit.numerics.fn.C1Function;

abstract public class Constraint extends C1Function {
	abstract public double tolerance();
}
