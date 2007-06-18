package kip.ising;

import static java.lang.Math.*;
import kip.util.Random;
import java.awt.Color;
import scikit.params.Parameters;
import scikit.graphics.*;
import scikit.dataset.*;
import scikit.jobs.*;


class LRIsing {
    Random random = new Random(0);

	public double J, h, t, dt;
	public int N, L, tN;	
	public double[] ψ;  // average of all spins within length L
	int[] ψnet;			// sum of all spins within length L
	int[] s;			// individual spins
	
	public LRIsing(Parameters params) {
		// get reasonable values for N and L
		N = params.iget("N");
		L = params.iget("L");
		N = max(min(N, Integer.highestOneBit(N)), 4);
		if (N % L != 0)
			L = N / 256;
		L = min(max(L, 1), N/4);
		assert N % L == 0;
		params.set("N", N);
		params.set("L", L);
		
		// allocate the arrays
		ψ		= new double[N / L];
		ψnet	= new int[N / L];
		s		= new int[N];
		
		// do standard field initialization
		initialize(params);
	}
	
	// set parameters which are allowed to change during the simulation
	public void setParameters(Parameters params) {
		J = params.fget("J");
		h = params.fget("h");
		dt = params.fget("dt");
	}
	
	// reset time, set random number seed, initialize fields to down
	public void initialize(Parameters params) {
		t = tN = 0;
		random.setSeed(params.iget("Random seed"));
		setParameters(params);
	
		for (int i = 0; i < N; i++) {
			s[i] = -1;
		}
		for (int i = 0; i < ψ.length; i++) {
			ψ[i]    = -1;
			ψnet[i] = -L;
		}
	}
	
	// has any part of the coarse grained field passed through 0?
	public boolean nucleated() {
//		for (int i = 0; i < ψ.length; i++)
//			if (ψ[i] > 0) return true;
		return false;
	}
	
/*
	int dist(int i, int j, int Length) {
		int d = abs(i - j);
		return (d < Length/2) ? d : Length - d;
	}
*/
	
	double avgFieldInRange(int i) {
		int j = i/L;
		int jm = (j - 1 + N/L) % (N/L);
		int jp = (j + 1) % (N/L);
		int sum = ψnet[jm] + ψnet[j] + ψnet[jp];

/*		
		// verify sum is correct
		int sum2 = 0;
		for (int i2 = 0; i2 < N; i2++) {
			if (dist(i/L, i2/L, N/L) <= 1)
				sum2 += s[i2];
		}
		assert sum == sum2;
*/
		
		return (sum-s[i])/(3.0*L-1.0);
	}
	
	void flipSpin(int i) {
		s[i] *= -1;
		ψnet[i/L] += 2*s[i];
		ψ[i/L] = (double)ψnet[i/L] / L;
	}
	
	// do N*dt spin flip trials, corresponding to (dt MCS)
	public void step() {
		for (int cnt = 0; cnt < N*dt; cnt++) {
			int i = random.nextInt(N);
			double dE = 2*s[i]*(h + J*avgFieldInRange(i));
			
			// glauber dynamics
//			double p = exp(-dE)/(1+exp(-dE));
//			if (random.nextDouble() < p)
//				flipSpin(i);
			
			// metropolis dynamics
			if (dE <= 0 || random.nextDouble() < Math.exp(-dE))
				flipSpin(i);
			
			tN++;
		}
		t = tN / N;
	}
}


public class LRIsingApp extends Simulation {
	Plot fieldPlot = new Plot("Fields");
//	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);	
	LRIsing sim;
	
	
	public static void main(String[] args) {
		new Control(new LRIsingApp(), "Long Range Ising");
	}
	
	public LRIsingApp() {
		params.addm("Random seed", 0);
		params.addm("Bin width", 0.5);
		params.add("N", 1 << 18);
		params.add("L", 1 << 10);
		params.addm("J", 0.5);
		params.addm("h", 0.5);
		params.addm("dt", 0.1);
	}
	
	
	public void animate() {
//		nucTimes.setBinWidth(2, params.fget("Bin width"));
		sim.setParameters(params);
	}
	
	void simulateUntilNucleation() {
		while (!sim.nucleated()) {
			sim.step();
			Job.animate();
		}
//		nucTimes.accum(2, sim.t);
	}
	
	public void run() {
		sim = new LRIsing(params);
		
		fieldPlot.addLines(0, new PointSet(0, sim.L, sim.ψ), Color.RED);
		fieldPlot.setYRange(-1, 1);
		
		Job.addDisplay(fieldPlot);
//		Job.addDisplay(nucTimes);		
		
		while (true) {
			sim.initialize(params);
			simulateUntilNucleation();
			params.set("Random seed", params.iget("Random seed")+1);
		}
	}
}
