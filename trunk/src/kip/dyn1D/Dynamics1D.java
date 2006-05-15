package kip.dyn1D;

import scikit.jobs.*;
import kip.util.Random;



abstract class Dynamics1D implements Cloneable {
	public double[]			ψ;
	public Random			random = new Random();
	private int[]			_ψnet; // helper array for spin systems
	private Dynamics1D		old;
	protected double		memoryTime = 1;
 		
	
	public Dynamics1D clone() {
        try {
            Dynamics1D c = (Dynamics1D)super.clone();
            c.ψ = (double[])ψ.clone();
            c.random = (Random)random.clone();
            return c;
        } catch (Exception e) {
            return null;
        }
	}
	
	
	public Dynamics1D simulationAtTime(double t) {
		if (time() > t) {
			if (old == null)
				throw new IllegalArgumentException();
			else
				return old.simulationAtTime(t);
		}
		else {
			assert(time() < t);
			Dynamics1D c = clone();
			_runUntil(t);
			return c;
		}
	}
	
	
	public void step() {
		if (old == null)
			old = clone();
		_step();
		if (time() - old.time() > memoryTime) {
			old = clone();
			old.old.old = null; // cut off history to avoid memory leaks
		}
	}
	
	
	abstract public void initialize(Parameters params);
	abstract public void setParameters(Parameters params);	
	abstract public double time();
	abstract protected void _step(); // step without saving state
	
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
	
	
	private double regionMagnetization(int x, int testLen) {
		double acc = 0;
		int L = ψ.length;
		for (int i = x-testLen; i <= x+testLen; i++)
			acc += ψ[(i+L)%L];
		return acc / (2*testLen+1);
	}
	
	
	private double testNucleationAtTime(double t, double testDt, int x, int testLen) {
		Dynamics1D sim = simulationAtTime(t);
		
		final int TRIALS = 100;
		double acc = 0;
		for (int i = 0; i < TRIALS; i++) {
			Dynamics1D c = sim.clone();
			c.random.setSeed(random.nextLong() + System.currentTimeMillis());			
			c._runUntil(sim.time()+testDt);
			acc += c.regionMagnetization(x, testLen);
		}
		
		return sim.regionMagnetization(x, testLen) - acc/TRIALS;
	}
	
	
	public double intervention(double testDt, int testLen) {
		if (!inGrowthMode())
			throw new IllegalArgumentException();
		
		double lo = time()-memoryTime;
		double hi = time();
		 
		int x = findGrowthCenter();
		
		while (hi - lo > testDt) {
			double t = (lo + hi) / 2;
			if (testNucleationAtTime(t, testDt, x, testLen) < 0)
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


