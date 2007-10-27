package kip.clump.dim2.apps;

import static java.lang.Math.log;
import static scikit.util.Utilities.frame;
import kip.clump.dim2.FieldClump2D;
import scikit.graphics.GrayScale;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.numerics.fn.C1Function;
//import scikit.numerics.opt.ConjugateGradient;
import scikit.numerics.opt.Constraint;
//import scikit.numerics.opt.LinearOptimizer;
import scikit.numerics.opt.Optimizer;
import scikit.numerics.opt.Relaxation;
import scikit.params.ChoiceValue;
import scikit.util.DoubleArray;
import scikit.util.Pair;

public class Saddle2App extends Simulation {
	final double inf = Double.POSITIVE_INFINITY;
	
	Grid grid = new Grid("Grid");
	Plot plot = new Plot("");
	FieldClump2D clump;
	
	public static void main(String[] args) {
		new Control(new Saddle2App(), "Clump Model Saddle Profile");
	}

	public Saddle2App() {
		frame(grid, plot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.1);
		params.addm("dt", 1.0);
		params.add("R", 1000.0);
		params.add("L", 5000.0);
		params.add("dx", 100.0);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		params.add("Valid profile");
		flags.add("Res up");
		flags.add("Res down");
	}
	
	public void animate() {
		grid.setColors(new GrayScale());
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0.2, 4);
		grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());

//		plot.registerLines("", new PointSet(0, 1, section), Color.BLUE);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
	}
	
	public void run() {
		clump = new FieldClump2D(params);
		clump.initializeFieldWithSeed();
		
		C1Function f = new C1Function() {
			double[] grad = new double[clump.phi.length];
			public Pair<Double,double[]> calculate(final double[] p) {
				final double[] pb = clump.phi_bar;
//				clump.convolveWithRange(p, pb, clump.R);
				double fe_acc = 0;
				for (int i = 0; i < p.length; i++) {
					if (p[i] <= 0) {
						fe_acc = grad[i] = inf;
					}
					else {
						fe_acc += (p[i]*pb[i]/2+clump.T*p[i]*log(p[i])) / p.length;
						grad[i] = pb[i]+clump.T*log(p[i]);
					}
				}
				double mu = DoubleArray.mean(grad);
				for (int i = 0; i < p.length; i++) {
					grad[i] -= mu;
				}
				return new Pair<Double,double[]>(fe_acc, grad);
			}
		};
		
		Constraint c = new Constraint() {
			double[] grad = new double[clump.phi.length];
			public double tolerance() {
				return 0.01;
			}
			public Pair<Double,double[]> calculate(double[] p) {
				double c = 0;
				for (int i = 0; i < p.length; i++) {
					c += (p[i]-1)*(p[i]-1) / p.length;
					grad[i] = 2*(p[i]-1);
				}
				return new Pair<Double,double[]>(c, grad);
			}
		};

		Optimizer opt = new Relaxation(clump.phi.length, 0.1);
		opt.setFunction(f);
		opt.addConstraint(c);
		opt.initialize(clump.phi);
		
		while (!opt.isFinished()) {
			opt.step();
			System.out.println(f.eval(clump.phi) + " " + DoubleArray.mean(clump.phi));
			Job.animate();
		}
		System.out.println("done");
	}
	
}
