package kip.dyn1D;

import scikit.jobs.*;
import kip.util.Random;



abstract class Dynamics1D implements Cloneable {
	public double[]			ψ;
	private int[]			_ψnet; // helper array for spin systems
	public Random			random = new Random();
	private Dynamics1D		old;
	protected double		memoryTime = 0;
	
	
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
			return (old == null) ? null : old.simulationAtTime(t);
		}
		else {
			assert(time() <= t);
			Dynamics1D c = clone();
			c.runUntil(t);
			return c;
		}
	}
	
	
	public void step() {
		/*
		if (old != null) {
			assert (time() >= old.time());
			if (old.old != null) {
				assert (old.time() >= old.old.time());
				assert (old.old.old == null);
			}
		}
		*/
		if (old == null)
			old = clone();
		_step();
		if (time() - old.time() > memoryTime) {
			old = clone();
			old.old.old = null; // cut off history to avoid memory leaks
		}
	}
	
	
	public void runUntil(double t) {
		while (time() < t)
			step();
	}
	
	
	public void initialize(Parameters params) {
		old = null;
	}
	
	abstract public void setParameters(Parameters params);	
	abstract public int systemSize();
	abstract public double time();
	abstract public double temperature();
	abstract public double externalField();
	abstract public double[] langerDroplet(int center);
	
	abstract protected void _step(); // step without saving "old" sim copies
	
	
	
	// ***********************************************************************************
	//  Intervention
	//
	
	final int ORIGIN = 0;
	final double testDt = 0.5;
	
	protected double[] rotate(double[] a, int x) {
		int L = a.length;
		double[] r = new double[L];
		for (int i = 0; i < a.length; i++) {
			r[i] = a[(i-x+L)%L];
		}
		return r;
	}
	
	protected int displacement(int x, int y) {
		int L = ψ.length;
		int d = y - x;
		if (d < L/2)
			d += L;
		if (d > L/2)
			d -= L;
		return d;
	}
	
	
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
		return ψ.length / 16;
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
	public double testNucleationAtTime(double t, int x, int testRadius) {
		Dynamics1D sim = simulationAtTime(t);
		int L = ψ.length;
		
		double magReference = sim.regionMagnetization(x, testRadius);
		if (magReference < sim.magnetization()) {
			System.out.println(t + " unenhanced");
			return +1;  // region is not even enhanced!
		}
		
		final int TRIALS = 100;
		
		double[] avg = new double[L];
		double acc = 0;
		
		for (int i = 0; i < TRIALS; i++) {
			Dynamics1D c = sim.clone();
			c.random.setSeed((long)(Math.random()*(1<<48)));
			while (c.time() < sim.time()+testDt)
				c.step();
			acc += c.regionMagnetization(x, testRadius);
			
			for (int j = 0; j < L; j++)
				avg[j] += c.ψ[j] / TRIALS;
		}
		
		Job.plot(0, rotate(sim.ψ, L/2-x));
		Job.plot(1, rotate(avg, L/2-x));
		Job.plot(2, langerDroplet(L/2));
		System.out.println("t " + t + " delta: " + (magReference - acc/TRIALS));
		
		return magReference - acc/TRIALS;
	}
	
	
	public double intervention() {
		if (!inGrowthMode())
			throw new IllegalArgumentException();
		
		double lo = Math.max(time()-memoryTime/2, 0);
		double hi = time();
		 
		int x = findGrowthCenter();
		int testRadius = findGrowthRadius(x);
		
		System.out.println("growth center " + x);
		
		while (hi - lo > testDt / 10) {
			double t = (lo + hi) / 2;
			double delta = testNucleationAtTime(t, x, testRadius);
			Job.yield();
			
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


