package kip.dyn1D.apps;


import static java.lang.Math.*;
import kip.dyn1D.*;
import scikit.plot.*;
import scikit.jobs.*;

public class NucleationApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);
	boolean phifour = true;
	Dynamics1D sim;
	
	double equilibrationTime = 5;
	// range of time for which to collect nucleating droplets
	double lowBound, highBound;
	// average difference between crude nucleation time, and real nucleation time
	public double overshootEstimate;
	
	public static void main(String[] args) {
		frame(new Control(new NucleationApp()), "Nucleation");
	}
	
	public NucleationApp() {
		
		params.addm("Intervention overshoot", 10.0);
		params.addm("Droplet low bound", 10000.0);
		params.addm("Droplet high bound", 10000.0);
		params.addm("Data path", "");
		
		params.addm("Random seed", 0);
		params.addm("Bin width", 0.5);
		
		if (phifour) {
			params.add("N/R", 50.0);
			params.add("dx/R", 0.5);
			params.addm("R", 1000);
			params.addm("dt", 0.1);
			params.addm("h", 0.223);
			params.addm("\u03b5", -5./9.);
		}
	}
	
	public void animate() {
		fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.copyField()));
		nucTimes.setBinWidth(2, params.fget("Bin width"));
		sim.setParameters(params);
	}
	
	void simulateUntilNucleation() {
		while (!sim.nucleated() && sim.time() < highBound) {
			sim.step();
			yield();
		}
		if (sim.nucleated()) {
			// if (lowBound < sim.t && sim.t < highBound)
			//	droplet.findDroplet(oldoldsim, sim.t-overshootEstimate, sim.dropletLocation());
			nucTimes.accum(2, sim.time());
		}
	}
	
	
	public void run() {
		overshootEstimate	= params.fget("Intervention overshoot");
		lowBound			= params.fget("Droplet low bound");
		highBound			= params.fget("Droplet high bound");		

		if (phifour) {
			sim = new PhiFourth(params);
		}
		sim.initialize(params);
		
		fieldPlot.setXRange(0, sim.N);
		fieldPlot.setYRange(-0.7, 0.1);
		addDisplay(fieldPlot);
		addDisplay(nucTimes);
		
		while (true) {
			sim.initialize(params);
			simulateUntilNucleation();
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}