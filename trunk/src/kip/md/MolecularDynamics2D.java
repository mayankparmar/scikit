package kip.md;

import static java.lang.Math.*;

import java.awt.Color;
import java.util.ArrayList;

import kip.util.Random;

import org.opensourcephysics.numerics.ODE;
import org.opensourcephysics.numerics.Verlet;

import scikit.dataset.DynamicArray;
import scikit.graphics.Particle2DGraphics;
import scikit.util.Bounds;
import scikit.util.Utilities;

abstract public class MolecularDynamics2D {
	
	protected final double PARTICLES_PER_CELL = 1;
	protected Random rand = new Random();
	protected boolean periodic = true;
	protected double L; // system length
	protected double interactionRange; // distance within which particles interact
	protected boolean canonicalEnsemble = false; // microcanonical ensemble by default
	protected double T, gamma; // temperature, and thermodynamic coupling of system to heat bath
	
	protected int N; // total number of particles
	protected int numParticleTypes = 0; // number of types of particles
	protected ArrayList<Integer> indexToType = new ArrayList<Integer>();
	protected ArrayList<Integer> typeToIndex = new ArrayList<Integer>();
	// particle description, one for each particle type
	protected ArrayList<Double> masses = new ArrayList<Double>();
	protected ArrayList<PointGrid2D> grids = new ArrayList<PointGrid2D>();
	
	// complete state of configuration.  positions, velocities, packed as
	// [x_1, vx_1, y_1, vy_1, ..., time]
	protected double phase[];
	// differential equation solver
	protected Verlet solver;
	
	abstract public double[] getParticlePositions();
	abstract public double getPairwisePotential(int type1, int type2, double r);
	abstract public double getPairwiseForce(int type1, int type2, double r);
	abstract public double getExternalPotential(int type, double x, double y);
	abstract public double[] getExternalForce(int type, double x, double y);
	
	
	public MolecularDynamics2D(double L, double interactionRange) {
		this.L = L;
		this.interactionRange = interactionRange;
	}
	
	public void addParticleType(int numParticles, double mass) {
		typeToIndex.add(N);
		for (int i = 0; i < numParticles; i++)
			indexToType.add(numParticleTypes);
		
		numParticleTypes++;
		N += numParticles;
		
		masses.add(mass);
		grids.add(new PointGrid2D(L, (int)(sqrt(numParticles/PARTICLES_PER_CELL))));
		phase = null;
	}
	
	public void initialize(double dt) {
		typeToIndex.add(N);
		
		// initialize phase space array
		phase = new double[4*N+1];
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		double positions[] = getParticlePositions();
		for (int i = 0; i < N; i++) {
			int j = indices[i];
			phase[4*j+0] = positions[2*i+0];	// x
			phase[4*j+1] = 0;					// vx
			phase[4*j+2] = positions[2*i+1];	// y
			phase[4*j+3] = 0;					// vy
		}
		phase[4*N] = 0;							// time
		
		// initialize ODE solver
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
	
	public void setStepSize(double dt) {
		solver.setStepSize(dt);
	}
	
	public double getStepSize() {
		return solver.getStepSize();
	}
	
	public void step() {
		System.out.println(kineticEnergy() + potentialEnergy());
		solver.step();
//		brownianNoise();
		correctBounds();
	}
	
	public void setPeriodic(boolean b) {
		periodic = b;
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
		for (int type = 0; type < numParticleTypes; type++) {
			grids.get(type).setPoints(phase, 2, typeToIndex.get(type), typeToIndex.get(type+1));
			grids.get(type).setPeriodic(periodic);
		}
		for (int i = 0; i < N; i++) {
			int type1 = indexToType.get(i);
			double x = phase[4*i+0];
			double y = phase[4*i+2];
			
			// accumulate pairwise interactions
			for (int type2 = 0; type2 < numParticleTypes; type2++) {
				DynamicArray ns = grids.get(type2).pointOffsetsWithinRange(x, y, interactionRange);
				for (int j = 0; j < ns.size()/2; j++) {
					double dx = ns.get(2*j+0);
					double dy = ns.get(2*j+1);
					double r = sqrt(dx*dx + dy*dy);
					if (0 < r) {
						V += getPairwisePotential(type1, type2, r);
					}
				}
			}
			
			// accumulate accelerations due to external forces
			V += getExternalPotential(type1, x, y);
		}
		return V;
	}
	
	
	public double kineticEnergy() {
		double K = 0;
		for (int i = 0; i < N; i++) {
			double M = masses.get(indexToType.get(i));
			double vx = phase[4*i+1];
			double vy = phase[4*i+3];
			K += 0.5*M*(vx*vx+vy*vy);
		}
		return K;
	}
	
	public double reducedKineticEnergy() {
		return (kineticEnergy() - N*T) / N*T;
	}
	
	public Particle2DGraphics getGraphics(int type, double R, Color color) {
		Bounds bounds = new Bounds(0, L, 0, L);
		Particle2DGraphics graphics = new Particle2DGraphics(R, bounds, color);
		graphics.setPoints(phase, 2, typeToIndex.get(type), typeToIndex.get(type+1));
		return graphics;
	}
	
	protected double[] getParticlePositionsInSquare() {
		double ret[] = new double[2*N];
		int rootN = (int)ceil(sqrt(N));
		double dx = L/rootN;
		for (int i = 0; i < N; i++) {
			ret[2*i+0] = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			ret[2*i+1] = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
		}
		return ret;
	}
	
	protected double[] getParticlePositionsInDisk() {
		double ret[] = new double[2*N];
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
			ret[2*i+0] = L/2. + r*cos(a);
			ret[2*i+1] = L/2. + r*sin(a);
		}
		return ret;
	}
	
	private void correctBounds() {
		for (int i = 0; i < N; i++) {
			phase[4*i+0] = (phase[4*i+0]+L)%L;
			phase[4*i+2] = (phase[4*i+2]+L)%L;
			
			if (max(abs(phase[4*i+1]), abs(phase[4*i+3])) > L/2) {
				throw new IllegalStateException("Simulation has destablized");
			}
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
		for (int type = 0; type < numParticleTypes; type++) {
			grids.get(type).setPoints(state, 2, typeToIndex.get(type), typeToIndex.get(type+1));
			grids.get(type).setPeriodic(periodic);
		}
		
		for (int i = 0; i < N; i++) {
			int type1 = indexToType.get(i);
			double x = state[4*i+0];
			double y = state[4*i+2];
			double M = masses.get(indexToType.get(i)); 
			
			// initialize accelerations to zero
			rate[4*i+1] = 0;
			rate[4*i+3] = 0;
			
			// accumulate accelerations due to pairwise interactions
			for (int type2 = 0; type2 < numParticleTypes; type2++) {
				DynamicArray ns = grids.get(type2).pointOffsetsWithinRange(x, y, interactionRange);
				for (int j = 0; j < ns.size()/2; j++) {
					double dx = ns.get(2*j+0);
					double dy = ns.get(2*j+1);
					double r = sqrt(dx*dx + dy*dy);
					if (0 < r) {
						double force = getPairwiseForce(type1, type2, r);
						rate[4*i+1] += (dx/r)*force/M;
						rate[4*i+3] += (dy/r)*force/M;
					}
				}
			}
			
			// accumulate accelerations due to external forces
			double force[] = getExternalForce(type1, x, y);
			rate[4*i+1] += force[0]/M;
			rate[4*i+3] += force[1]/M;
		}
	}
	
	private void brownianNoise() {
		// accumulate accelerations due to stochastic noise
		if (canonicalEnsemble) {
			for (int i = 0; i < N; i++) {
				double M = masses.get(indexToType.get(i)); 
				// dp/dt = - gamma 2p/2m + sqrt(2 gamma T) eta
				// dv/dt = - (gamma v + sqrt(2 gamma T) eta) / m
				double dt = solver.getStepSize();
				phase[4*i+1] += sqrt(2*dt*gamma*T)*rand.nextGaussian()/M;
				phase[4*i+1] += -dt*gamma*(phase[4*i+1])/M;
				phase[4*i+3] += sqrt(2*dt*gamma*T)*rand.nextGaussian()/M;
				phase[4*i+3] += -dt*gamma*(phase[4*i+3])/M;
			}
		}
	}
}
