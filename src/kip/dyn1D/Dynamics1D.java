package kip.dyn1D;

import scikit.jobs.*;
import kip.util.Random;
import static java.lang.Math.*;


abstract public class Dynamics1D implements Cloneable {
	private Dynamics1D		old;
	private double			memoryTime;
	
	public Random			random = new Random();
	public int N, R, dx;
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
		time = 0;
		old = null;
		
		memoryTime = params.fget("Memory time", Double.POSITIVE_INFINITY);
		random.setSeed(params.iget("Random seed", 0));
		
		setParameters(params);
	}
	
	
	public void setParameters(Parameters params) {
		dt = params.fget("dt");
	}
	
	
	public double[] copyField() {
		double ret[] = new double[N/dx];
		for (int i = 0; i < N/dx; i++)
			ret[i] = fieldElement(i);
		return ret;
	}
	
	
	public boolean nucleated() {
		for (int i = 0; i < N/dx; i++)
			if (fieldElement(i) > 0)
				return true;
		return false;
	}
	
	
	abstract public double magnetization();
	abstract public void randomizeField(double m);
	abstract public double fieldElement(int i);
	abstract protected void _step(); // step without saving "old" sim copies
}


