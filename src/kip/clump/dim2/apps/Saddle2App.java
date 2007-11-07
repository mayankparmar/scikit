package kip.clump.dim2.apps;

import static java.lang.Math.log;
import static kip.util.MathPlus.hypot;
import static kip.util.MathPlus.j1;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;
import kip.util.Random;
import scikit.graphics.GrayScale;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.numerics.fft.util.FFT2D;
import scikit.numerics.fn.C1Function;
import scikit.numerics.fn.Function2D;
//import scikit.numerics.opt.Constraint;
import scikit.numerics.opt.Relaxation;
import scikit.util.DoubleArray;
import scikit.util.Pair;

public class Saddle2App extends Simulation {
	final double inf = Double.POSITIVE_INFINITY;
	
	Grid grid = new Grid("Grid");
	Plot plot = new Plot("");
	
	double L, R, T, dt;
	int dim;
	double[] phi, phibar;
	Random random = new Random();
	FFT2D fft;
	double fe;
	int time;
	double targetVar;
	
	
	public static void main(String[] args) {
		new Control(new Saddle2App(), "Clump Optimizer");
	}

	public Saddle2App() {
		frame(grid, plot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.05);
		params.addm("dt", 0.1);
		params.addm("var", 2);
		params.add("R", 1000.0);
		params.add("L", 5000.0);
		params.add("dim", 32);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
	}
	
	public void animate() {
		T = params.fget("T");
		dt = params.fget("dt");
		targetVar = params.fget("var"); 
		
		grid.setColors(new GrayScale());
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0, 4);
		grid.registerData(dim, dim, phi);
		
		params.set("F density", format(fe-0.5));
		params.set("Time", time);
//		plot.registerLines("", new PointSet(0, 1, section), Color.BLUE);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
	}
	
	public void run() {
		T = params.fget("T");
		dt = params.fget("dt");
		R = params.fget("R");
		L = params.fget("L");
		dim = params.iget("dim");
		time = 0;
		
		phi = new double[dim*dim];
		phibar = new double[dim*dim];
		for (int i = 0; i < dim*dim; i++)
			phi[i] = 1 + 0.01*random.nextGaussian();
		
		fft = new FFT2D(dim, dim);
		fft.setLengths(L, L);
		
		random.setSeed(params.iget("Random seed"));
		
		C1Function f = new C1Function() {
			double[] grad = new double[dim*dim];
			public Pair<Double,double[]> calculate(final double[] p) {
				final double[] pb = phibar;
				fft.convolve(p, pb, potential);
				double fe_acc = 0;
				for (int i = 0; i < p.length; i++) {
					if (p[i] <= 0) {
						fe_acc = grad[i] = inf;
					}
					else {
						fe_acc += (p[i]*pb[i]/2+T*p[i]*log(p[i])) / p.length;
						grad[i] = pb[i]+T*log(p[i]);
					}
				}
				double mu = DoubleArray.mean(grad);
				for (int i = 0; i < p.length; i++) {
					grad[i] -= mu;
				}
				return new Pair<Double,double[]>(fe_acc, grad);
			}
		};
		
//		Constraint c = new Constraint() {
//			double[] grad = new double[dim*dim];
//			public double tolerance() {
//				return 0.01;
//			}
//			public Pair<Double,double[]> calculate(double[] p) {
//				double c = 0;
//				for (int i = 0; i < p.length; i++) {
//					c += (p[i]-1)*(p[i]-1) / p.length;
//					grad[i] = 2*(p[i]-1);
//				}
//				c -= targetVar;
//				return new Pair<Double,double[]>(c, grad);
//			}
//		};

		Relaxation opt = new Relaxation(dim*dim, dt);
		opt.setFunction(f);
//		opt.addConstraint(c);
		opt.initialize(phi);
		
		while(true) {
			opt.setStepSize(dt);
			opt.step();
			fe = f.eval(phi);
			time++;
			Job.animate();
		}
	}
	
	private Function2D potential = new Function2D() {
		public double eval(double k1, double k2) {
			double kR = hypot(k1,k2)*R;
			return kR == 0 ? 1 : 2*j1(kR)/kR;
		}
	};
}
