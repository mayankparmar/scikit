package kip.dyn1D;

import scikit.jobs.*;
import kip.util.Random;



abstract class Dynamics1D implements Cloneable {
	public double[]			ψ;
	private int[]			_ψnet; // helper array for spin systems
	public Random			random = new Random();
	private Dynamics1D		old;
	protected double		memoryTime = 1;
 		
	
	public Dynamics1D clone() {
        try {
            Dynamics1D c = (Dynamics1D)super.clone();
            c.ψ = ψ.clone();
			if (_ψnet != null)
				c._ψnet = _ψnet.clone();
            c.random = (Random)random.clone();
            return c;
        } catch (Exception e) {
            return null;
        }
	}
	
	
	public Dynamics1D simulationAtTime(double t) {
		if (t < time()) {
			if (old == null)
				throw new IllegalArgumentException();
			else
				return old.simulationAtTime(t);
		}
		else {
			assert(time() <= t);
			Dynamics1D c = clone();
			c._runUntil(t);
			return c;
		}
	}
	
	
	public void step() {
		if (old != null) {
			assert (time() >= old.time());
			if (old.old != null) {
				assert (old.time() >= old.old.time());
				assert (old.old.old == null);
			}
		}
		
		if (old == null)
			old = clone();
		_step();
		if (time() - old.time() > memoryTime) {
			old = clone();
			old.old.old = null; // cut off history to avoid memory leaks
		}
	}
	
	
	public void initialize(Parameters params) {
		old = null;
	}
	
	
	abstract public void setParameters(Parameters params);	
	abstract public double time();
	abstract protected void _step(); // step without saving "old" sim copies
	
	// run without saving state
	private void _runUntil(double t) {
		while (time() < t)
			_step();
	}
	
	
	// ***********************************************************************************
	//  Intervention
	//
	
	final int ORIGIN = 0;
	
	// in growth mode if field crosses origin; assume field was in a negative metastable well
	public boolean inGrowthMode() {
		for (int i = 0; i < ψ.length; i++)
			if (ψ[i] >= ORIGIN)
				return true;
		return false;
	}
	
	
	private int findGrowthCenter() {
		assert(inGrowthMode());
		
		int loc = 0; // guaranteed less than i; can be negative
		int cnt = 0; // number of accumulations into loc
		int L = ψ.length;
		
		for (int i = 0; i < L; i++) {
			if (ψ[i] >= ORIGIN) {
				if (cnt == 0)
					loc = i;
				else
					loc += (i - loc/cnt < L/2) ? i : i - L;
				cnt++;
			}
		}
		return (loc/cnt + L) % L;
	}
	
	
	private int findGrowthRadius(int x) {
		// possibilities for improvement:
		// a) dynamically find width
		// b) feed in information about known solution
		return ψ.length / 32;
	}
	
	
	private double magnetization() {
		double acc = 0;
		for (int i = 0; i < ψ.length; i++)
			acc += ψ[i];
		return acc / ψ.length;
	}
	
	
	private double regionMagnetization(int x, int testRadius) {
		double acc = 0;
		int L = ψ.length;
		for (int i = x-testRadius; i <= x+testRadius; i++)
			acc += ψ[(i+L)%L];
		return acc / (2*testRadius+1);
	}
	
	
	// negative number means growth; nucleation occurred earlier
	// positive number means loss; nucleation occurred later
	private double testNucleationAtTime(double t, double testDt, int x, int testRadius) {
		Dynamics1D sim = simulationAtTime(t);
		
		System.out.println("plotting");
		Job.plot(0, sim.ψ);
		
		double magReference = sim.regionMagnetization(x, testRadius);
		if (magReference < magnetization())
			return +1;  // region is not even enhanced!
		
		final int TRIALS = 100;
		double acc = 0;
		for (int i = 0; i < TRIALS; i++) {
			Dynamics1D c = sim.clone();
			c.random.setSeed((long)(Math.random()*(1<<48)));
			c._runUntil(sim.time()+testDt);
			acc += c.regionMagnetization(x, testRadius);
		}
		
		return magReference - acc/TRIALS;
	}
	
	
	public double intervention(double testDt) {
		if (!inGrowthMode())
			throw new IllegalArgumentException();
		
		double lo = time()-memoryTime;
		double hi = time();
		 
		int x = findGrowthCenter();
		int testRadius = findGrowthRadius(x);
		
		while (hi - lo > testDt) {
			double t = (lo + hi) / 2;
			System.out.println("t " + t);
			double delta = testNucleationAtTime(t, testDt, x, testRadius);
			System.out.println("delta " + delta);
			if (delta < 0)
				hi = t;
			else
				lo = t;
		}
		return (lo + hi) / 2;
	}


	// ***********************************************************************************
	//  Helper functions for spin systems
	//
	
	public void initializeField(int N, int[] spins) {
		N = Math.min(N, spins.length);
		if (ψ == null || ψ.length != N) {
			ψ = new double[N];
			_ψnet = new int[N];
		}
		
		int spinsPerBlock = spins.length / ψ.length;
		
		for (int i = 0; i < _ψnet.length; i++) {
			_ψnet[i] = 0;
		}
		for (int i = 0; i < spins.length; i++) {
			_ψnet[i/spinsPerBlock] += spins[i];
		}
		for (int i = 0; i < ψ.length; i++) {
			ψ[i] = (double)_ψnet[i] / spinsPerBlock;
		}
	}
	
	public void spinFlippedInField(int i, int[] spins) {
		int spinsPerBlock = spins.length / ψ.length;
		int j = i / spinsPerBlock;
		_ψnet[j] += 2*spins[i];
		ψ[j]      = (double)_ψnet[j] / spinsPerBlock;
	}
	
}


