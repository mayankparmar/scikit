package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class IsingApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);	
	Ising sim;
	
	
	public static void main(String[] args) {
		frame(new Control(new IsingApp()), "Long Range Ising");
	}
	
	public IsingApp() {
		params.add("Memory time", 1.0, true);
		params.add("N", 1 << 13, true);
		params.add("R", 1 << 9, true);
		params.add("Random seed", 0, false);
		params.add("Bin width", 0.5, false);
		params.add("T", (4.0/9.0)*4.0, false);
		params.add("h", 1.26, false);
		params.add("dt", 0.1, false);
		
		outputs.add("time");
		outputs.add("h_sp");
		outputs.add("h_sp - h");
		outputs.add("psi_sp");
		outputs.add("psi_bg");
	}
	
	public void animate() {
		nucTimes.setBinWidth(2, params.fget("Bin width"));
		sim.setParameters(params);
		outputs.set("time", sim.time());
		
		double J = 4/sim.T;
		double h = sim.h/sim.T;
		
		double s = -abs(h)/h;
		
		double psi_sp = s*sqrt(1 - 1/J);
		double h_sp = (atanh(psi_sp) - J*psi_sp);		
		double dh = h_sp - h;
		double u_bg = sqrt(-dh / (J*J*psi_sp));
		double psi_bg = psi_sp + s*u_bg;
		
		outputs.set("h_sp", sim.T*h_sp);
		outputs.set("h_sp - h", sim.T*dh);
		outputs.set("psi_sp", psi_sp);
		outputs.set("psi_bg", psi_bg);
	}
	
	void simulateUntilNucleation() {
		while (true) {
			sim.step();
			yield();
		}
/*		
		while (!sim.inGrowthMode()) {
			sim.step();
			yield();
		}
		nucTimes.accum(2, sim.time());
*/
	}
	
	public void run() {
		sim = new Ising(params);
		
		fieldPlot.setDataSet(0, new PointSet(0, sim.N, sim.ψ));
		// fieldPlot.setXRange(0, sim.N);
		fieldPlot.setYRange(-1.1, 0.1);
		
		addDisplay(fieldPlot);
		addDisplay(nucTimes);		
		
		while (true) {
			yield();
			
			sim.initialize(params);
			simulateUntilNucleation();
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
