package rachele.apps;

import kip.ising.dim2.IsingLR;
import kip.clump.dim2.StructureFactor;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;
import static scikit.util.Utilities.format;


public class IsingLRApp extends Simulation {
	public static void main(String[] args) {
		new Control(new IsingLRApp(), "Ising Model");
	}
	
	FieldDisplay fieldDisplay = new FieldDisplay("Coarse Grained Display", true);
	Plot structureDisplay = new Plot("Structure Factor", true);
	int dx;
	StructureFactor structure;
	IsingLR sim;
	
	public IsingLRApp() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis", "Kawasaki Glauber", "Kawasaki Metropolis"));
		params.add("Random seed", 1);
		params.add("L", 1<<8);
		params.add("R", 1<<4);
		params.addm("T", 4.0/9.0);
		params.addm("J", 1.0);
		params.addm("h", -0.37);
		params.addm("dt", 0.1);
		params.add("time");
	}
	
	
	public void animate() {
		params.set("time", format(sim.time()));
		sim.setParameters(params);
		
		fieldDisplay.setData(sim.L/dx, sim.L/dx, sim.getField(dx));
		fieldDisplay.setScale(-1, 1);
		
		structureDisplay.setDataSet(0, structure.getAccumulator());
	}
	
	
	public void run() {
		Job.addDisplay(fieldDisplay);
		Job.addDisplay(structureDisplay);
		
		sim = new IsingLR(params);
		dx = Math.max(Integer.highestOneBit(sim.R)/8, 1);
		structure = new StructureFactor(sim.L/dx, sim.L, sim.R, 0.1);
		
		double lastUpdate = 0;
		while (true) {
			while (sim.time() - lastUpdate < 2) {
				sim.step();
				Job.animate();
			}
			lastUpdate = sim.time();
			structure.getAccumulator().clear();
			structure.accumulate(sim.getField(dx));
			Job.animate();
		}
	}
}
