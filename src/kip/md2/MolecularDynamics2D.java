package kip.md2;

import static java.lang.Math.*;

import kip.util.Random;
import kip.util.Vec3;

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
	public double time, dt;
	
	protected boolean canonicalEnsemble = false; // microcanonical ensemble by default
	protected double T, gamma; // temperature, and thermodynamic coupling of system to heat bath
	
	
	// complete state of configuration.  positions, velocities, packed as
	// [x_1, vx_1, y_1, vy_1, ..., time]
	protected double phase[];
	// differential equation solver
	protected ODESolver solver;
	// grid for finding neighbors quickly
	PointGrid2D<Pt> grid;

	
	public MolecularDynamics2D(double L, boolean inDisk, double dt, Pt[] particles) {
		this.particles = particles;
		N = particles.length;
		this.L = L;
		this.inDisk = inDisk;
		time = 0;
		this.dt = dt;
		
		// initialize particles
		for (Pt p : particles)
			p.tag.initialize(this);
		layOutParticles();
		
		// initialize phase space array and ODE solver
		phase = new double[4*N+1];
		writeStateArray(phase); // TODO kill me		
		ODE ode = new ODE() {
			public void getRate(double[] state, double[] rate) {
				MolecularDynamics2D.this.getRate(state, rate);
			}
			public double[] getState() {
				return phase;
			}
		};
		grid = new PointGrid2D<Pt>(L, (int)sqrt(N), !inDisk, particles);
		solver = new Verlet(ode, 4*N);
		solver.initialize(dt);
	}
	
	private void layOutParticles() {
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
	}
	
	public void setStepSize(double dt) {
		solver.setStepSize(dt);
	}
	
	public double getStepSize() {
		return solver.getStepSize();
	}
	
	private void writeStateArray(double[] state) {
		for (int i = 0; i < N; i++) {
			state[4*i+0] = particles[i].x; 
			state[4*i+1] = particles[i].vx;
			state[4*i+2] = particles[i].y;
			state[4*i+3] = particles[i].vy; 
		}
		state[4*N] = time;
	}
	
	private void readStateArray(double[] state) {
		for (int i = 0; i < N; i++) {
			Pt p = particles[i];
			p.x  = (state[4*i+0] + L)%L; 
			p.vx = state[4*i+1];
			p.y  = (state[4*i+2] + L)%L;
			p.vy = state[4*i+3];
			
			if (dt*max(abs(p.vx), abs(p.vy)) > L/2) {
				throw new IllegalStateException("Simulation has destablized");
			}
		}
		time = state[4*N];
	}
	
	public void step() {
		writeStateArray(phase);
		solver.step();
		readStateArray(phase);
		
		if (canonicalEnsemble)
			brownianNoise();
	}
	
	
	public void setTemperature(double T, double gamma) {
		if (T >= 0) {
			canonicalEnsemble = true;
			this.T = T;
			this.gamma = gamma;
		}
		else {
			disableTemperature();
		}
	}
	
	public void disableTemperature() {
		canonicalEnsemble = false;
	}
	
	public double time() {
		return time;
	}
	
	public double potentialEnergy() {
		double V = 0;
		
		grid.initialize();
		for (Pt p1 : particles) {
			for (Pt p2 : grid.pointOffsetsWithinRange(p1, p1.tag.interactionRange)) {
				if (p1 != p2)
					V += p1.potential(p2)/2.; // divisor of 2 corrects for double counting
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

	Vec3 tempVec = new Vec3(0,0,0);
	
	public Vec3 boundaryDistance(Point p) {
		if (inDisk) {
			double boundaryRadius = L/2.;
			double xc = p.x - L/2.;
			double yc = p.y - L/2.;
			double distanceFromCenter = sqrt(xc*xc + yc*yc);
			tempVec.x = xc*(boundaryRadius/distanceFromCenter - 1);
			tempVec.y = yc*(boundaryRadius/distanceFromCenter - 1);
			tempVec.z = 0;
			return tempVec;
		}
		else
			return null;
	}

	public Vec3 displacement(Point p1, Point p2) {
		if (inDisk) {
			tempVec.x = p2.x-p1.x;
			tempVec.y = p2.y-p1.y;
			tempVec.z = 0;
		}
		else {
			tempVec.x = periodicOffset(L, p2.x-p1.x);
			tempVec.y = periodicOffset(L, p2.y-p1.y);
			tempVec.z = 0;
		}
		return tempVec;
	}
	
	private void getRate(double[] state, double[] rate) {
		readStateArray(state);
		for (int i = 0; i < N; i++) {
			// set dx/dt = v
			rate[4*i+0] = particles[i].vx;
			rate[4*i+2] = particles[i].vy;
		}
		calculateForces(rate);		
		rate[4*N] = 1; // dt/dt = 1
	}
	
	
	private void calculateForces(double[] rate) {
		Vec3 f = new Vec3(0,0,0);
		
		grid.initialize();
		for (int i = 0; i < N; i++) {
			Pt p1 = particles[i];
			double M = p1.tag.mass;
			
			// initialize accelerations to zero
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			
			// accumulate accelerations due to pairwise interactions
			for (Pt p2 : grid.pointOffsetsWithinRange(p1, p1.tag.interactionRange)) {
				if (p1 != p2) {
					p1.force(p2, f);
					rate[4*i+1] += f.x/M;
					rate[4*i+3] += f.y/M;
				}
			}
			
			// accumulate accelerations due to external forces
			p1.force(f);
			rate[4*i+1] += f.x/M;
			rate[4*i+3] += f.y/M;
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
