package kip.md.apps;

import java.awt.Color;
import kip.clump.PtsGrid;
import kip.util.Random;
import scikit.graphics.*;
import scikit.jobs.*;


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
		params.add("Length", 50);
		params.add("Density A", 1);
		params.add("Sigma A", 1);
		params.add("Epsilon A", 1);
		params.add("Mass A", 1);
		params.add("dt", 0.01);
	}
	
	public static void main(String[] args) {
		new Control(new CobbAnderson(), "Particle Simulation");
	}
	
	public void animate() {
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
		
		while (true)
			Job.animate();
	}
	
	private void initializeParticles() {
		for (int i = 0; i < N; i++) {
			phase[(2*i+0)*2] = 2 + rand.nextDouble()*(L-4);
			phase[(2*i+1)*2] = 2 + rand.nextDouble()*(L-4);
		}
	}
}
