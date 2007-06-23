package kip.md.apps;

import java.awt.Color;
import static java.lang.Math.*;

import kip.md.PointGrid2D;
import kip.util.*;
import scikit.dataset.DynamicArray;
import scikit.graphics.*;
import scikit.jobs.*;
import scikit.util.Bounds;
import static scikit.util.Utilities.format;
import org.opensourcephysics.numerics.*;

import delaunay.VoronoiGraphics;


public class CobbAnderson extends Simulation {
	private final double PARTICLES_PER_CELL = 1;
	private final double SIGMA_CUTOFF = 3;
	
	Canvas2D canvas = new Canvas2D("Particles");
	Random rand = new Random();
	
	int NA, NB;		// number of particles
	double L;		// system length
	// lennard jones parameters
	// V(r) = 4 eps [ (sig/r)^12 - (sig/r)^6 ]
	// where sigma is the sum of radii
	double RA, RB; // radius of particles
	double MA, MB; // mass of particles
	double epsilon;
	
	// complete state of configuration.  4N+1 elements: positions, velocities, time.
	// packed as: (x_1, vx_1, y_1, vy_1, ..., time)
	double[] phase;
	Verlet solver;
	PointGrid2D gridA, gridB;
	
	
	public CobbAnderson() {
		params.add("Length", 50.0);
		params.add("Density A", 0.15);
		params.add("Density B", 0.05);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.7);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.05);
		params.addm("Temperature", 1.0);
		params.add("Time");
	}
	
	
	public static void main(String[] args) {
		new Control(new CobbAnderson(), "Particle Simulation");
	}
	
	public void animate() {
		solver.setStepSize(params.fget("dt"));
		params.set("Time", format(phase[4*(NA+NB)]));
		
		double scale = pow(2, 1/6.);
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics particlesA = new Particle2DGraphics(scale*RA, bounds, Color.BLUE);
		particlesA.setPoints(phase, 2, 0, NA);
		Particle2DGraphics particlesB = new Particle2DGraphics(scale*RB, bounds, Color.GREEN);
		particlesB.setPoints(phase, 2, NA, NA+NB);
		
		VoronoiGraphics voronoi = new VoronoiGraphics(bounds);		
//		voronoi.setPoints(phase, 2, 0, NA);
		
		canvas.removeAllGraphics();
		canvas.addGraphics(particlesA);
		canvas.addGraphics(particlesB);
		canvas.addGraphics(voronoi);
	}
	
	public void run() {
		L = params.fget("Length");
		NA = (int) (params.fget("Density A")*L*L);
		NB = (int) (params.fget("Density B")*L*L);
		RA = params.fget("Radius A");
		RB = params.fget("Radius B");
		MA = PI*RA*RA;
		MB = PI*RB*RB;
		epsilon = params.fget("Epsilon");
		
		gridA = new PointGrid2D(L, (int)(sqrt(NA/PARTICLES_PER_CELL)));
		gridB = new PointGrid2D(L, (int)(sqrt(NB/PARTICLES_PER_CELL)));
		phase = new double[4*(NA+NB)+1];
		initializeParticles();
		
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
		
		rate[4*(NA+NB)] = 1;
	}
	
	
	private void calculateForces(double[] state, double[] rate) {
		gridA.setPoints(state, 2, 0, NA);
		gridB.setPoints(state, 2, NA, NA+NB);
		 
		for (int i = 0; i < NA+NB; i++) {
			double R = i < NA ? RA : RB;
			double M = i < NA ? MA : MB;
			
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			accumulateForces(i, state, rate, (R+RA), M, gridA);
			accumulateForces(i, state, rate, (R+RB), M, gridB);
		}
	}
	
	private void accumulateForces(int i, double[] state, double[] rate, double sigma, double M, PointGrid2D grid) {
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
				double LJ = 4*epsilon*(12*a12-6*a6)/(r*r);
				rate[4*i+1] -= dx*LJ/M; // accumulate force_x
				rate[4*i+3] -= dy*LJ/M; // accumulate force_y
			}
		}
	}
	
	private void initializeParticles() {
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
