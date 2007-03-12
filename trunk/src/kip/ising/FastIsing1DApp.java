package kip.ising;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;


public class FastIsing1DApp extends AbstractSimulation {
	PlotFrame fieldPlot = new PlotFrame("x", "spin", "Ising 1D");
	HistogramFrame clusterDist1 = new HistogramFrame("", "", "Distribution of Cluster Sizes");

	FastIsing1D ising = new FastIsing1D();
	
	public void initialize() {
		fieldPlot.setPreferredMinMaxY(-1, 1);
		ising.initialize(control);
	}
	
	
	public void doStep() {
		ising.getParameters(control);
		ising.step();
		
		fieldPlot.clearData();
		int spins[] = ising.spins();
		int L = ising.length();
		int di = L < 100 ? 1 : (L / 100);
		for (int i = 0; i < L - L%di; i += di) {
			int acc = 0;
			for (int j = 0; j < di; j++)
				acc += spins[i+j];
			fieldPlot.append(0, i, (double)acc/di);
		}
		fieldPlot.setMessage("t = " + ising.time());
		
/*		
		clusters.setBondProbability(1 - Math.exp(-2*J/T));
		for (int i = 0; i < 100; i++) {	
			clusters.buildClusters(true);
			for (int size = 0; size < clusters.L; size++)
				for (int j = 0; j < clusters.numClusters[size]; j++)
					clusterDist1.append(size);
		}
*/
	}
	
	public void reset() {
		control.setValue("L", 1 << 18);
		control.setValue("R", 1 << 12);
		control.setValue("Random seed", 0);
		control.setAdjustableValue("T", 1.53);
		control.setAdjustableValue("h", -0.16);
		control.setAdjustableValue("dt", 0.1);
	}
	
		
	public static void main(String[] args) {
		SimulationControl.createApp(new FastIsing1DApp());
   }
}
