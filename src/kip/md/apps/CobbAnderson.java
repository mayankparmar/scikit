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
	
	int N;			// number of particles
	double L;		// system length
	// lennard jones parameters
	// V = 4 eps [ (sig/r)^12 - (sig/r)^6 ]
	double sigma;
	double epsilon;
	
	// complete state of configuration.  4N+1 elements: positions, velocities, time.
	// packed as: (x_1, vx_1, y_1, vy_1, ..., time)
	double[] phase;
	Verlet solver;
	PointGrid2D grid;

	
	
	public CobbAnderson() {
		params.add("Length", 20.0);
		params.add("Density A", 0.8);
		params.add("Sigma A", 1.0);
		params.add("Epsilon A", 1.0);
		params.add("Mass A", 1.0);
		params.add("dt", 0.01);
		params.add("time");
	}
	
	
	public static void main(String[] args) {
		new Control(new CobbAnderson(), "Particle Simulation");
	}
	
	public void animate() {
		params.set("time", format(phase[4*N]));
		
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics particles = new Particle2DGraphics(0.2, bounds, Color.BLUE);
		particles.setPoints(phase, 2, 0, N);
		VoronoiGraphics voronoi = new VoronoiGraphics(bounds);		
//		voronoi.setPoints(phase, 2, 0, N);
		
		canvas.removeAllGraphics();
		canvas.addGraphics(particles);
		canvas.addGraphics(voronoi);
	}
	
	public void run() {
		L = params.fget("Length");
		N = (int) (params.fget("Density A")*L*L);
		sigma = params.fget("Sigma A");
		epsilon = params.fget("Epsilon A");
		
		grid = new PointGrid2D(L, (int)(sqrt(N/PARTICLES_PER_CELL)));
		phase = new double[4*N+1];
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
		
		solver = new Verlet(ode);
		solver.initialize(params.fget("dt"));
		while (true) {
			solver.step();
			correctBounds();
			Job.animate();
		}
	}
	
	private void getRate(double[] state, double[] rate) {
		for (int i = 0; i < N; i++) {
			// set dx/dt = v
			rate[4*i+0] = state[4*i+1];
			rate[4*i+2] = state[4*i+3];
		}
		
		if (solver.getRateCounter() == 1)
			calculateForces(state, rate);
		
		rate[4*N] = 1;
	}
	
	
	private void calculateForces(double[] state, double[] rate) {
		grid.setPoints(state, 2, 0, N);
		
		for (int i = 0; i < N; i++) {
			double fx = 0;
			double fy = 0;
			double x = phase[4*i+0];
			double y = phase[4*i+2];
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
					double A = 4*epsilon*(12*a12-6*a6)/(r*r);
					fx -= dx*A;
					fy -= dy*A;
				}
				rate[4*i+1] = fx;
				rate[4*i+3] = fy;
			}
		}
	}
	
	private void initializeParticles() {
		int rootN = (int)ceil(sqrt(N));
		double dx = L/rootN;
		for (int i = 0; i < N; i++) {
			phase[4*i+0] = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			phase[4*i+1] = 0;
			phase[4*i+2] = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			phase[4*i+3] = 0;
		}
	}
	
	private void correctBounds() {
		for (int i = 0; i < N; i++) {
			phase[4*i+0] = (phase[4*i+0]+L)%L;
			phase[4*i+2] = (phase[4*i+2]+L)%L;
		}
	}
}
