package kip.md2;

import static java.lang.Math.sqrt;


public class LJParticle2D extends Particle<LJParticle2D> {
	
	public LJParticle2D(ParticleTag tag) {
		super(tag);
	}
	
	double[] offset = new double[3];
	
	public void force(LJParticle2D that, double[] f) {
		tag.md.displacement(this, that, offset);
		_force(offset, tag.radius + that.tag.radius, f);
	}
	public void force(double[] f) {
		if (tag.md.inDisk) {
			tag.md.boundaryDistance(this, offset);
			_force(offset, tag.radius, f);
		}
	}
	public double potential(LJParticle2D that) {
		tag.md.displacement(this, that, offset);
		return _potential(offset, tag.radius + that.tag.radius);
	}
	public double potential() {
		if (tag.md.inDisk) {
			tag.md.boundaryDistance(this, offset);
			return _potential(offset, tag.radius);
		}
		return 0;
	}

	private double _potential(double[] d, double sigma) {
		double epsilon=1; // FIXME

		double r = sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
		double a = sigma/r;
		double a6 = a*a*a*a*a*a;
		double a12 = a6*a6;
		return 4*epsilon*(a12 - 2*a6);			
	}
	
	private void _force(double[] d, double sigma, double[] f) {
		double epsilon=1;

		double r = sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
		double a = sigma/r;
		double a6 = a*a*a*a*a*a;
		double a12 = a6*a6;
		double fmag = -(12/r)*4*epsilon*(a12 - a6);
		f[0] = fmag*d[0]/r;
		f[1] = fmag*d[1]/r;
		f[2] = fmag*d[2]/r;
	}
}
