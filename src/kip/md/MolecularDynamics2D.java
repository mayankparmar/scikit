package kip.md;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.util.ArrayList;

import kip.md.apps.CobbAnderson;
import kip.util.Random;

import org.opensourcephysics.numerics.ODE;
import org.opensourcephysics.numerics.Verlet;

import scikit.util.Utilities;

abstract public class MolecularDynamics2D {
	
	protected final double PARTICLES_PER_CELL = 1;
	protected Random rand = new Random();
	protected enum Topology {Disk, Square};
	protected Topology topology;
	protected double L; // system length
	protected double T = Double.NaN; // temperature; NaN indicates microcanonical ensemble
	
	// particle description, one for each particle type
	protected int numParticleTypes;
	protected ArrayList<Integer> Ns;
	protected ArrayList<Double> masses;
	protected ArrayList<PointGrid2D> grids;
	protected int N;
	
	// complete state of configuration.  positions, velocities, packed as
	// [x_1, vx_1, y_1, vy_1, ..., time]
	public double phase[];
	protected Verlet solver;
	
	
	public void addParticleType(int N, double mass) {
		this.N += N;
		Ns.add(N);
		masses.add(mass);
		grids.add(new PointGrid2D(L, (int)(sqrt(N/PARTICLES_PER_CELL))));
		phase = null;
	}
	
	public void initialize(double dt) {
		phase = new double[4*N+1];
		
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		switch (topology) {
		case Disk:
			initializeParticlesInDisk(indices);
			break;
		case Square:
			initializeParticlesInSquare(indices);
			break;
		}
		phase[4*N] = 0;
		
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
	
	public void useConstantTemperature(double T) {
		this.T = T;
	}
	
	public void useConstantEnergy() {
		T = Double.NaN;
	}
	
	private void initializeParticlesInSquare(int indices[]) {
		int rootN = (int)ceil(sqrt(N));
		double dx = L/rootN;
		for (int i = 0; i < N; i++) {
			double x = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			double y = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			initializeParticle(indices[i], x, y);
		}
	}
	
	private void initializeParticlesInDisk(int indices[]) {
		// this value of R was derived so that the distance from a particle to the wall is half
		// the interparticle distances given below
		double R = L / (2 + sqrt(PI/N));
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
			initializeParticle(indices[i], x, y);
		}
	}
	
	private void initializeParticle(int i, double x, double y) {
		phase[4*i+0] = x;
		phase[4*i+1] = 0;
		phase[4*i+2] = y;
		phase[4*i+3] = 0;
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
	
	private double kineticEnergy() {
		double K = 0;
		int i = 0;
		for (int type = 0; type < numParticleTypes; type++) {
			for (int j = 0; j < Ns.get(type); j++, i++) {
				double M = masses.get(type);
				double vx = phase[4*i+1];
				double vy = phase[4*i+3];
				K += 0.5*M*(vx*vx+vy*vy);
			}
		}
		return K;
	}
	
	private void calculateForces(double[] state, double[] rate) {}
}
