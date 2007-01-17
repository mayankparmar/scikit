package kip.dyn1D.apps;


import static java.lang.Math.*;
import kip.dyn1D.*;
import scikit.plot.*;
import scikit.jobs.*;

public class NucleationApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
    Histogram profilePlot = new Histogram("Average Droplet Profile", 0.0, true);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);
    
	boolean phifour = true;
	Dynamics1D sim;
	
	// range of time for which to collect nucleating droplets
	double lowBound, highBound;
	// average difference between crude nucleation time, and real nucleation time
	public double overshootEstimate;
	
	public static void main(String[] args) {
		frame(new Control(new NucleationApp()), "Nucleation");
	}
	
	public NucleationApp() {
		params.add("Memory time", 20.0);
		params.add("Droplet low bound", 10000.0);
		params.add("Droplet high bound", 10000.0);
		params.add("Data path", "");
		
		params.addm("Random seed", 0);
		params.addm("Bin width", 0.5);
		
		if (phifour) {
			params.add("N/R", 300.0);
			params.add("dx/R", 1.0);
			params.addm("R", 1000);
			params.addm("dt", 0.1);
			params.addm("h", 0.538);
			params.addm("\u03b5", -1.0);
		}
	}
	
	public void animate() {
		fieldPlot.setDataSet(0, new PointSet(0, (double)sim.dx/sim.R, sim.copyField()));
		nucTimes.setBinWidth(2, params.fget("Bin width"));
		sim.setParameters(params);
	}
    
    void accumulateDroplet(double[] field, double pos) {
        int j = (int)round(pos/sim.dx);
        int c = sim.N/sim.dx;
        for (int i = 0; i < field.length; i++) {
            profilePlot.accum(0, (double)i*sim.dx/sim.R, field[(i+j+c/2)%c]);
        }
    }
    
	void equilibrate() {
		sim.h = -sim.h;
		sim.runUntil(10);
		sim.h = -sim.h;
		sim.resetTime();
	}
	
	void simulateUntilNucleation() {
		while (!sim.nucleated() && sim.time() < highBound) {
			sim.step();
			yield();
		}
		if (sim.nucleated()) {
			if (lowBound < sim.time() && sim.time() < highBound) {
				double[] drop = sim.nucleationTimeAndLocation(overshootEstimate);
                accumulateDroplet(sim.simulationAtTime(drop[0]).copyField(), drop[1]);
            }
			nucTimes.accum(2, sim.time());
		}
	}
	
	
	public void run() {
		overshootEstimate	= params.fget("Memory time")/2;
		lowBound			= params.fget("Droplet low bound");
		highBound			= params.fget("Droplet high bound");		

		if (phifour) {
			sim = new PhiFourth(params);
		}
		sim.initialize(params);
		
        nucTimes.setNormalizing(2, true);
        
		fieldPlot.setXRange(0, (double)sim.N/sim.R);
		fieldPlot.setYRange(-0.7, 0.1);
        fieldPlot.setDataSet(1, new Function(0, (double)sim.N/sim.R) {
            public double eval(double kR) { return 0; }
        });
        
        profilePlot.setBinWidth(1, (double)sim.dx/sim.R);
        profilePlot.setAveraging(0, true);
        
		addDisplay(fieldPlot);
		addDisplay(nucTimes);
		addDisplay(profilePlot);
        
		while (true) {
			sim.initialize(params);
			equilibrate();
			simulateUntilNucleation();
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}