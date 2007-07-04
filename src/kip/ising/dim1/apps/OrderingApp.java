package kip.ising.dim1.apps;

import scikit.params.ChoiceValue;
import static scikit.util.Utilities.format;
import scikit.plot.*;
import scikit.dataset.*;
import scikit.jobs.*;
import kip.ising.dim1.AbstractIsing;
import kip.ising.dim1.FieldIsing;
import kip.ising.dim1.Ising;
import static java.lang.Math.*;


class Structure {
	int Lp;
	double L;
	double R;
	double kRmin = 0, kRmax = Double.MAX_VALUE;
	
	scikit.numerics.fft.RealDoubleFFT fft;
	private double[] fftScratch;
	
	public Structure(int Lp, double L, int R) {
		this.Lp = Lp;
		this.L = L;
		this.R = R;
		fftScratch = new double[Lp];
		fft = new scikit.numerics.fft.RealDoubleFFT_Radix2(Lp);
	}

	public void setBounds(double kRmin, double kRmax) {
		this.kRmin = kRmin;
		this.kRmax = kRmax;
	}
	
	public void accumulate(double[] field, Accumulator acc) {
		double dx = L/Lp;
		for (int i = 0; i < Lp; i++)
			fftScratch[i] = field[i];
		fft.transform(fftScratch);
		
		for (int i = 0; i < Lp/2; i++) {
			double kR = 2*PI*i*R/L;
			if (kR >= kRmin && kR <= kRmax) {			
				double re = fftScratch[i];
				double im = (i == 0) ? 0 : fftScratch[Lp-i];
				acc.accum(kR, (re*re+im*im)*dx*dx/L);
			}
		}
	}
	
	public double theory(AbstractIsing sim, double kR) {
		double Q = kR == 0 ? 1 : sin(kR)/kR;
		double K = sim.J/sim.T;
		double t = sim.time();
		double M = 2;
		double D;
		
		switch (sim.dynamics) {
		case METROPOLIS:
			M = 4; // fall through
		case GLAUBER:
			D = -1 + Q*K;
			return (exp(M*D*t)*(1 + 1/D) - 1/D);
			
		case KAWA_METROPOLIS:
			M = 4; // fall through
		case KAWA_GLAUBER:
			D = -1 + Q*(1 + K) - Q*Q*K;
			return  kR == 0 ? 1 : exp(M*D*t)*(1+(1-Q)/D) - (1-Q)/D;

		default:
			return Double.NaN;
		}
	}
	
	
	public void theory(AbstractIsing sim, Accumulator acc) {
		for (int i = 0; i < Lp/2; i++) {
			double kR = 2*PI*i*R/L;
			if (kR >= kRmin && kR <= kRmax)
				acc.accum(kR, theory(sim, kR));
		}
	}
}


public class OrderingApp extends Simulation {
	Plot fieldPlot = new Plot("Fields", true);
	Plot structurePlot = new Plot("Structure", true);
	AbstractIsing sim;
	Structure structure;
	
	Accumulator[] structSim;
	Accumulator structTheory;
	
	double[] field;
	int numSteps = 10;
	
	public static void main(String[] args) {
		new Control(new OrderingApp(), "Growth for Ising Droplets");
	}

	public OrderingApp() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis", "Kawasaki Glauber", "Kawasaki Metropolis"));
		params.add("Simulation type", new ChoiceValue("Ising", "Langevin"));
		params.add("kR maximum", 20.0);
		params.add("kR bin width", 0.1);
		params.add("Random seed", 0);
		params.add("N", 1<<20);
		params.add("R", 1<<12);
		params.add("dx", 1<<6);
		params.add("T", 4.0/9.0);
		params.add("J", 1.0);
		params.add("dt", 0.1);
		params.add("time");
	}
	
	public void animate() {
		params.set("time", format(sim.time()));
		fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.copyField()));
	}
	
	private Accumulator makeStructureAccumulator() {
		Accumulator ret = new Accumulator(params.fget("kR bin width"));
		ret.setAveraging(true);
		return ret;
	}
	
	public void run() {
		String type = params.sget("Simulation type");
		sim = type.equals("Ising") ? new Ising(params) : new FieldIsing(params);
		
		fieldPlot.setYRange(-1, 1);
		Job.addDisplay(fieldPlot);
		Job.addDisplay(structurePlot);
		
		structure = new Structure(sim.N/sim.dx, sim.N, sim.R);
		structure.setBounds(0, params.fget("kR maximum"));
		structSim = new Accumulator[numSteps];
		for (int i = 0; i < numSteps; i++)
			structSim[i] = makeStructureAccumulator();
		structTheory = makeStructureAccumulator();
		
		while (true) {
			sim.initialize(params);
			sim.randomizeField(0);
//			sim.setField(0);
			
			for (int i = 0; i < numSteps; i++) {
				sim.step();
				structTheory.clear();
				structure.theory(sim, structTheory);
				structure.accumulate(sim.copyField(), structSim[i]);
				structurePlot.setDataSet(0, structSim[i]);
				structurePlot.setDataSet(1, structTheory);
//				final int ip = i;
//				structurePlot.setDataSet(2, new DiscreteFunction(structTheory.copyData(), 2) {
//					public double eval(double kR) {
//						return (structTheory.eval(kR)-structSim[ip].eval(kR))*sqrt(sim.R);
//					}
//				});
				
				Job.animate();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
