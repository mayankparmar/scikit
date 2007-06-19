package kip.md.apps;

import java.awt.Color;
import static java.lang.Math.*;
import kip.clump.PtsGrid;
import kip.util.Random;
import scikit.dataset.DynamicArray;
import scikit.graphics.*;
import scikit.jobs.*;
import static scikit.util.Utilities.format;
import org.opensourcephysics.numerics.*;


public class CobbAnderson extends Simulation {
	Canvas2D canvas = new Canvas2D("Particles");
	Random rand = new Random();
	
	double L;		// system length
	// lennard jones parameters
	// V = 4 eps [ (sig/r)^12 - (sig/r)^6 ]
	double sigma;
	double epsilon;
	
	PtsGrid pts;
	int N;
	// complete state of configuration.  4N+1 elements: positions, velocities, time.
	// packed as: (x_1, vx_1, y_1, vy_1, ..., time)
	double[] phase;
	
	
	public CobbAnderson() {
		params.add("Length", 10.0);
		params.add("Density A", 1.0);
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
	}
	
	public void run() {
		L = params.fget("Length");
		N = (int) (params.fget("Density A")*L*L);
		
		sigma = params.fget("Sigma A");
		epsilon = params.fget("Epsilon A");
		
		phase = new double[4*N+1];
		initializeParticles();
		
		Particles2D particles = new Particles2D(phase, 0.2, L, Color.BLUE);
		particles.setPhaseArrayFormat(2, 0, N);
		canvas.addDrawable(particles);
		Job.addDisplay(canvas);
		
		ODE ode = new ODE() {
			public void getRate(double[] state, double[] rate) {
				CobbAnderson.this.getRate(state, rate);
			}
			public double[] getState() {
				return phase;
			}
		};
		
		ODESolver solver = new Verlet(ode);
		solver.initialize(params.fget("dt"));
		while (true) {
			solver.step();
			Job.animate();
		}
	}
	
	private void getRate(double[] state, double[] rate) {
		for (int i = 0; i < N; i++) {
			// set dx/dt = v
			rate[4*i+0] = state[4*i+1];
			rate[4*i+2] = state[4*i+3];
			calculateForce(i, state, rate);
		}
		rate[4*N] = 1;
	}
	
	private DynamicArray getPointsInRange(double x, double y) {
		return null;
	}
	
	private void calculateForce(int i, double[] state, double[] rate) {
		rate[4*i+1] = 0.00001;
		rate[4*i+3] = 0;
	}
	
	private void initializeParticles() {
		int rootN = (int)ceil(sqrt(N));
		double dx = L/rootN;
		for (int i = 0; i < N; i++) {
			int x = i % rootN;
			int y = i / rootN;
			phase[(2*i+0)*2] = (x+0.5+0.1*rand.nextGaussian()) * dx;
			phase[(2*i+1)*2] = (y+0.5+0.1*rand.nextGaussian()) * dx;
		}
	}
}
