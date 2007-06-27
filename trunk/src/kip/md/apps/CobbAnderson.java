package kip.md.apps;

import geometry.VoronoiGraphics;

import java.awt.Color;
import static java.lang.Math.*;

import kip.md.PointGrid2D;
import kip.util.*;
import scikit.dataset.DynamicArray;
import scikit.graphics.*;
import scikit.jobs.*;
import scikit.params.ChoiceValue;
import scikit.util.Bounds;
import static scikit.util.Utilities.format;
import org.opensourcephysics.numerics.*;



public class CobbAnderson extends Simulation {
	private final double PARTICLES_PER_CELL = 1;
	private final double SIGMA_CUTOFF = 3;
	private final double DENSITY = 1;
	
	Canvas2D canvas = new Canvas2D("Particles");
	Random rand = new Random();
	
	boolean onDisk;
	int NA, NB;		// number of particles
	double L;		// system length
	// lennard jones, uses an unusual convention for sigma:
	// V(r) = 4 eps [ (sig/r)^12 - 2 (sig/r)^6 ]
	// where sigma is the sum of two particle radii
	double RA, RB; // radius of particles
	double MA, MB; // mass of particles
	double epsilon;
	double T;
	double Q;
	
	// complete state of configuration.  4N+1 elements: positions, velocities, time.
	// packed as: (x_1, vx_1, y_1, vy_1, ..., time)
	double[] phase;
	int timeOffset, gammaOffset;
	
	Verlet solver;
	PointGrid2D gridA, gridB;
	
	
	public CobbAnderson() {
		params.add("Topology", new ChoiceValue("Torus", "Disk"));
		params.add("Length", 50.0);
		params.add("Area fraction A", 0.5);
		params.add("Area fraction B", 0.1);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.7);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.05);
		params.addm("Temperature", 0.3);
		params.addm("Bath coupling", 0.1);
		params.add("Time");
		params.add("Gamma");
	}
	
	
	public static void main(String[] args) {
		new Control(new CobbAnderson(), "Particle Simulation");
	}
	
	public void animate() {
		solver.setStepSize(params.fget("dt"));
		T = params.fget("Temperature");
		Q = params.fget("Bath coupling");
		params.set("Time", format(phase[timeOffset]));
		params.set("Gamma", format(phase[gammaOffset]));
		
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics particlesA = new Particle2DGraphics(RA, bounds, Color.BLUE);
		particlesA.setPoints(phase, 2, 0, NA);
		Particle2DGraphics particlesB = new Particle2DGraphics(RB, bounds, Color.GREEN);
		particlesB.setPoints(phase, 2, NA, NA+NB);
		
		VoronoiGraphics voronoi = new VoronoiGraphics(bounds);		
//		voronoi.construct(phase, 2, 0, NA+NB);
		
		canvas.removeAllGraphics();
		canvas.addGraphics(particlesA);
		canvas.addGraphics(particlesB);
		canvas.addGraphics(onDisk ? new CircleGraphics(L/2., L/2., L/2.) : new RectangleGraphics(0., 0., L, L));
		canvas.addGraphics(voronoi);
	}
	
	public void run() {
		onDisk = params.sget("Topology").equals("Disk");
		L = params.fget("Length");
		double systemArea = onDisk ? (PI*(L/2.)*(L/2.)) : L*L;
		RA = params.fget("Radius A");
		RB = params.fget("Radius B");
		double particleAreaA = PI*RA*RA;
		double particleAreaB = PI*RB*RB;
		MA = DENSITY*particleAreaA;
		MB = DENSITY*particleAreaB;
		NA = (int) (params.fget("Area fraction A")*systemArea/particleAreaA);
		NB = (int) (params.fget("Area fraction B")*systemArea/particleAreaB);
		epsilon = params.fget("Epsilon");
		T = params.fget("Temperature");
		Q = params.fget("Bath coupling");
		
		gridA = new PointGrid2D(L, (int)(sqrt(NA/PARTICLES_PER_CELL)));
		gridB = new PointGrid2D(L, (int)(sqrt(NB/PARTICLES_PER_CELL)));
		gridA.usePeriodicBoundaryConditions(!onDisk);
		gridB.usePeriodicBoundaryConditions(!onDisk);
		phase = new double[4*(NA+NB)+2];
		if (onDisk) initializeParticlesInDisk(); else initializeParticlesInSquare();
		
		timeOffset  = 4*(NA+NB)+0;
		gammaOffset = 4*(NA+NB)+1;
		phase[timeOffset] = 0;
		phase[gammaOffset] = 0;
		
		Job.addDisplay(canvas);
		
		ODE ode = new ODE() {
			public void getRate(double[] state, double[] rate) {
				CobbAnderson.this.getRate(state, rate);
			}
			public double[] getState() {
				return phase;
			}
		};
		
		solver = new Verlet(ode, 4*(NA+NB));
		solver.initialize(params.fget("dt"));
		while (true) {
			solver.step();
			correctBounds();
			Job.animate();
		}
	}
	
	private void getRate(double[] state, double[] rate) {
		for (int i = 0; i < NA+NB; i++) {
			// set dx/dt = v
			rate[4*i+0] = state[4*i+1];
			rate[4*i+2] = state[4*i+3];
		}
		
		if (solver.getRateCounter() == 1)
			calculateForces(state, rate);
		
		rate[timeOffset] = 1;
		rate[gammaOffset] = Q*(kineticEnergy() - (2/2)*(NA+NB)*T)/(NA+NB);
	}
	
	
	private double kineticEnergy() {
		double K = 0;
		for (int i = 0; i < NA+NB; i++) {
			double M = i < NA ? MA : MB;
			double vx = phase[4*i+1];
			double vy = phase[4*i+3];
			K += 0.5*M*(vx*vx+vy*vy);
		}
		return K;
	}
	
	
	private void calculateForces(double[] state, double[] rate) {
		gridA.setPoints(state, 2, 0, NA);
		gridB.setPoints(state, 2, NA, NA+NB);
		 
		for (int i = 0; i < NA+NB; i++) {
			double R = i < NA ? RA : RB;
			double M = i < NA ? MA : MB;
			
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			accumulatePairwiseForces(i, state, rate, (R+RA), M, gridA);
			accumulatePairwiseForces(i, state, rate, (R+RB), M, gridB);
			if (onDisk)
				accumulateBoundaryForces(i, state, rate, R, M);
			
			rate[4*i+1] -= phase[gammaOffset]*state[4*i+1]; // nose-hoover drag term
			rate[4*i+3] -= phase[gammaOffset]*state[4*i+3];
		}
	}
	
	private void accumulatePairwiseForces(int i, double[] state, double[] rate, double sigma, double M, PointGrid2D grid) {
		double x = state[4*i+0];
		double y = state[4*i+2];		
		DynamicArray ns = grid.pointOffsetsWithinRange(x, y, sigma*SIGMA_CUTOFF);
		
		for (int j = 0; j < ns.size()/2; j++) {
			double dx = ns.get(2*j+0);
			double dy = ns.get(2*j+1);
			double r = sqrt(dx*dx + dy*dy);
			if (0 < r && r < sigma*SIGMA_CUTOFF) {
				double a = sigma/r;
				double a3 = a*a*a;
				double a6 = a3*a3;
				double a12 = a6*a6;
				double LJ = 4*epsilon*12*(a12-a6)/(r*r);
				rate[4*i+1] -= dx*LJ/M; // accumulate force_x
				rate[4*i+3] -= dy*LJ/M; // accumulate force_y
			}
		}
	}
	
	private void accumulateBoundaryForces(int i, double[] state, double[] rate, double sigma, double M) {
		double boundaryRadius = L/2.;
		double xc = state[4*i+0] - L/2.;
		double yc = state[4*i+2] - L/2.;
		double distanceFromCenter = sqrt(xc*xc + yc*yc);
		double dx = xc*(boundaryRadius/distanceFromCenter - 1);
		double dy = yc*(boundaryRadius/distanceFromCenter - 1);
		double r = boundaryRadius - distanceFromCenter;
		if (r < sigma*SIGMA_CUTOFF) {
			double a = sigma/r;
			double a3 = a*a*a;
			double a6 = a3*a3;
			double a12 = a6*a6;
			double LJ = 4*epsilon*12*(a12-a6)/(r*r);
			rate[4*i+1] -= dx*LJ/M; // accumulate force_x
			rate[4*i+3] -= dy*LJ/M; // accumulate force_y
		}
	}
	
	private void initializeParticlesInSquare() {
		int rootN = (int)ceil(sqrt(NA+NB));
		double dx = L/rootN;
		int cntA = 0, cntB = 0;
		for (int i = 0; i < NA+NB; i++) {
			double x = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			double y = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			if (cntA*NB <= cntB*NA && NA != 0)
				initializeParticle(cntA++, x, y);
			else
				initializeParticle(NA+cntB++, x, y);
		}
	}
	
	private void initializeParticlesInDisk() {
		double R = L/2. - max(RA, RB);
		int N = NA+NB;
		int cntA = 0, cntB = 0;
		for (int i = 0; i < N; i++) {
			// these expressions for radius and angle were derived to give a spiral trajectory
			// in which the radial distance between loops has a fixed length, and the "velocity"
			// is constant. here the particle number, i, plays the role of "time".
			// it is somewhat surprising that the angle a(t) is independent of the bounding radius R
			// and total particle number N.
			double r = R * sqrt((double)(i+1) / N);
			double a = 2 * sqrt(PI * (i+1));
			
			double x = L/2. + r*cos(a);
			double y = L/2. + r*sin(a);
			if (cntA*NB <= cntB*NA && NA != 0)
				initializeParticle(cntA++, x, y);
			else
				initializeParticle(NA+cntB++, x, y);			
		}
	}
	
	private void initializeParticle(int i, double x, double y) {
		phase[4*i+0] = x;
		phase[4*i+1] = 0;
		phase[4*i+2] = y;
		phase[4*i+3] = 0;
	}
	
	private void correctBounds() {
		for (int i = 0; i < NA+NB; i++) {
			phase[4*i+0] = (phase[4*i+0]+L)%L;
			phase[4*i+2] = (phase[4*i+2]+L)%L;
		}
	}
}
