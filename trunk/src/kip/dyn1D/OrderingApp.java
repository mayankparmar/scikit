package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


class Structure {
	int N, n, dx;
	
	public double[] avg;
	public Coarsened coarse;
	
	private double[][] accum;
	private int[] cnt;
	private double[] data;
	jnt.FFT.RealDoubleFFT fft;
	
	public Structure(int N, int dx, int R, double kRmax, int numSteps) {
		this.N = N;
		this.dx = dx;
		n = (int) ((kRmax * N) / (2 * PI * R)) + 1;
		kRmax = 2 * PI * R * (n-1) / N;
		
		avg = new double[n];
		coarse = new Coarsened(avg, 0, kRmax, 0.1);
		
		accum = new double[numSteps][n];
		cnt = new int[numSteps];
		data = new double[N/dx];
		fft = new jnt.FFT.RealDoubleFFT_Radix2(N/dx);
	}
	
	public double[] fn(double[] field, int timeIndex) {
		for (int i = 0; i < N/dx; i++)
			data[i] = field[i];
		fft.transform(data);
		
		cnt[timeIndex]++;
		
		accum[timeIndex][0] += data[0]*data[0] / (N/(dx*dx));
		avg[0] = accum[timeIndex][0] / cnt[timeIndex];
		for (int i = 1; i < n; i++) {
			accum[timeIndex][i] += (data[i]*data[i] + data[N/dx-i]*data[N/dx-i]) / (N/(dx*dx));
			avg[i] = accum[timeIndex][i] / cnt[timeIndex];
		}
		
		return avg;
	}
}


public class OrderingApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Plot structurePlot = new Plot("Structure", true);
	Dynamics1D sim;
	Structure structure;
	double[] field;
	int numSteps = 10;
	
	public static void main(String[] args) {
		frame(new Control(new OrderingApp()), "Growth for Ising Droplets");
	}

	public OrderingApp() {
		params.add("Dynamics", true, "Ising Glauber", "Ising Metropolis", "Kawasaki Glauber", "Kawasaki Metropolis");
		params.add("Simulation type", true, "Ising", "Langevin");
		params.add("kR maximum", 20.0, true);
		params.add("Coarse graining size", 0.1, false);
		params.add("Random seed", 0, true);
		params.add("N", 1<<20, true);
		params.add("R", 512, true);
		params.add("T", 4.0/9.0, false);
		params.add("J", 1.0, false);
		params.add("dt", 0.1, false);
		outputs.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		outputs.set("time", sim.time());
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
	}
	
	
	public void run() {
		double kRmax = params.fget("kR maximum");
		
		String type = params.sget("Simulation type");
		sim = type.equals("Ising") ? new Ising(params) : new FieldIsing(params);
		
		field = sim.copyField(null);
		fieldPlot.setDataSet(0, new Coarsened(field, 0, sim.N, sim.N/100.0));
		fieldPlot.setYRange(-1, 1);
		
		structure = new Structure(sim.N, sim.blocklen, sim.R, kRmax, numSteps);
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
		structurePlot.setDataSet(0, structure.coarse);
		structurePlot.setDataSet(1, new Function(0, kRmax) {
			public double eval(double kR) {
				if (sim.time() == 0) return 1;
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
					return exp(M*D*t)*(1 + 1/D) - 1/D;
					
				case KAWA_METROPOLIS:
					M = 4; // fall through
				case KAWA_GLAUBER:
					D = -1 + Q*(1 + K) - Q*Q*K;
					return kR == 0 ? 1 : exp(M*D*t)*(1+(1-Q)/D) - (1-Q)/D;
					
				default:
					return Double.NaN;
				}
			}
		});
		
		addDisplay(fieldPlot);
		addDisplay(structurePlot);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeField(0);
			
			for (int i = 0; i < numSteps; i++) {
				sim.copyField(field);
				structure.fn(field, i);
				yield();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
