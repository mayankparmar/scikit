package kip.dyn1D.apps;


import static java.lang.Math.*;
import kip.dyn1D.*;
import scikit.params.ChoiceValue;
import scikit.plot.*;
import scikit.jobs.*;

public class NucleationApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
    Histogram profilePlot = new Histogram("Average Droplet Profile", 0.0, true);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, true);
    
	boolean phifour = true;
	Dynamics1D sim;
	
	// range of time for which to collect nucleating droplets
	double EARLY_END = 15;
    double LATE_BEGIN = 30;
	
	public static void main(String[] args) {
		frame(new Control(new NucleationApp()), "Nucleation");
	}
	
	public NucleationApp() {
		params.add("Memory time", 20.0);
        params.add("Profile type", new ChoiceValue("None", "Early", "Late"));
		params.add("Data path", "");
		
        params.add("Profile count", 0);
		params.addm("Max count", 50000);
        
		params.addm("Random seed", 0);
		params.addm("Bin width", 0.5);
		
		if (phifour) {
			double eps = -1;
			params.add("N/R", 300.0);
			params.add("dx/R", 1.0);
			params.addm("R", 2000);
			params.addm("dt", 0.1);
			params.addm("h", sqrt(-8*eps/27) - 0.005);
			params.addm("\u03b5", eps);
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
			double x = (double)(i-field.length/2)*sim.dx/sim.R;
            profilePlot.accum(0, x, field[(i+j+c/2)%c]);
        }
    }
    
	void equilibrate() {
		sim.h = -sim.h;
		sim.runUntil(10);
		sim.h = -sim.h;
		sim.resetTime();
	}
	
	void simulateUntilNucleation() {
        double lowBound = 0;
        double highBound = Double.MAX_VALUE;
        boolean findProfile = false;
        if (params.sget("Profile type").equals("Early")) {
            highBound = EARLY_END;
            findProfile = true;
        }
        else if (params.sget("Profile type").equals("Late")) {
            lowBound = LATE_BEGIN;
            findProfile = true;
        }
        
		while (!sim.nucleated() && sim.time() < highBound) {
			sim.step();
			yield();
		}
        
        if (lowBound < sim.time() && sim.time() < highBound) {
            if (findProfile) {
                // average difference between crude nucleation time, and real nucleation time
                double overshootEstimate = params.fget("Memory time")/2;
                double[] drop = sim.nucleationTimeAndLocation(overshootEstimate);
                accumulateDroplet(sim.simulationAtTime(drop[0]).copyField(), drop[1]);
            }
            nucTimes.accum(2, sim.time());
            params.set("Profile count", params.iget("Profile count")+1);            
        }
	}
	
	
	public void run() {
		if (phifour) {
			sim = new PhiFourth(params);
		}
		sim.initialize(params);
        
        nucTimes.setNormalizing(2, true);
        
		double N_R = (double)sim.N/sim.R;
		fieldPlot.setXRange(0, N_R);
		fieldPlot.setYRange(-0.7, 0.1);
        fieldPlot.setDataSet(1, new Function(0, N_R) {
            public double eval(double x) { return 0; }
        });
        
        profilePlot.setBinWidth(0, (double)sim.dx/sim.R);
        profilePlot.setAveraging(0, true);
        profilePlot.setDataSet(1, sim.saddleProfile());
        
		addDisplay(fieldPlot);
		addDisplay(nucTimes);
		addDisplay(profilePlot);
        
		while (params.iget("Profile count") < params.iget("Max count")) {
            sim.initialize(params);
            equilibrate();
            simulateUntilNucleation();
            params.set("Random seed", params.iget("Random seed")+1);
            yield();
		}
	}
}
