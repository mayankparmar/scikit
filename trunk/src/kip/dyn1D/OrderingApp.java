package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


class Structure {
	int N, SN;
	
	public double[] avg;
	public Coarsened coarse;
	
	private double[][] accum;
	private int[] cnt;
	private double[] data;
	jnt.FFT.RealDoubleFFT fft;
	
	public Structure(int _N, int R, double kRmax, int numSteps) {
		N = _N;
		SN = (int) ((kRmax * N) / (2 * PI * R)) + 1;
		kRmax = 2 * PI * R * (SN-1) / N;
		
		avg = new double[SN];
		coarse = new Coarsened(avg, 0, kRmax, 0.1);
		
		accum = new double[numSteps][SN];
		cnt = new int[numSteps];
		data = new double[N];
		fft = new jnt.FFT.RealDoubleFFT_Radix2(N);
	}
	
	public double[] fn(double[] field, int timeIndex) {
		for (int i = 0; i < N; i++)
			data[i] = field[i];
		fft.transform(data);
		
		cnt[timeIndex]++;
		
		accum[timeIndex][0] += data[0]*data[0] / N;
		avg[0] = accum[timeIndex][0] / cnt[timeIndex];
		for (int i = 1; i < SN; i++) {
			accum[timeIndex][i] += (data[i]*data[i] + data[N-i]*data[N-i]) / N;
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
		params.add("kR maximum", 20.0, true);
		params.add("Coarse graining size", 0.1, false);
		params.add("Memory time", 20.0, true);
		params.add("Random seed", 0, true);
		params.add("N", 1<<20, true);
		params.add("R", 256, true);
		params.add("T", 4.0/9.0, false);
		params.add("J", 1.0, false);
		params.add("h", 0.0, false);
		params.add("dt", 0.2, false);
		outputs.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		outputs.set("time", sim.time());
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
	}
	
	
	public void run() {
		double kRmax = params.fget("kR maximum");
		
		sim = new Ising(params);
		
		field = sim.copyField(null);
		fieldPlot.setDataSet(0, new Coarsened(field, 0, sim.N, sim.N/100.0));
		fieldPlot.setYRange(-1, 1);
		
		structure = new Structure(sim.N, sim.R, kRmax, numSteps);
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
		structurePlot.setDataSet(0, structure.coarse);
		structurePlot.setDataSet(1, new Function(0, kRmax) {
			public double eval(double kR) {
				if (sim.time() == 0) return 1;
				double Q = kR == 0 ? 1 : sin(kR)/kR;
				double D = Q/sim.T - 1;
				return exp(2*D*sim.time())*(1 + 1/D) - 1/D;
			}
		});
		
		addDisplay(fieldPlot);
		addDisplay(structurePlot);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeSpins();
			
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
