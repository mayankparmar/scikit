package kip.ising;

import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.GridDisplay;
import kip.util.Complex;

class Ising {
	public static final double Tc = 2.0/Math.log(1.0+Math.sqrt(2.0));
	public int spin[];
	public int L;
	public int N;
	public double T;
	int[] stack;
	boolean[] cluster;
	
	
	public Ising(int _L, double _T) {
		L=_L;
		T = _T;
		N = L*L;
		spin = new int[N];
		stack = new int[N];
		cluster = new boolean[N]; 		
		for (int i = 0; i < N; i++)
			spin[i] = 1;
	}
	
	
	int neighbor(int i, int k) {
		int y = i/L;
		int x = i%L;
		int yp = (y + (k-1)%2 + L) % L;    // (k-1)%2 == {-1,  0, 1, 0}
		int xp = (x + (k-2)%2 + L) % L;    // (k-2)%2 == { 0, -1, 0, 1}
		return yp*L+xp;
	}
	
	
	public void step() {
		int nflipped = 0;
		while (nflipped < N/2) {
			double p = 1 - Math.exp(-2/T);

			stack[0] = (int)(Math.random()*N);
			cluster[stack[0]] = true;
			int stack_len = 1;

			while (stack_len-- > 0) {
				int i = stack[stack_len];
				for (int k = 0; k < 4; k++) {
					int ip = neighbor(i, k);
					if (!cluster[ip] && spin[ip] == spin[i] && Math.random() <= p) {
						cluster[ip] = true;
						stack[stack_len++] = ip;
					}
				}
				spin[i] *= -1;
				cluster[i] = false; // cannot be readded to the cluster since spin is now misaligned
				nflipped++;
			}
		}
		
		// if more than quarter of the spins have been flipped, then flip all system spins
		// for visual continuity
		int up = 0;
		for (int i = 0; i < N; i++)
			up += (spin[i]+1)/2;
		if (up < N/2) {
			for (int i = 0; i < N; i++)
				spin[i] *= -1;
		}
	}
}

public class Wolff2DApp extends Job {
    GridDisplay grid = new GridDisplay("Ising spins", true);
    GridDisplay grid2 = new GridDisplay("Conformal mapping", true);    
	Ising sim;
	
	public static void main(String[] args) {
		frame(new Control(new Wolff2DApp()), "Clump Model");
	}

	public Wolff2DApp() {
		params.add("L", 512);
		params.addm("T", Ising.Tc);
	}
	
	public void animate() {
		sim.T = params.fget("T");
	}
	
	public void run() {
		sim = new Ising(params.iget("L"), params.fget("T"));	
    	int L = sim.L;
    	int Lc = L/2;
         
        double[] original  = new double[L*L];
		double[] conformal = new double[Lc*Lc];
		grid.setData(L, L, original);
        grid2.setData(Lc, Lc, conformal);
        
        addDisplay(grid);
        addDisplay(grid2);
        
        while (true) {
        	sim.step();
	        for (int y = 0; y < L; y++) {
	        	for (int x = 0; x < L; x++) {
	        		original[y*L+x] = sim.spin[y*L+x];
        			if ((x/32+y/32)%2 == 0) {
        				if (original[y*L+x] == 1)
        					original[y*L+x] *= 0.7;
        			}
	        	}
	        }
	        for (int yc = 0; yc < Lc; yc++) {
	        	for (int xc = 0; xc < Lc; xc++) {
	        		Complex c = new Complex(xc-Lc/2,yc-Lc/2);
	        		c = c.sqrt().times(new Complex(L/Math.sqrt(Lc),0));
//	        		c = c.times(c).times(new Complex(2.4*L/(Lc*Lc),0));
	        		int x = (int)c.re() + L/2;
	        		int y = (int)c.im() + L/2;
	        		if (x >= 0 && x < L && y >= 0 && y < L)
	        			conformal[yc*Lc+xc] = original[y*L+x];
	        	}
	       	}
        	
        	yield();
        }
	}
}
