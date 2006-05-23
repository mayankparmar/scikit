package kip.dyn1D;

import scikit.jobs.*;
import kip.ising.SpinBlocks1D;
import kip.util.Random;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class Ising extends Dynamics1D {
    public int N, R;
	public double h, dt, T, J;
	public int tN;
	SpinBlocks1D spins;
	
	
	public Ising(Parameters params) {
		
		N = params.iget("N");
		if (N > Integer.highestOneBit(N))
			N = Integer.highestOneBit(N);
		params.set("N", N);
		
		R = params.iget("R");
		if (2*R+1 >= N)
			R = N/2 - 1;
		params.set("R", R);
		
		memoryTime = params.fget("Memory time");
		J = 4.0 / (2*R);
		
		initialize(params);
	}
	
	
    public Ising clone() {
		Ising c = (Ising)super.clone();
		c.spins = (SpinBlocks1D)spins.clone();
		return c;
    }
	
	
	// set parameters which are allowed to change during the simulation
	public void setParameters(Parameters params) {
		T  = params.fget("T");
		h  = params.fget("h");
		dt = params.fget("dt");
	}
	
	
	// reset time, set random number seed, initialize fields to down
	public void initialize(Parameters params) {
		super.initialize(params);
		
		random.setSeed(params.iget("Random seed"));
		setParameters(params);
		
		tN = 0;
		spins = new SpinBlocks1D(N, R, -1);
		initializeField(64, spins.getAll());
	}
	
	
	public int systemSize() { return N; }
	public double time() { return (double)tN / N; }
	public double temperature() { return T; }
	public double externalField() { return h; }
	
	protected void _step() {
		for (int cnt = 0; cnt < N*dt; cnt++) {
			int i = random.nextInt(N);
			int spin = spins.get(i);
			double dE = 2*spin*(h + J*(spins.sumInRange(i)-spin));
			if (dE <= 0 || random.nextDouble() < Math.exp(-dE/T)) {
				spins.flip(i);
				spinFlippedInField(i, spins.getAll());
			}
			tN++;
		}
	
	}
	
	
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
}
