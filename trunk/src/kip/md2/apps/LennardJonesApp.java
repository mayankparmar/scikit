package kip.md2.apps;

import static java.lang.Math.sqrt;
import scikit.util.Point;
import static scikit.util.Utilities.*;
import kip.md2.Particle;
import kip.md2.ParticleTag;

public class LennardJonesApp {
	double _L;
	double _interactionRange;
	boolean inDisk;
	
	class LJTag extends ParticleTag {
		public double interactionRange() {
			return _interactionRange;
		}
		
		public double[] displacement(Point p1, Point p2) {
			return new double[] {periodicOffset(_L, p1.x-p2.x), periodicOffset(_L, p1.y-p2.y)};
		}
		
		public double[] boundaryDistance(Point p) {
			if (inDisk) {
				double boundaryRadius = _L/2.;
				double xc = p.x - _L/2.;
				double yc = p.y - _L/2.;
				double distanceFromCenter = sqrt(xc*xc + yc*yc);
				double dx = xc*(boundaryRadius/distanceFromCenter - 1);
				double dy = yc*(boundaryRadius/distanceFromCenter - 1);
				return new double[] {dx, dy};
			}
			return null;
		}
	}
}



class LJParticle2D extends Particle<ParticleTag, LJParticle2D> {
	
	public LJParticle2D(ParticleTag tag) {
		super(tag);
	}
	
	public double[] force(LJParticle2D that) {
		return displacementToForce(tag.displacement(this, that));
	}
	public double[] force() {
		return displacementToForce(tag.boundaryDistance(this));
	}
	public double potential(LJParticle2D that) {
		return displacementToPotential(tag.displacement(this, that));
	}
	public double potential() {
		return displacementToPotential(tag.boundaryDistance(this));		
	}

	private double displacementToPotential(double[] d) {
		double r = sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
		return 1/r;
	}
	
	private double[] displacementToForce(double[] d) {
		double r = sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
		double force = -1/(r*r);
		return new double[] {d[0]*force/r, d[1]*force/r, d[2]*force/r};
	}
}
