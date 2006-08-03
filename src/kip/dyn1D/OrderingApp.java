package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


class Structure {
	int N, SN;
	
	public double[] theory;
	public double[] avg;
	public Coarsened coarse;
	
	private double[][] accum;
	private int[] cnt;
	private double[] data;
	jnt.FFT.RealDoubleFFT fft;
	
	public Structure(int _N, int numSteps) {
		N = _N;
		SN = N / 40;
		
		theory = new double[SN];
		avg = new double[SN];
		coarse = new Coarsened(avg);
		
		accum = new double[numSteps][SN];
		cnt = new int[numSteps];
		data = new double[N];
		fft = new jnt.FFT.RealDoubleFFT_Radix2(N);
	}
	
	public double[] fn(int[] spins, int timeIndex) {
		for (int i = 0; i < N; i++)
			data[i] = spins[i];
		fft.transform(data);
		
		cnt[timeIndex]++;
		
		accum[timeIndex][0] += data[0]*data[0] / N;
		avg[0] = accum[timeIndex][0] / cnt[timeIndex];
		for (int i = 1; i < SN; i++) {
			accum[timeIndex][i] += (data[i]*data[i] + data[N-i]*data[N-i]) / N;
			avg[i] = accum[timeIndex][i] / cnt[timeIndex];
		}

		coarse.updateAll();
		return avg;
	}
	
	public double[] theory(double t, double R, double T) {
		for (int i = 0; i < SN; i++) {
			double k = 1e-10 + 2.0 * PI * i / N;
			double D = sin(k*R)/(T*k*R) - 1;
//			double gamma = 2 - 4*t;
//			exp(2*t*D) + (gamma / (2*D)) * (1 - exp(-2*t*D));
			double gamma = 2;
			theory[i] = exp(2*t*D) + (gamma / (2*D)) * (-1 + exp(2*t*D));
		}
		return theory;
	}
}


public class OrderingApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Plot structurePlot = new Plot("Structure", true);
	Ising sim;
	Structure structure;
	int numSteps = 10;
	
	public static void main(String[] args) {
		frame(new Control(new OrderingApp()), "Growth for Ising Droplets");
	}

	public OrderingApp() {
		params.add("Memory time", 20.0, true);
		params.add("Random seed", 0, true);
		params.add("Coarse graining size", 200.0, false);
		params.add("N", 1<<20, true);
		params.add("R", 512, true);
		params.add("T", 4.0/9.0, false);
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
		sim = new Ising(params);
		structure = new Structure(sim.N, numSteps);
		structure.coarse.setBinWidth(params.fget("Coarse graining size"));
		
		fieldPlot.setDataSet(0, new PointSet(0, sim.systemSize()/sim.ψ.length, sim.ψ));
		fieldPlot.setYRange(-1, 1);
		structurePlot.setDataSet(0, structure.coarse);
		structurePlot.setDataSet(1, new YArray(structure.theory));
		
		addDisplay(fieldPlot);
		addDisplay(structurePlot);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeSpins();
			
			for (int i = 0; i < numSteps; i++) {
				structure.fn(sim.spins.getAll(), i);
				structure.theory(sim.time(), sim.R, sim.T);
				yield();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}

}	