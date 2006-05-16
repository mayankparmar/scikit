package kip.dyn1D;

import scikit.jobs.*;
import kip.ising.SpinBlocks1D;
import kip.util.Random;
import static java.lang.Math.*;


public class Ising extends Dynamics1D {
    public int N, R;
	public double h, dt, T, J;
	public int tN;
	SpinBlocks1D spins;
	
	
	public Ising(Parameters params) {
		
		N = params.iget("N");
		if (N > Integer.highestOneBit(N))
			N = Integer.highestOneBit(N);
		params.set("N", N);
		
		R = params.iget("R");
		if (2*R+1 >= N)
			R = N/2 - 1;
		params.set("R", R);
		
		memoryTime = params.fget("Memory time");
		J = 4.0 / (2*R);
		
		initialize(params);
	}
	
	
    public Ising clone() {
		Ising c = (Ising)super.clone();
		c.spins = (SpinBlocks1D)spins.clone();
		return c;
    }
	
	
	// set parameters which are allowed to change during the simulation
	public void setParameters(Parameters params) {
		T  = params.fget("T");
		h  = params.fget("h");
		dt = params.fget("dt");
	}
	
	
	// reset time, set random number seed, initialize fields to down
	public void initialize(Parameters params) {
		random.setSeed(params.iget("Random seed"));
		setParameters(params);
		
		tN = 0;
		spins = new SpinBlocks1D(N, R, -1);
		initializeField(256, spins.getAll());
	}
	
	
	public double time() {
		return (double)tN / N;
	}
	
	
	protected void _step() {
		for (int cnt = 0; cnt < N*dt; cnt++) {
			int i = random.nextInt(N);
			int spin = spins.get(i);
			double dE = 2*spin*(h + J*(spins.sumInRange(i)-spin));
			if (dE <= 0 || random.nextDouble() < Math.exp(-dE/T)) {
				spins.flip(i);
				spinFlippedInField(i, spins.getAll());
			}
			tN++;
		}
	
	}
	
}
