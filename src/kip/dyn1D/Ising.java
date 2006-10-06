package kip.dyn1D;

import scikit.jobs.*;
import kip.ising.SpinBlocks1D;
import kip.util.Random;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class Ising extends Dynamics1D {
	public enum Dynamics {METROPOLIS, GLAUBER};
	public Dynamics dynamics = Dynamics.GLAUBER;
	public SpinBlocks1D spins;
	
	
	public Ising(Parameters params, Dynamics dynamics) {
		initialize(params);
		this.dynamics = dynamics;
	}
	
	
    public Ising clone() {
		Ising c = (Ising)super.clone();
		c.spins = (SpinBlocks1D)spins.clone();
		return c;
    }
	
	
	// reset time, set random number seed, initialize fields to down
	public void initialize(Parameters params) {
		super.initialize(params);
		spins = new SpinBlocks1D(N, R, -1);
		blocklen = 1;
	}
	
	
	public double magnetization() {
		return (double)spins.sumAll() / N;
	}
	
	
	public void randomizeField(double m) {
		if (m == 1 || m == -1) {
			for (int i = 0; i < N; i++)
				if (spins.get(i) != m)
					spins.flip(i);
		}
		else {
			for (int i = 0; i < N; i++)
				if (random.nextDouble() < (1 - spins.get(i)*m)/2)
					spins.flip(i);
		}
	}
	
	
	public double[] copyField(double[] field) {
		if (field == null)
			field = new double[N];
		if (field.length != N)
			throw new IllegalArgumentException();
		for (int i = 0; i < N; i++)
			field[i] = spins.get(i);
		return field;
	}
	
	
	private boolean shouldFlip(double dE) {
		switch (dynamics) {
			case METROPOLIS:
				return dE <= 0 || random.nextDouble() < Math.exp(-dE/T);
			case GLAUBER:
				return random.nextDouble() < exp(-dE/T)/(1+exp(-dE/T));
			default:
				assert false;
		}
		return false;
	}
	
	protected void _step() {
		for (int cnt = 0; cnt < N*dt; cnt++) {
			int i = random.nextInt(N);
			int spin = spins.get(i);
			
			double dE = 2*spin*(h + J*(spins.sumInRange(i)-spin)/(2*R));
			if (shouldFlip(dE)) {
				spins.flip(i);
			}
		}
	}
	
	/*
	public double[] langerDroplet(int center) {
		double[] saddle = new double[ψ.length];
		double dx = systemSize() / ψ.length;
		
		double K = 4/T;
		double H = h/T;
		double s = -abs(H)/H;
		double psi_sp = s*sqrt(1 - 1/K);
		double H_sp = (atanh(psi_sp) - K*psi_sp);		
		double dH = H_sp - H;
		double u_bg = sqrt(-dH / (K*K*psi_sp));
		
		for (int i = 0; i < saddle.length; i++) {			
			double d = dx * displacement(i, center);
			double c = cosh(sqrt(dH / (2*u_bg)) * d / R);
			saddle[i] = psi_sp + s * u_bg * (1 - 3 / (c*c));
		}
		
		return saddle;
	}
	*/
}