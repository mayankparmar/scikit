package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


class Structure {
	public double[] data, theory;
	int N;
	jnt.FFT.RealDoubleFFT fft;
	
	public Structure(int _N) {
		N = _N;
		data = new double[N];
		theory = new double[N];
		fft = new jnt.FFT.RealDoubleFFT_Radix2(N);
	}
	
	public double[] fn(int[] spins) {
		for (int i = 0; i < N; i++) {
			data[i] = spins[i];
		}
		fft.transform(data);
		
		// data[0] = data[0]*data[0];
		data[0] = 0;
		for (int i = 1; i < N/2; i++) {
			data[i] = (data[i]*data[i] + data[N-i]*data[N-i]) / N;
		}
		for (int i = N/2; i < N; i++) {
			data[i] = 0;
		}
		return data;
	}
	
	public double[] theory(double t, double R, double T) {
		for (int i = 0; i < N/2; i++) {
			double k = 1e-10 + 2.0 * PI * i / N;
			double D = sin(k*R)/(T*k*R) - 1;
			double gamma = 2 - 8*t;
			double A = gamma * exp(-4*t*D) / (2*D);
			theory[i] = exp(4*t*D)*(1+A)-A;
		}
		return theory;
	}
}


public class OrderingApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Plot structurePlot = new Plot("Structure", true);
	Ising sim;
	Structure structure;

	public static void main(String[] args) {
		frame(new Control(new OrderingApp()), "Growth for Ising Droplets");
	}

	public OrderingApp() {
		params.add("Memory time", 20.0, true);
		params.add("Random seed", 0, true);
		params.add("N", 1<<20, true);
		params.add("R", 128, true);
		params.add("T", 4.0/9.0, false);
		params.add("h", 0.0, false);
		params.add("dt", 0.01, false);
		outputs.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		outputs.set("time", sim.time());
	}
	
	
	public void run() {
		sim = new Ising(params);
		structure = new Structure(sim.N);
		
		fieldPlot.setDataSet(0, new PointSet(0, sim.systemSize()/sim.ψ.length, sim.ψ));
		fieldPlot.setYRange(-1, 1);
		structurePlot.setDataSet(0, new YArray(structure.data));
		structurePlot.setDataSet(1, new YArray(structure.theory));
		
		addDisplay(fieldPlot);
		addDisplay(structurePlot);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeSpins();
			structure.fn(sim.spins.getAll());
			yield();
			
			while (sim.time() < 2) {
				sim.step();
				structure.fn(sim.spins.getAll());
				structure.theory(sim.time(), sim.R, sim.T);
				yield();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}

}	