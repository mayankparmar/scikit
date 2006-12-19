package kip.langevin;


import static java.lang.Math.*;
import scikit.plot.*;
import scikit.jobs.*;

import java.awt.*;


public class Langevin1DApp extends Job {
	Plot fieldPlot = new Plot("Fields", false);
	Histogram nucTimes = new Histogram("Nucleation Times", 0.1, false);
	
//	LangevinDroplet2 droplet = new LangevinDroplet2();
	Langevin1D sim, origsim;
	
	double equilibrationTime = 5;
	// range of time for which to collect nucleating droplets
	double lowBound, highBound;
	// average difference between crude nucleation time, and real nucleation time
	public double overshootEstimate;
	
	public volatile boolean flagSave = false;
	
	
	public static void main(String[] args) {
		Langevin1DApp job = new Langevin1DApp();
		frame(job.fieldPlot, "Fields");
		frame(job.nucTimes, "Nucleation Times");
		
		Control c = new Control(job);
		c.addButton("Save Sim", "flagSave");
		frame(c, "Langevin Simulation");
	}
	
	
	public static Job initApplet(javax.swing.JApplet applet) {
		Langevin1DApp job = new Langevin1DApp();
		
		applet.setLayout(new BorderLayout());
		applet.add(new Control(job), BorderLayout.EAST);
		
		job.fieldPlot.setBorder(javax.swing.BorderFactory.createLineBorder(Color.GRAY));
		job.nucTimes.setBorder(javax.swing.BorderFactory.createLineBorder(Color.GRAY));
		
		Panel p = new Panel();
		p.setLayout(new GridLayout(2, 1));
		p.add(job.fieldPlot);
		p.add(job.nucTimes);
		applet.add(p, BorderLayout.CENTER);
		
		return job;
	}
	
	
	public Langevin1DApp() {
		params.addm("Intervention overshoot", 10.0);
		params.addm("Droplet low bound", 10000.0);
		params.addm("Droplet high bound", 10000.0);
//		params.addm("Data path", "/Users/kbarros/dev/nucleation/droplet_profiles");
		params.addm("Data path", "");
		
		params.addm("Random seed", 0);
		params.addm("Crude cutoff", 0.0);
		params.addm("Bin width", 0.5);
		
		params.addm("h", 0.223);
		params.addm("Length", 50.0);
		params.addm("dx", 0.5);
		params.addm("dt", 0.1);
		params.addm("R\u00b2", 0.1); // R2
		params.addm("\u03b5", -5./9.); // ε
		params.addm("\u0393", 0.005); // Γ
		params.addm("\u03bb", 0.0); // λ
		params.addm("\u03b1", 1.0); // α
	}
	
/*	
	public Langevin1DApp(Parameters params) {
		this.params = params;
	}
*/	
	
	public void animate() {
		nucTimes.setBinWidth(2, params.fget("Bin width"));
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
		Langevin1D oldsim = sim.clone();
		Langevin1D oldoldsim = oldsim;
		
		while (!sim.nucleated() && sim.t < highBound) {
			sim.step();
			yield();
			
			if (flagSave) {
				origsim = sim.clone();
				nucTimes.clear();
				flagSave = false;
			}
			
			if (sim.t - oldsim.t > 4*overshootEstimate) {
				oldoldsim = oldsim;
				oldsim = sim.clone();
			}
			
			if (sim.nucleated()) {
				/*
				if (lowBound < sim.t && sim.t < highBound) {
					droplet.findDroplet(oldoldsim, sim.t-overshootEstimate, sim.dropletLocation());
				}
				*/
				nucTimes.accum(2, sim.t);
			}
		}		
	}
	
	
	public void run() {
		overshootEstimate	= params.fget("Intervention overshoot");
		lowBound			= params.fget("Droplet low bound");
		highBound			= params.fget("Droplet high bound");		
		// droplet.initialize(params);
		sim = new Langevin1D();
		sim.initialize(params);
		
		fieldPlot.setXRange(0, sim.L);
		fieldPlot.setYRange(-0.7, 0.1);
		fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.ψ));
		fieldPlot.setDataSet(1, new PointSet(0, sim.dx, sim.φ));
		addDisplay(fieldPlot);
		addDisplay(nucTimes);
		
		while (sim.t < equilibrationTime) {
			sim.h = -abs(sim.h);
			sim.step();
			yield();
		}
		sim.h = abs(sim.h);
		origsim = sim;
		
		while (true) {
			sim = origsim.clone();
			fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.ψ));
			fieldPlot.setDataSet(1, new PointSet(0, sim.dx, sim.φ));
			
			simulateUntilNucleation();
			
			params.set("Random seed", ""+origsim.incrementRandomSeed());			
		}
	}

}
