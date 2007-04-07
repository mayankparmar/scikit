package mina;


import static java.lang.Math.*;
import scikit.params.DoubleValue;
import scikit.params.IntValue;
import scikit.plot.*;
import scikit.jobs.*;


public class PistonApp extends Job {
	public static void main(String[] args) {
		frame(new Control(new PistonApp()), "Ideal Gas Simulation");
	}
	
	double[] pistonLine = new double[2];
	Plot particles = new Plot("Particles", true);
	Plot enthalpy = new Plot("Enthalpy: U + PV", true);
	Plot idealGas = new Plot("PV / NkT", true);
	Histogram distrib = new Histogram("Velocity distribution", 0.1, true);
	
	int N;			// number of particles
	double T;		// kinetic energy
	double t, dt;	// time, time step
	
	double px, pv;	// piston phase coordinates
	double pa;		// piston acceleration
	double pm;		// piston mass
	double[] x, v;	// gas phase coordinates
	double m = 1;	// gas mass
	
	
	public PistonApp() {
		params.add("Initial piston position", new DoubleValue(10.0, 0, 100));
		params.add("Initial piston velocity", new DoubleValue(0.0, -1, 1));
		params.add("# of particles", new IntValue(1000, 1, 5000));
		params.addm("Piston mass", new DoubleValue(100.0, 0, 200)).enableAuxiliaryEditor();
		params.addm("Piston acceleration", new DoubleValue(0.00001, 0,  0.00002)).enableAuxiliaryEditor();
		params.addm("dt", new DoubleValue(0.05, 0, 0.2));
		params.addm("Bin width", new DoubleValue(0.0002, 0.00005, 0.01));
		
		addDisplay(particles);
		addDisplay(enthalpy);
		addDisplay(idealGas);
		addDisplay(distrib);
	}
	
	
	public void animate() {
		pm  = params.fget("Piston mass");
		pa	= params.fget("Piston acceleration");
		dt	= params.fget("dt");
		
		pistonLine[0] = pistonLine[1] = px;
		idealGas.append(0, t, force()*px / (N * kT()));
		enthalpy.append(0, t, kineticEnergy());
		enthalpy.append(1, t, force()*px);
		enthalpy.append(2, t, kineticEnergy() + force()*px);
		
		double dv = params.fget("Bin width");
		double maxV = Double.NEGATIVE_INFINITY;
		double minV = Double.POSITIVE_INFINITY;
		distrib.clear();
		distrib.setBinWidth(2, dv);
		for (double vi : v) {
			distrib.accum(2, vi);
			maxV = max(maxV, vi);
			minV = min(minV, vi);
		}
		for (double vi = minV; vi < maxV; vi += dv/4) {
			distrib.append(0, vi, N*velocityProbability(vi, dv));
		}
	}
	
	
	public void run() {
		N = params.iget("# of particles");
		px	= params.fget("Initial piston position");
		pv	= params.fget("Initial piston velocity");
		pm  = params.fget("Piston mass");
		pa	= params.fget("Piston acceleration");
		dt	= params.fget("dt");
		
		t = 0;
		v = new double[N];
		x = new double[N];
		for (int i = 0; i < N; i++) {
			x[i] = random() * px;
			v[i] = 0.001 * (2*random() - 1);
		}
		
		particles.setStyle(0, Plot.Style.MARKS);
		particles.setDataSet(0, new PointSet(0, 1, x));
		particles.setDataSet(1, new PointSet(-0.1, N-0.8, pistonLine));
		
		while (true) {
			simulationStep();
			yield();
		}
	}
	
	
	private void simulationStep() {
		pv += - pa * dt;
		px += pv * dt - 0.5*pa*dt*dt;
		
		for (int i = 0; i < N; i++) {
			x[i] += v[i] * dt;
			
			if (x[i] < 0 && v[i] < 0) {
				x[i] = 0;
				v[i] = -v[i];
			}
			
			if (x[i] > px && v[i] > pv) {
				// elastic collision with piston
				x[i] = px;
				double m1 = m;
				double m2 = pm;
				double v1 = v[i];
				double v2 = pv;
				v[i] =   (m1-m2)*v1/(m1+m2) + 2*m2*v2/(m1+m2);
				pv   = - (m1-m2)*v2/(m1+m2) + 2*m1*v1/(m1+m2);
			}
		}
		
		t += dt;
	}
	
	
	// F = ma
	private double force() {
		return pa*pm;
	}
	
	// kinetic energy = internal energy = 1/2 NkT
	private double kT() {
		return 2*kineticEnergy()/N;
	}
	
	private double kineticEnergy() {
		double sum = 0;
		for (double vi : v)
			sum += vi*vi;
		return sum/2;
	}
	
	// Probability particle velocity lies within [v, v+dv]
	// Boltzmann velocity distribution: P*dv ~ e^(beta*m*v^2 / 2)
	// Normalize by integral[P*dv, v=-inf..inf] = sqrt(2*pi / beta*m)
	private double velocityProbability(double v, double dv) {
		double beta = 1 / kT();
		return dv*sqrt(beta*m/(2*PI))*exp(-beta*m*v*v/2);
	}
}
