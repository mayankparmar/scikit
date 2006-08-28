package kip.dyn1D;

import scikit.jobs.*;
import kip.util.Random;



abstract class Dynamics1D implements Cloneable {
	public Random			random = new Random();
	private Dynamics1D		old;
	protected double		memoryTime = Double.POSITIVE_INFINITY;
	
	public int blocklen;
	public int N, R;
	public double T, J=1, h=0;
	public double time, dt;
	
	public Dynamics1D clone() {
        try {
            Dynamics1D c = (Dynamics1D)super.clone();
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
		time += dt; // BUG: some error here
		if (time() - old.time() > memoryTime) {
			old = clone();
			old.old.old = null; // cut off history to avoid memory leaks
		}
	}
	
	
	public void runUntil(double t) {
		while (time() < t)
			step();
	}
	
	
	public double time() {
		return time;
	}
	
	public void initialize(Parameters params) {
		N = params.iget("N");
		if (N > Integer.highestOneBit(N))
			N = Integer.highestOneBit(N);
		params.set("N", N);
		
		R = params.iget("R");
		if (2*R+1 >= N)
			R = N/2 - 1;
		params.set("R", R);
		
		try { memoryTime = params.fget("Memory time"); } catch(Exception e) {}
		random.setSeed(params.iget("Random seed"));
		setParameters(params);
		
		time = 0;
		old = null;
	}
	
	public void setParameters(Parameters params) {
		T  = params.fget("T");
		try { J  = params.fget("J"); } catch(Exception e) {}
		try { h  = params.fget("h"); } catch(Exception e) {}
		dt = params.fget("dt");	
	}
	
	abstract public double magnetization();
	abstract public void randomizeField(double m);
	abstract public double[] copyField(double[] field);	
	abstract protected void _step(); // step without saving "old" sim copies
}


