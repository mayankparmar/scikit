package kip.ising;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
import java.util.Random;


public class FastIsing2DApp extends AbstractSimulation {
	LatticeFrame lattice;
	Random r = new Random(0);
	
	int L, N, R;
	double E, T, J, h;
	double t;
	SpinBlocks2D spins;
	

	public FastIsing2DApp() {
		lattice = new LatticeFrame("Ising Spins");
		lattice.setIndexedColor(-1, java.awt.Color.blue);
		lattice.setIndexedColor(0, java.awt.Color.blue);
		lattice.setIndexedColor(1, java.awt.Color.green);
	}
	
	
	public void initialize() {
		L = control.getInt("L");
		R = control.getInt("R");
		
		if (L > Integer.highestOneBit(L))
			L = Integer.highestOneBit(L);
		control.setValue("L", L);
		
		if (2*R+1 >= L)
			L = N/2 - 1;
		control.setValue("R", R);
		
		spins = new SpinBlocks2D(L, R);
		lattice.resizeLattice(L, L);
		
		N = L*L;
		int spinsInRange = (2*R+1)*(2*R+1) - 1;
		J = 4.0 / spinsInRange;
		E = - N * (J*spinsInRange + h);	// all spins initially 1
		t = 0;
	}
	
	
	public void doStep() {
		T = control.getDouble("T");
		h = control.getDouble("h");	
		double mcsPerDisplay = control.getDouble("MCS per display");
		
		for (int cnt = 0; cnt < N*mcsPerDisplay; cnt++) {
			int x = r.nextInt(L);
			int y = r.nextInt(L);
			int spin = spins.get(x,y);
			double dE = 2*spin*(h + J*(spins.sumInRange(x,y)-spin));
			if (dE <= 0 || r.nextDouble() < Math.exp(-dE/T)) {
				spins.flip(x,y);
				E += dE;
			}
			t++;
		}
		
		lattice.setAll(spins.getAll());
		lattice.setMessage("t = " + t / N);
	}
	
	
	public void reset() {
		control.setValue("L", 512);
		control.setValue("R", 25);
		control.setAdjustableValue("T", "(4/9)*4");
		control.setAdjustableValue("h", -1.34);
		control.setAdjustableValue("MCS per display", 0.5);
	}
	
	
	public static void main(String[] args) {
		SimulationControl c = SimulationControl.createApp(new FastIsing2DApp());
   }
}
