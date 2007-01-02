package kip.dyn1D.apps;

import scikit.plot.*;
import scikit.jobs.*;
import kip.dyn1D.*;
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
		n = min(n, N/dx);
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
	AbstractIsing sim;
	Structure structure;
	double[] field;
	int numSteps = 10;
	
	public static void main(String[] args) {
		frame(new Control(new OrderingApp()), "Growth for Ising Droplets");
	}

	public OrderingApp() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis", "Kawasaki Glauber", "Kawasaki Metropolis"));
		params.add("Simulation type", new ChoiceValue("Ising", "Langevin"));
		params.add("kR maximum", 20.0);
		params.addm("Coarse graining size", 0.1);
		params.add("Random seed", 0);
		params.add("N", 1<<20);
		params.add("R", 512);
		params.add("dx", 32);
		params.addm("T", 4.0/9.0);
		params.addm("J", 1.0);
		params.addm("dt", 0.1);
		params.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		params.set("time", DoubleValue.format(sim.time()));
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
		fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.copyField()));
	}
	
	
	public void run() {
		double kRmax = params.fget("kR maximum");
		
		String type = params.sget("Simulation type");
		sim = type.equals("Ising") ? new Ising(params) : new FieldIsing(params);
		
		fieldPlot.setYRange(-1, 1);
		addDisplay(fieldPlot);
		
		structure = new Structure(sim.N, sim.dx, sim.R, kRmax, numSteps);
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
		addDisplay(structurePlot);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeField(0);
			
			for (int i = 0; i < numSteps; i++) {
				structure.fn(sim.copyField(), i);
				yield();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
