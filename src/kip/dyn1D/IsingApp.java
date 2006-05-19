package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class IsingApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);	
	Dynamics1D sim;
	
	
	public static void main(String[] args) {
		frame(new Control(new IsingApp()), "Long Range Ising");
	}
	
	public IsingApp() {
		params.add("Dynamics", true, "Standard Ising", "Block Ising", "Field Ising");
		params.add("Memory time", 20.0, true);
		params.add("N", 1 << 13, true);
		params.add("R", 1 << 9, true);
		params.add("Random seed", 0, false);
		params.add("Bin width", 0.5, false);
		params.add("T", (4.0/9.0)*4.0, false);
		params.add("h", 1.26, false);
		params.add("dt", 0.1, false);
		params.add("time", 0.0, false);
		
		outputs.add("h_sp");
		outputs.add("h_sp - h");
		outputs.add("psi_bg");
	}
	
	public void animate() {
		nucTimes.setBinWidth(2, params.fget("Bin width"));
		sim.setParameters(params);
		params.set("time", sim.time());
		
		double T = sim.temperature();
		double J = 4/T;
		double h = sim.externalField()/T;
		double s = -abs(h)/h;
		double psi_sp = s*sqrt(1 - 1/J);
		double h_sp = (atanh(psi_sp) - J*psi_sp);		
		double dh = h_sp - h;
		double u_bg = sqrt(-dh / (J*J*psi_sp));
		double psi_bg = psi_sp + s*u_bg;
		
		outputs.set("h_sp", T*h_sp);
		outputs.set("h_sp - h", T*dh);
		outputs.set("psi_bg", psi_bg);
	}
	
	
	void simulateUntilNucleation() {
		while (!sim.inGrowthMode()) {
			sim.step();
			yield();
		}
		nucTimes.accum(2, sim.time());
		
//		double t = sim.intervention(1.0);
//		System.out.println("found " + t + "\n");
//		Dynamics1D c = sim.simulationAtTime(t);
//		fieldPlot.setDataSet(1, new PointSet(0, sim.N/sim.ψ.length, c.ψ));
	}
	
	public void run() {
		String dyn = params.sget("Dynamics");
		if (dyn.equals("Standard Ising"))
			sim = new Ising(params);
		else if (dyn.equals("Block Ising"))
			sim = new BlockIsing(params);
		else if (dyn.equals("Field Ising"))
			sim = null;
		
		fieldPlot.setDataSet(0, new PointSet(0, sim.systemSize()/sim.ψ.length, sim.ψ));
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
