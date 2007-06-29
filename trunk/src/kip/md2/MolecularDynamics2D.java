package kip.md2;

import static java.lang.Math.*;

import java.awt.Color;
import java.util.ArrayList;

import kip.util.Random;

import org.opensourcephysics.numerics.ODE;
import org.opensourcephysics.numerics.Verlet;

import scikit.graphics.Particle2DGraphics;
import scikit.util.Bounds;
import scikit.util.Utilities;

public class MolecularDynamics2D {
	protected Random rand = new Random();
	protected Particle<?,?>[] particles;
	protected int N;
	protected double L; // system length
	
	protected boolean canonicalEnsemble = false; // microcanonical ensemble by default
	protected double T, gamma; // temperature, and thermodynamic coupling of system to heat bath
	
	// complete state of configuration.  positions, velocities, packed as
	// [x_1, vx_1, y_1, vy_1, ..., time]
	protected double phase[];
	// differential equation solver
	protected Verlet solver;
	
	
	public MolecularDynamics2D(double L, double dt, Particle[] particles) {
		this.L = L;
		this.particles = particles;
		N = particles.length;
		
		// initialize phase space array and ODE solver
		phase = new double[4*N+1];
		phase[4*N] = 0;	// time
		ODE ode = new ODE() {
			public void getRate(double[] state, double[] rate) {
				MolecularDynamics2D.this.getRate(state, rate);
			}
			public double[] getState() {
				return phase;
			}
		};
		solver = new Verlet(ode, 4*N);
		solver.initialize(dt);
	}
	
	public void layoutParticlesInDisk(Particle[] particles) {
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		// this value of R is such that the minimum distance from a particle to the wall is half
		// the minimum interparticle distance
		double R = L / (2 + sqrt(PI/N));
		for (int i = 0; i < N; i++) {
			// these expressions for radius and angle are chosen to give a spiral trajectory
			// in which the radial distance between loops has a fixed length, and the "velocity"
			// is constant. here the particle number, i, plays the role of "time".
			// it is somewhat surprising that the angle a(t) is independent of the bounding radius R
			// and total particle number N.
			double r = R * sqrt((double)(i+1) / N);
			double a = 2 * sqrt(PI * (i+1));
			particles[indices[i]].x = L/2. + r*cos(a);
			particles[indices[i]].y = L/2. + r*sin(a);
		}		
	}
	
	public void layoutParticlesInSquare(Particle[] particles) {
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		int rootN = (int)ceil(sqrt(N));
		double dx = L/rootN;
		for (int i = 0; i < N; i++) {
			particles[indices[i]].x = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			particles[indices[i]].y = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
		}		
	}
	
	public void setStepSize(double dt) {
		solver.setStepSize(dt);
	}
	
	public double getStepSize() {
		return solver.getStepSize();
	}
	
	private void particlesToPhase(double[] state) {
		for (int i = 0; i < N; i++) {
			state[4*i+0] = particles[i].x; 
			state[4*i+1] = particles[i].vx;
			state[4*i+2] = particles[i].y;
			state[4*i+3] = particles[i].vy; 
		}
	}
	
	private void phaseToParticles(double[] state) {
		for (int i = 0; i < N; i++) {
			particles[i].x  = state[4*i+0]; 
			particles[i].vx = state[4*i+1];
			particles[i].y  = state[4*i+2];
			particles[i].vy = state[4*i+3]; 
		}		
	}
	
	public void step() {
		System.out.println(kineticEnergy() + potentialEnergy());
		
		particlesToPhase(phase);
		solver.step();
		phaseToParticles(phase);
		
//		brownianNoise();
		correctBounds();
	}
	
	
	public void setTemperature(double T, double gamma) {
		canonicalEnsemble = true;
		this.T = T;
		this.gamma = gamma;
	}
	
	public void disableTemperature() {
		canonicalEnsemble = false;
	}
	
	public double time() {
		return phase[4*N];
	}
	
	@SuppressWarnings("unchecked")
	public double potentialEnergy() {
		double V = 0;
		
		PointGrid2D<Particle> grid = new PointGrid2D<Particle>(L, (int)sqrt(N), particles);
		for (Particle p1 : particles) {
			ArrayList<Particle> ns = grid.pointOffsetsWithinRange(p1, p1.tag.interactionRange());
			for (Particle p2 : ns) {
				if (p1 != p2)
					V += p1.potential(p2);
			}
			// accumulate accelerations due to external forces
			V += p1.potential();
		}
		return V;
	}
	
	
	public double kineticEnergy() {
		double K = 0;
		for (Particle p : particles) {
			double M = p.tag.mass;
			K += 0.5*M*(p.vx*p.vx+p.vy*p.vy);
		}
		return K;
	}
	
	public double reducedKineticEnergy() {
		return (kineticEnergy() - N*T) / N*T;
	}
	
	// TODO remove me
	public Particle2DGraphics getGraphics(int type, double R, Color color) {
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics graphics = new Particle2DGraphics(R, bounds, color);
		graphics.setPoints(phase, 2, 0, N);
		return graphics;
	}
	
	private void correctBounds() {
		for (Particle p : particles) {
			p.x = (p.x+L)%L;
			p.y = (p.y+L)%L;
			if (max(abs(p.vx), abs(p.vy)) > L/2) {
				throw new IllegalStateException("Simulation has destablized");
			}
		}
	}
	
	private void getRate(double[] state, double[] rate) {
		phaseToParticles(state);
		
		for (int i = 0; i < N; i++) {
			// set dx/dt = v
			rate[4*i+0] = particles[i].vx;
			rate[4*i+2] = particles[i].vy;
		}
		
		if (solver.getRateCounter() == 1)
			calculateForces(rate);
		
		rate[4*N] = 1;
	}
	
	
	// TODO use Vec3 for forces
	private void calculateForces(double[] rate) {
		PointGrid2D<Particle> grid = new PointGrid2D<Particle>(L, (int)sqrt(N), particles);
		
		for (int i = 0; i < N; i++) {
			Particle p1 = particles[i];
			double M = p1.tag.mass;
			
			// initialize accelerations to zero
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			
			// accumulate accelerations due to pairwise interactions
			for (Particle p2 : grid.pointOffsetsWithinRange(p1, p1.tag.interactionRange())) {
				if (p1 != p2) {
					double[] force = p1.force(p2);
					rate[4*i+1] += force[0]/M;
					rate[4*i+3] += force[1]/M;
				}
			}
			
			// accumulate accelerations due to external forces
			double force[] = p1.force();
			rate[4*i+1] += force[0]/M;
			rate[4*i+3] += force[1]/M;
		}
	}
	
	private void brownianNoise() {
		// accumulate accelerations due to stochastic noise
		if (canonicalEnsemble) {
			for (Particle p : particles) {
				double M = p.tag.mass;
				// dp/dt = - gamma 2p/2m + sqrt(2 gamma T) eta
				// dv/dt = - (gamma v + sqrt(2 gamma T) eta) / m
				double dt = solver.getStepSize();
				p.vx += (-dt*gamma*p.vx + sqrt(2*dt*gamma*T)*rand.nextGaussian())/M;
				p.vy += (-dt*gamma*p.vy + sqrt(2*dt*gamma*T)*rand.nextGaussian())/M;
			}
		}
	}
}
