package kip.ising;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
import java.util.Random;


public class FastIsing1DApp extends AbstractSimulation {
	PlotFrame fieldPlot = new PlotFrame("x", "spin", "Ising 1D");
	HistogramFrame clusterDist1 = new HistogramFrame("", "", "Distribution of Cluster Sizes");
	HistogramFrame clusterDist2 = new HistogramFrame("", "", "(Slow) Distribution of Cluster Sizes");
	
	Random r = new Random(0);

	int L, R;
	double E, T, J, h;
	double t;
	SpinBlocks1D spins;
	Clusters1D clusters;
	
	public void initialize() {
		fieldPlot.setPreferredMinMaxY(-1, 1);
		
		L = control.getInt("L");
		R = control.getInt("R");
		T = control.getDouble("T");
		h = control.getDouble("h");	
		
		if (L > Integer.highestOneBit(L))
			L = Integer.highestOneBit(L);
		control.setValue("L", L);
		
		if (2*R+1 >= L)
			R = L/2 - 1;
		control.setValue("R", R);
		
		
		int spinsInRange = 2*R;
		J = 2.0 / spinsInRange;
		E = - L * (J*spinsInRange + h);	// all spins initially 1
		t = 0;
		
		spins = new SpinBlocks1D(L, R);
		clusters = new Clusters1D(spins, 1 - Math.exp(-2*J/T));
	}
	
	
	public void doStep() {
		T = control.getDouble("T");
		h = control.getDouble("h");	
		double mcsPerDisplay = control.getDouble("MCS per display");
		
		for (int cnt = 0; cnt < L*mcsPerDisplay; cnt++) {
			int i = r.nextInt(L);
			int spin = spins.get(i);
			double dE = 2*spin*(h + J*(spins.sumInRange(i)-spin));
			if (dE <= 0 || r.nextDouble() < Math.exp(-dE/T)) {
				spins.flip(i);
				E += dE;
			}
			t++;
		}
		
		fieldPlot.clearData();
		int di = L < 400 ? 1 : (L / 400);
		for (int i = 0; i < L - L%di; i += di) {
			int acc = 0;
			for (int j = 0; j < di; j++)
				acc += spins.get(i+j);
			fieldPlot.append(0, i, (double)acc/di);
		}
		fieldPlot.setMessage("t = " + t / L);
		
		clusters.setBondProbability(1 - Math.exp(-2*J/T));

		for (int i = 0; i < 100; i++) {	
			clusters.buildClusters(true);
			for (int size = 0; size < clusters.L; size++)
				for (int j = 0; j < clusters.numClusters[size]; j++)
					clusterDist1.append(size);

			clusters.buildClusters(false);
			for (int size = 0; size < clusters.L; size++)
				for (int j = 0; j < clusters.numClusters[size]; j++)
					clusterDist2.append(size);
		}
	}
	
	public void reset() {
		control.setValue("L", 128);
		control.setValue("R", 20);
		control.setAdjustableValue("T", 1.5);
		control.setAdjustableValue("h", 0);
		control.setAdjustableValue("MCS per display", 1);
	}
	
		
	public static void main(String[] args) {
		SimulationControl c = SimulationControl.createApp(new FastIsing1DApp());
   }
}
