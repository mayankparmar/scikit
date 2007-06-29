package kip.md2;

import scikit.util.Point;


abstract public class Particle<S extends ParticleTag, T extends Particle<S, T>> extends Point {
	public double vx = 0, vy = 0, vz = 0;
	protected S tag;
	
	public Particle(S tag) {
		this.tag = tag;
	}
	
	abstract public double[] force(T that);
	abstract public double[] force();
	abstract public double potential(T that);
	abstract public double potential();
}
