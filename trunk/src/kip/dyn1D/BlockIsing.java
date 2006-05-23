package kip.dyn1D;

import scikit.jobs.*;
import kip.ising.SpinBlocks1D;
import kip.util.Random;
import static java.lang.Math.*;


public class BlockIsing extends Dynamics1D {
    public int N, R;
	public double h, dt, T, J;
	public int tN;
	int[] spins;
	int[] blocks;
	
	public BlockIsing(Parameters params) {
		// get reasonable values for N and R
		N = params.iget("N");
		R = params.iget("R");
		if (N > Integer.highestOneBit(N))
			N = Integer.highestOneBit(N);
		if (N % R != 0)
			R = N / 256;
		R = min(max(R, 1), N/4);
		assert N % R == 0;
		params.set("N", N);
		params.set("R", R);
		
		memoryTime = params.fget("Memory time");
		J = 4.0 / (3*R);
		
		// allocate the arrays
		spins	= new int[N];
		blocks	= new int[N / R];
		
		// do standard initialization		
		initialize(params);
	}
	
	
    public BlockIsing clone() {
		BlockIsing c = (BlockIsing)super.clone();
		c.spins = spins.clone();
		c.blocks = blocks.clone();
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
		super.initialize(params);
		
		random.setSeed(params.iget("Random seed"));
		setParameters(params);
		
		tN = 0;
		for (int i = 0; i < N; i++)
			spins[i] = -1;
		for (int i = 0; i < blocks.length; i++)
			blocks[i] = -R;
		
		initializeField(64, spins);
	}
	
	public int systemSize() { return N; }
	public double time() { return (double)tN / N; }
	public double temperature() { return T; }
	public double externalField() { return h; }
	
/*
	int dist(int i, int j) {
		int d = abs(i - j);
		return (d < blocks.length/2) ? d : blocks.length - d;
	}
*/	
	
	int sumInRange(int i) {
		int L = blocks.length;
		int j = i/R;
		int sum = blocks[(j-1+L)%L] + blocks[j] + blocks[(j+1)%L];
/*
		// verify sum is correct
		int sum2 = 0;
		for (int i2 = 0; i2 < N; i2++) {
			if (dist(i/R, i2/R) <= 1)
				sum2 += spins[i2];
		}
		assert sum == sum2;
*/		
		return sum;
	}
	
	
	protected void _step() {
		for (int cnt = 0; cnt < N*dt; cnt++) {
			int i = random.nextInt(N);
			int spin = spins[i];
			double dE = 2*spin*(h + J*(sumInRange(i)-spin));
			if (dE <= 0 || random.nextDouble() < Math.exp(-dE/T)) {
				spins[i] = -spin;
				blocks[i/R] -= 2*spin;
				spinFlippedInField(i, spins);
			}
			tN++;
		}
	}
	
	
	public double[] langerDroplet(int center) {
		return new double[ψ.length];
	}
	
}
