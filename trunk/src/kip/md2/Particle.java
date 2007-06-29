package kip.md2;

import scikit.util.Point;


abstract public class Particle<T extends Particle<T>> extends Point {
	public double vx = 0, vy = 0, vz = 0;
	protected ParticleTag tag;
	
	public Particle(ParticleTag tag) {
		this.tag = tag;
	}
	
	abstract public void force(T that, double[] f);
	abstract public void force(double[] f);
	abstract public double potential(T that);
	abstract public double potential();
}
