package kip.clump.dim2.apps;

import static java.lang.Math.log;
import static scikit.util.Utilities.frame;
import kip.clump.dim2.FieldClump2D;
import kip.util.DoubleArray;
import scikit.graphics.GrayScale;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.numerics.opt.C1Function;
import scikit.numerics.opt.ConjugateGradient;
import scikit.numerics.opt.Constraint;
import scikit.numerics.opt.LinearOptimizer;
import scikit.params.ChoiceValue;

public class Saddle2App extends Simulation {
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
//		ConjugateGradient opt = new ConjugateGradient(2, new LinearOptimizer());
//		opt.setFunction(new C1Function() {
//			public double eval(double[] p) {
//				return (p[0]-3)*(p[0]-3) + 2*(p[1]+1)*(p[1]+1);
//			}
//			public void grad(double[] p, double[] ret) {
//				ret[0] = 2*(p[0]-3);
//				ret[1] = 4*(p[1]+1);
//			}
//		});
//		double[] p = new double[] {4, 3};
//		opt.initialize(p);

		clump = new FieldClump2D(params);
		clump.initializeFieldWithSeed();
		
		C1Function f = new C1Function() {
			double freeEnergy(double p, double pb) {
				double inf = Double.POSITIVE_INFINITY;
				double f = 0.5*p*pb+clump.T*p*log(p);
				return p <= 0 ? inf : f; 
			}
			double delFreeEnergy(double p, double pb) {
				return pb+clump.T*log(p);
			}
			public double eval(double[] p) {
				double[] pb = clump.phi_bar;
				clump.convolveWithRange(p, pb, clump.R);
				double acc = 0;
				for (int i = 0; i < p.length; i++) {
					acc += freeEnergy(p[i], pb[i]);
				}
				return acc / p.length;
			}
			public void grad(double[] p, double[] ret) {
				double[] pb = clump.phi_bar;
				clump.convolveWithRange(p, pb, clump.R);
				for (int i = 0; i < p.length; i++) {
					ret[i] = delFreeEnergy(p[i], pb[i]);
				}
				double mu = DoubleArray.mean(ret);
				for (int i = 0; i < p.length; i++) {
					ret[i] -= mu;
				}
			}
		};
		
		Constraint c = new Constraint() {
			public double tolerance() {
				return 0.01;
			}
			public double eval(double[] p) {
				// TODO
				return 0;
			}
			public void grad(double[] p, double[] dir) {
				// TODO Auto-generated method stub
			}
		};
		c.eval(null);
		
		ConjugateGradient opt = new ConjugateGradient(clump.phi.length, new LinearOptimizer());
		opt.setFunction(f);
		opt.initialize(clump.phi);
		
		while (!opt.isFinished()) {
			opt.step();
			System.out.println(f.eval(clump.phi) + " " + DoubleArray.mean(clump.phi));
			Job.animate();
		}
		System.out.println("done");
	}
	
}
