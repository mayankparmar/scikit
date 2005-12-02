package kip.next;


import static java.lang.Math.*;

import scikit.plot.Plot;
import scikit.plot.PointSet;
import scikit.plot.Accumulator;
import scikit.jobs.Job;
import scikit.jobs.Control;
import scikit.jobs.Parameters;



public class Langevin1DApp2 extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Plot nuclnPlot = new Plot("Nucleation Times", true);
	Accumulator nucTimes;
	
//	LangevinDroplet2 droplet = new LangevinDroplet2();
	Langevin1D2 sim = new Langevin1D2(), oldsim, oldoldsim;
	
	double equilibrationTime = 5;
	// range of time for which to collect nucleating droplets
	double lowBound, highBound;
	// average difference between crude nucleation time, and real nucleation time
	public double overshootEstimate;
	
	
	
	public static void main(String[] args) {
		new Control(new Langevin1DApp2(), "Langevin Simulation");
	}
	
	
	public Langevin1DApp2() {
		params.add("Intervention overshoot", 10);
		params.add("Droplet low bound", 10000);
		params.add("Droplet high bound", 10000);
		params.add("Data path", "/Users/kbarros/dev/nucleation/droplet_profiles");
		
		params.add("Random seed", 0);
		params.add("Crude cutoff", 0.0);
		params.add("Bin width", 0.5);
		
		params.add("h", 0.223);
		params.add("Length", 50.0);
		params.add("dx", 0.5);
		params.add("dt", 0.1);
		params.add("R\u00b2", 0.1); // R2
		params.add("\u03b5", -5./9.); // ε
		params.add("\u0393", 0.005); // Γ
		params.add("\u03bb", 0.0); // λ
		params.add("\u03b1", 1.0); // α
/*
		params.put("\u03bb", 0); // λ
		params.put("h", 0.223);
		params.put("Length", 1000);
		params.put("dx", 10);
		params.put("dt", 0.1);
		params.put("R\u00b2", 100); // R2
		params.put("\u03b5", "-5/9"); // ε
		params.put("\u03b1", 1); // α
		params.put("\u0393", 0.5); // Γ
*/		
	}
	
	
	public Langevin1DApp2(Parameters params) {
		this.params = params;
	}
	
	
	public void animate() {
		nucTimes.setBinWidth(params.fget("Bin width"));
		sim.getParameters(params);
	}
	
	/*
	public void copySeed() {
		for (int i = 0; i < sim.N; i++) {
			sim.ψ[i] = droplet.getψ(i);
			sim.φ[i] = droplet.getφ(i);
		}
		sim.t = 0;
		state = State.metastable;
		oldoldsim = oldsim = (Langevin1D)sim.clone();
		sim.ψcutoff = 10; // no more nucleation!
		fieldPlot.repaint();
	}
	*/	
	
	
	void simulateUntilNucleation() {
		while (sim.t < equilibrationTime) {
			sim.h = -abs(sim.h);
			sim.step();
			yield();
		}
		
		sim.h = abs(sim.h);
		oldoldsim = oldsim = (Langevin1D2)sim.clone();
		
		while (!sim.nucleated() && sim.t < highBound) {
			sim.step();
			yield();
			
			if (sim.t - oldsim.t > 4*overshootEstimate) {
				oldoldsim = oldsim;
				oldsim = (Langevin1D2)sim.clone();
			}
			
			if (sim.nucleated()) {
				/*
				if (lowBound < sim.t && sim.t < highBound) {
					droplet.findDroplet(oldoldsim, sim.t-overshootEstimate, sim.dropletLocation());
				}
				*/
				nucTimes.accum(sim.t);
			}
		}
	}
	
	
	public void run() {
		overshootEstimate	= params.fget("Intervention overshoot");
		lowBound			= params.fget("Droplet low bound");
		highBound			= params.fget("Droplet high bound");		
		sim.initialize(params);
		// droplet.initialize(params);
		
		fieldPlot.setXRange(0, sim.L);
		fieldPlot.setYRange(-0.7, 0.1);
		fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.ψ));
		fieldPlot.setDataSet(1, new PointSet(0, sim.dx, sim.φ));
		addDisplay(fieldPlot);
		
		nucTimes = new Accumulator(params.fget("Bin width"));
		nuclnPlot.setDataSet(2, nucTimes);
		nuclnPlot.setStyle(2, Plot.Style.BARS);
		addDisplay(nuclnPlot);
		
		while (true) {
			sim.initialize(params);
			simulateUntilNucleation();
			params.set("Random seed", ++sim.randomSeed);			
		}
	}
	
	
	public void dispose() {
	}

}
