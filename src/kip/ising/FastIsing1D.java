package kip.ising;

import kip.util.Random;
import org.opensourcephysics.controls.*;


public class FastIsing1D implements Cloneable {
    public int L, R;
	public double h, dt, T, J;
	public int tL;
    public int randomSeed;
	SpinBlocks1D spins;
    Random random = new Random(0);
	
	
    public Object clone() {
        try {
            FastIsing1D c = (FastIsing1D)super.clone();
            c.spins = (SpinBlocks1D)spins.clone();
            c.random = (Random)random.clone();
            return c;
        } catch (Exception e) {
            return null;
        }
    }
	
	
	public void initialize(SimControl control) {
		L = control.getInt("L");
		if (L > Integer.highestOneBit(L))
			L = Integer.highestOneBit(L);
		control.setValue("L", L);
		
		R = control.getInt("R");
		if (2*R+1 >= L)
			R = L/2 - 1;
		control.setValue("R", R);
		
        randomSeed = control.getInt("Random seed");
        random.setSeed(randomSeed);
		
		T = control.getDouble("T");
		h = control.getDouble("h");	
		dt = control.getDouble("dt");
		
		tL = 0;
		int spinsInRange = 2*R;
		J = 2.0 / spinsInRange;
		spins = new SpinBlocks1D(L, R, 1);
	}
	
	
    public void getParameters(SimControl control) {
        R = control.getInt("R");
        h = control.getDouble("h");
		T = control.getDouble("T");
		dt = control.getDouble("dt");
    }
	
	
	public void step() {
		for (int cnt = 0; cnt < L*dt; cnt++) {
			int i = random.nextInt(L);
			int spin = spins.get(i);
			double dE = 2*spin*(h + J*(spins.sumInRange(i)-spin));
			if (dE <= 0 || random.nextDouble() < Math.exp(-dE/T)) {
				spins.flip(i);
			}
			tL++;
		}
	
	}
	
	
	public double time() {
		return (double)tL / L;
	}
	
	
	public int length() {
		return L;
	}
	
	
	public int[] spins() {
		return spins.getAll();
	}
	
    // choose a new random number seed
    public void perturb() {
        random = new Random();
    }
	
}
