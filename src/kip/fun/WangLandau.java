package kip.fun;

import scikit.jobs.*;
import scikit.graphics.*;
import scikit.dataset.*;
import java.awt.Color;


public class WangLandau extends Simulation {
	Plot histogramPlot = new Plot("Histogram vs. Energy");
	Plot densityPlot = new Plot("Density of states vs Energy");
	Plot heatPlot = new Plot("Heat capacity vs Temperature");

	int mcs;
	int L, N;
	double density;	 // percentage of (spin 0) magnetic impurities
	double[] g;       // logarithm of the density of states (energy argument translated by 2N)
	int[] H;          // histogram (reduce f when it is "flat")
	int E;            // energy of current spin configuration (translated by 2N)
	int[] spin;
	double f;         // multiplicative modification factor to g
	int iterations;   // number of reductions to f


	public static void main (String[] args) {
		new Control(new WangLandau(), "Wang Landau");
	}

	public WangLandau() {
		params.add("L", 16);
		params.add("Impurity density", 0.2);
		params.add("mcs");
		params.add("iteration");
	}

	public void run() {
		L = params.iget("L");
		density = params.fget("Impurity density");

		mcs = 0;
		N = L*L;
		f = Math.exp(1);
		iterations = 0;

		spin = new int[N];
		for (int i = 0; i < N; i++) {
			spin[i] = Math.random() < 0.5 ? 1 : -1;
			if (Math.random() < density)
				spin[i] = 0;
		}

		g = new double[4*N + 1];
		H = new int   [4*N + 1];
		for (int e = 0; e <= 4*N; e++) {
			g[e] = 0;
			H[e] = 0;
		}

		E = 0;
		for (int i = 0; i < N; i++) {
			E += - spin[i] * sumNeighbors(i);
		}
		E /= 2;        // we double counted all interacting pairs
		E += 2*N;      // translate energy by 2*N to facilitate array access

		while (true) {
			doStep();
			Job.animate();
		}
	}


	int sumNeighbors(int i) {
		int u = i - L;
		int d = i + L;
		int l = i - 1;
		int r = i + 1;

		if (u < 0)        u += N;
		if (d >= N)       d -= N;
		if (i % L == 0)   l += L;
		if (r % L == 0)   r -= L;
		return spin[u] + spin[d] + spin[l] + spin[r];
	}

	void flipSpins() {
		for (int steps = 0; steps < N; steps++) {
			int i = (int) (N * Math.random());
			int dE = 2*spin[i]*sumNeighbors(i);

			if (Math.random() < Math.exp(g[E] - g[E + dE])) {
				spin[i] = -spin[i];
				E += dE;
			}

			g[E] += Math.log(f);
			H[E] += 1;
			
			Job.yield();
		}
	}

	boolean isFlat() {
		int netH = 0;
		double numEnergies = 0;

		for (int e = 0; e <= 4*N; e++) {
			if (H[e] > 0) {
				netH += H[e];
				numEnergies++;
			}
		}

		for (int e = 0; e <= 4*N; e++)
			if (0 < H[e] && H[e] < 0.8*netH/numEnergies)
				return false;

		return true;
	}


	void doStep() {
		int mcsMax = mcs + Math.max(100000/N, 1);
		for (; mcs < mcsMax; mcs++)
			flipSpins();

		if (isFlat()) {
			f = Math.sqrt(f);
			iterations++;
			for (int e = 0; e <= 4*N; e++)
				H[e] = 0;
		}
	}
	
	
	public void animate() {
		params.set("mcs", mcs);
		params.set("iteration", iterations);

		DynamicArray densityData = new DynamicArray();
		DynamicArray histogramData = new DynamicArray();
		Accumulator heatData = new Accumulator(0.02);
		heatData.setAveraging(true);

		for (int e = 0; e <= 4*N; e++) {
			if (g[e] > 0) {
				densityData.append2  (e - 2*N, g[e] - g[0]);
				histogramData.append2(e - 2*N, H[e]);
			}
		}
		
		for (double T = 0.5; T < 5; T += 0.1)
			heatData.accum(T, Thermodynamics.heatCapacity(N, g, 1/T));
		for (double T = 1.2; T < 1.8; T += 0.02)
			heatData.accum(T, Thermodynamics.heatCapacity(N, g, 1/T));
		
		
		densityPlot.resetViewWindow();
		densityPlot.animate(Plot.points(densityData, Color.BLUE));
		
		histogramPlot.animate(Plot.points(histogramData, Color.BLACK));
		
		heatPlot.resetViewWindow();
		heatPlot.animate(Plot.lines(heatData, Color.RED));
	}
	
	public void clear() {
		densityPlot.clear();
		histogramPlot.clear();
		heatPlot.clear();
	}
}

class Thermodynamics {
	static double logZ(int N, double[] g, double beta) {
		// m = max {e^(g - beta E)}
		double m = 0;
		for (int E = -2*N; E <= 2*N; E++)
			m = Math.max(m, g[E+2*N] - beta*E);

		//     s     = Sum {e^(g - beta E)} * e^(-m)
		// =>  s     = Z * e^(-m)
		// =>  log s = log Z - m
		double s = 0;
		for (int E = -2*N; E <= 2*N; E++)
			s += Math.exp(g[E+2*N] - beta*E - m);
		return Math.log(s) + m;
	}


	static double heatCapacity(int N, double[] g, double beta) {
		double logZ = logZ(N, g, beta);

		double E_avg = 0;
		double E2_avg = 0;

		for (int E = -2*N; E <= 2*N; E++) {
			if (g[E+2*N] == 0) continue;

			E_avg  += E   * Math.exp(g[E+2*N] - beta*E - logZ);
			E2_avg += E*E * Math.exp(g[E+2*N] - beta*E - logZ);
		}

		return (E2_avg - E_avg*E_avg) * beta*beta;
	}
}
