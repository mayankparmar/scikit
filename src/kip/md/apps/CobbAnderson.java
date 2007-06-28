package kip.md.apps;

import geometry.VoronoiGraphics;

import java.awt.Color;
import static java.lang.Math.*;

import kip.md.MolecularDynamics2D;
import scikit.graphics.*;
import scikit.jobs.*;
import scikit.params.ChoiceValue;
import scikit.util.Bounds;
import static scikit.util.Utilities.format;



public class CobbAnderson extends Simulation {
	private final double DENSITY = 1;
	
	Canvas2D canvas = new Canvas2D("Particles");
	Cobb sim;
	boolean inDisk;
	int NA, NB;		// number of particles
	double L;		// system length
	double RA, RB; 	// radius of particles
	double epsilon;

	public CobbAnderson() {
		params.add("Topology", new ChoiceValue("Torus", "Disk"));
		params.add("Length", 50.0);
		params.add("Area fraction A", 0.5);
		params.add("Area fraction B", 0.1);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.7);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.02);
		params.addm("Temperature", 2);
		params.addm("Bath coupling", 0.2);
		params.add("Time");
	}
	
	
	public static void main(String[] args) {
		new Control(new CobbAnderson(), "Particle Simulation");
	}
	
	public void animate() {
		sim.setStepSize(params.fget("dt"));
		sim.setTemperature(params.fget("Temperature"), params.fget("Bath coupling"));
		params.set("Time", format(sim.time()));
		
		VoronoiGraphics voronoi = new VoronoiGraphics(new Bounds(0, L, 0, L));		
//		voronoi.construct(phase, 2, 0, NA+NB);
		
		canvas.removeAllGraphics();
		canvas.addGraphics(sim.getGraphics(0, RA, new Color(0f, 0f, 1f, 0.5f)));
		canvas.addGraphics(sim.getGraphics(1, RB, new Color(0f, 1f, 0f, 0.5f)));
		canvas.addGraphics(inDisk ? new CircleGraphics(L/2., L/2., L/2.) : new RectangleGraphics(0., 0., L, L));
		canvas.addGraphics(voronoi);
	}
	
	public void run() {
		Job.addDisplay(canvas);
		
		inDisk = params.sget("Topology").equals("Disk");
		L = params.fget("Length");
		double systemArea = inDisk ? (PI*(L/2.)*(L/2.)) : L*L;
		RA = params.fget("Radius A");
		RB = params.fget("Radius B");
		double particleAreaA = PI*RA*RA;
		double particleAreaB = PI*RB*RB;
		NA = (int) (params.fget("Area fraction A")*systemArea/particleAreaA);
		NB = (int) (params.fget("Area fraction B")*systemArea/particleAreaB);
		epsilon = params.fget("Epsilon");
		
		sim = new Cobb(L, 2*max(RA, RB));
		sim.addParticleType(NA, DENSITY*particleAreaA);
		sim.addParticleType(NB, DENSITY*particleAreaB);
		sim.initialize(params.fget("dt"));
		
		while (true) {
			sim.step();
			Job.animate();
		}
	}

	class Cobb extends MolecularDynamics2D {
		public Cobb(double L, double interactionRange) {
			super(L, interactionRange);
		}
		
		public double[] getExternalForce(int type, double x, double y) {
			if (inDisk) {
				double R = type == 0 ? RA : RB;
				double boundaryRadius = L/2.;
				double xc = x - L/2.;
				double yc = y - L/2.;
				double distanceFromCenter = sqrt(xc*xc + yc*yc);
				double dx = xc*(boundaryRadius/distanceFromCenter - 1);
				double dy = yc*(boundaryRadius/distanceFromCenter - 1);
				double r = boundaryRadius - distanceFromCenter;
				double force = lennardJonesForce(R, r); 
				return new double[] {(dx/r)*force/r, (dy/r)*force};
			}
			else {
				return new double[] {0, 0};
			}
		}

		public double getPairwiseForce(int type1, int type2, double r) {
			double R1 = type1 == 0 ? RA : RB;
			double R2 = type2 == 0 ? RA : RB;
			return lennardJonesForce(R1+R2, r);
		}

		public double[] getParticlePositions() {
			return inDisk ? getParticlePositionsInDisk() : getParticlePositionsInSquare(); 
		}
		
		public double[] phase() {
			return phase;
		}
		
		// lennard jones, uses an unusual convention for sigma:
		// V(r) = 4 eps [ (sig/r)^12 - 2 (sig/r)^6 ]
		// where sigma is the sum of two particle radii
		private double lennardJonesForce(double sigma, double r) {
			double a = sigma/r;
			double a6 = a*a*a*a*a*a;
			double a12 = a6*a6;
			return - (12/r)*4*epsilon*(a12 - a6);
		}		
	}
}
