package kip.md2;

import kip.util.Vec3;


public class LJParticle2D extends Particle<LJParticle2D> {
	static final double epsilon=1;
	

	public LJParticle2D(ParticleTag tag) {
		super(tag);
	}
	
	public void force(LJParticle2D that, Vec3 f) {
		_force(tag.md.displacement(this, that), tag.radius + that.tag.radius, f);
	}
	public void force(Vec3 f) {
		_force(tag.md.boundaryDistance(this), tag.radius, f);
	}
	public double potential(LJParticle2D that) {
		return _potential(tag.md.displacement(this, that), tag.radius + that.tag.radius);
	}
	public double potential() {
		return _potential(tag.md.boundaryDistance(this), tag.radius);
	}

	private double _potential(Vec3 d, double sigma) {
		if (d == null) {
			return 0;
		}
		else {
			double r = d.norm();
			double R = tag.interactionRange;
			double a = sigma/r;
			double b = sigma/R;
			double a6 = a*a*a*a*a*a;
			double b6 = b*b*b*b*b*b; 
			double a12 = a6*a6;
			double b12 = b6*b6;
			return 4*epsilon*(a12 - 2*a6 - b12 + 2*b6);
		}
	}
	
	private void _force(Vec3 d, double sigma, Vec3 f) {
		if (d == null) {
			f.x = f.y = f.z = 0;
		}
		else {
			double r = d.norm();
			double a = sigma/r;
			double a6 = a*a*a*a*a*a;
			double a12 = a6*a6;
			double fmag = -(12/r)*4*epsilon*(a12 - a6);
			f.x = fmag*d.x/r;
			f.y = fmag*d.y/r;
			f.z = fmag*d.z/r;
		}
	}
}
