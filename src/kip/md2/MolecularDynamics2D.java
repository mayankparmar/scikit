package kip.md2;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Vector;

import kip.util.Random;

import org.opensourcephysics.numerics.*;

import scikit.graphics.Canvas;
import scikit.graphics.CircleGraphics;
import scikit.graphics.Particle2DGraphics;
import scikit.graphics.RectangleGraphics;
import scikit.util.Bounds;
import scikit.util.Point;
import scikit.util.Utilities;
import static scikit.util.Utilities.periodicOffset;


public class MolecularDynamics2D<Pt extends Particle<Pt>> {
	public Random rand = new Random(0);
	public Pt[] particles;
	public int N;
	public double L; // system length
	public boolean inDisk = true;
	
	protected boolean canonicalEnsemble = false; // microcanonical ensemble by default
	protected double T, gamma; // temperature, and thermodynamic coupling of system to heat bath
	
	// complete state of configuration.  positions, velocities, packed as
	// [x_1, vx_1, y_1, vy_1, ..., time]
	protected double phase[];
	// differential equation solver
	protected Verlet solver;
	// grid for finding neighbors quickly
	PointGrid2D<Pt> grid;

	
	public MolecularDynamics2D(double L, boolean inDisk, double dt, Pt[] particles) {
		this.L = L;
		this.inDisk = inDisk;
		this.particles = particles;
		N = particles.length;
		
		// particles belong to this object
		for (Pt p : particles)
			p.tag.md = this;
		
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
		particlesToPhase(phase); // TODO kill me
		
		grid = new PointGrid2D<Pt>(L, (int)sqrt(N), !inDisk, particles);
		
		solver = new Verlet(ode, 4*N);
		solver.initialize(dt);
	}
	
	public void layOutParticles() {
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		
		if (inDisk) {
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
		} else {
			int rootN = (int)ceil(sqrt(N));
			double dx = L/rootN;
			for (int i = 0; i < N; i++) {
				particles[indices[i]].x = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
				particles[indices[i]].y = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			}
		}
		particlesToPhase(phase); // TODO kill me
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
			Pt p = particles[i];
			p.x  = (state[4*i+0] + L)%L; 
			p.vx = state[4*i+1];
			p.y  = (state[4*i+2] + L)%L;
			p.vy = state[4*i+3];
			
			if (solver.getStepSize()*max(abs(p.vx), abs(p.vy)) > L/2) {
				throw new IllegalStateException("Simulation has destablized");
			}			
		}
	}
	
	public void step() {
		particlesToPhase(phase);
		solver.step();
		phaseToParticles(phase);
		
//		if (canonicalEnsemble)
//			brownianNoise();
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
	
	public double potentialEnergy() {
		double V = 0;
		
		grid.initialize();
		for (Pt p1 : particles) {
			for (Pt p2 : grid.pointOffsetsWithinRange(p1, p1.tag.interactionRange)) {
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
		for (Pt p : particles) {
			double M = p.tag.mass;
			K += 0.5*M*(p.vx*p.vx+p.vy*p.vy);
		}
		return K;
	}
	
	public double reducedKineticEnergy() {
		return (kineticEnergy() - N*T) / N*T;
	}
	
	
	public void addGraphicsToCanvas(Canvas canvas) {
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics graphics = new Particle2DGraphics(particles[0].tag.radius, bounds, particles[0].tag.color);
		graphics.setPoints(phase, 2, 0, N);
		canvas.addGraphics(graphics);
		canvas.addGraphics(inDisk ? new CircleGraphics(L/2., L/2., L/2.) : new RectangleGraphics(0., 0., L, L));

	}

	public void boundaryDistance(Point p, double[] o) {
		if (inDisk) {
			double boundaryRadius = L/2.;
			double xc = p.x - L/2.;
			double yc = p.y - L/2.;
			double distanceFromCenter = sqrt(xc*xc + yc*yc);
			o[0] = xc*(boundaryRadius/distanceFromCenter - 1);
			o[1] = yc*(boundaryRadius/distanceFromCenter - 1);
			o[2] = 0;
		}
	}

	public void displacement(Point p1, Point p2, double[] o) {
		if (inDisk) {
			o[0] = p2.x-p1.x;
			o[1] = p2.y-p1.y;
			o[2] = 0;
		}
		else {
			o[0] = periodicOffset(L, p2.x-p1.x);
			o[1] = periodicOffset(L, p2.y-p1.y);
			o[2] = 0;
		}
	}
	
	private void getRate(double[] state, double[] rate) {
		phaseToParticles(state);
		
		for (int i = 0; i < N; i++) {
			// set dx/dt = v
			rate[4*i+0] = particles[i].vx;
			rate[4*i+2] = particles[i].vy;
		}
		
//		if (solver.getRateCounter() == 1)
			calculateForces(rate);
		
		rate[4*N] = 1;
	}
	
	
	// TODO use Vec3 for forces
	private void calculateForces(double[] rate) {
		double[] f = new double[3];

		grid.initialize();
		for (int i = 0; i < N; i++) {
			Pt p1 = particles[i];
			double M = p1.tag.mass;
			
			// initialize accelerations to zero
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			
			
			ArrayList<Pt> ns = grid.pointOffsetsWithinRangeSlow(p1, p1.tag.interactionRange);
//			Comparator<Pt> cmp = new Comparator<Pt>() {
//				public int compare(Pt p1, Pt p2) {
//					if (p1.x != p2.x) {
//						return (int)kip.util.MathPlus.sign(p1.x - p2.x);
//					}
//					else {
//						return (int)kip.util.MathPlus.sign(p1.y - p2.y);
//					}
//				}
//			};
//			java.util.Collections.sort(ns, cmp);
			
			// accumulate accelerations due to pairwise interactions
			for (Pt p2 : ns) {
				if (p1 != p2) {
					p1.force(p2, f);
					rate[4*i+1] += f[0]/M;
					rate[4*i+3] += f[1]/M;
				}
			}
			
			// accumulate accelerations due to external forces
			p1.force(f);
			rate[4*i+1] += f[0]/M;
			rate[4*i+3] += f[1]/M;
		}
	}
	
	private void brownianNoise() {
		// accumulate accelerations due to stochastic noise
		for (Pt p : particles) {
			double M = p.tag.mass;
			// dp/dt = - gamma 2p/2m + sqrt(2 gamma T) eta
			// dv/dt = - (gamma v + sqrt(2 gamma T) eta) / m
			double dt = solver.getStepSize();
			p.vx += (-dt*gamma*p.vx + sqrt(2*dt*gamma*T)*rand.nextGaussian())/M;
			p.vy += (-dt*gamma*p.vy + sqrt(2*dt*gamma*T)*rand.nextGaussian())/M;
		}
	}
}
