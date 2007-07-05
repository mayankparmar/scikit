package rachele.ising.dim2.apps;

import rachele.ising.dim2.StructureFactor;
import kip.ising.dim2.IsingLR;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;
import static scikit.util.Utilities.format;


public class IsingLRStructApp extends Simulation {
	public static void main(String[] args) {
		new Control(new IsingLRStructApp(), "Ising Model");
	}
	
	FieldDisplay fieldDisplay = new FieldDisplay("Coarse Grained Display", true);
	Plot structureDisplay = new Plot("Structure Factor - Vert and Hor Components", true);
	Plot circleStructureDisplay = new Plot("Structure Factor - Circle Average", true);

	int dx;
	StructureFactor structure;
	IsingLR sim;
	//Accumulator avStructH;
	//Accumulator avStructV;
	//Accumulator avStructC;
	
	public IsingLRStructApp() {
		params.addm("Dynamics", new ChoiceValue("Kawasaki Glauber", "Kawasaki Metropolis", "Ising Glauber", "Ising Metropolis"));
		params.addm("Scale colors", new ChoiceValue("False", "True"));
		params.add("Random seed", 0);
		params.add("L", 1<<8);
		params.add("R", 1<<4);
		params.add("Initial magnetization", 0.6);
		params.addm("T", 0.11);
		params.addm("J", -1.0);
		params.addm("h", 0.0);
		params.addm("dt", 1.0);
		params.addm("init time", 50);
		params.add("time");
		params.add("magnetization");
		
		flags.add("Clear S.F.");
	}
	
	
	public void animate() {
		params.set("time", format(sim.time()));
		params.set("magnetization", format(sim.magnetization()));
		sim.setParameters(params);
		
		fieldDisplay.setData(sim.L/dx, sim.L/dx, sim.getField(dx));
		if (params.sget("Scale colors").equals("False"))
			fieldDisplay.setScale(-1, 1);
		else
			fieldDisplay.setAutoScale();
		structureDisplay.setDataSet(0, structure.getAccumulatorV());
		structureDisplay.setDataSet(2, structure.getAccumulatorH());
		structureDisplay.setDataSet(1, structure.getAccumulatorVA());	
		structureDisplay.setDataSet(3, structure.getAccumulatorHA());
		circleStructureDisplay.setDataSet(0, structure.getAccumulatorC());	
		circleStructureDisplay.setDataSet(1, structure.getAccumulatorCA());	
	
		if (flags.contains("Clear S.F.")) {
			// structure.getAccumulatorC().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	
	public void run() {
		Job.addDisplay(fieldDisplay);
		Job.addDisplay(structureDisplay);
		Job.addDisplay(circleStructureDisplay);
		
		sim = new IsingLR(params);
		sim.setField(params.fget("Initial magnetization"));
		dx = Math.max(Integer.highestOneBit(sim.R)/8, 1);
		structure = new StructureFactor(sim.L/dx, sim.L, sim.R, 0.1);
		//avStructH = new Accumulator(sim.L/dx);
		//avStructV = new Accumulator(sim.L/dx);
		//avStructC = new Accumulator(sim.L/dx);
		//avStructH.setAveraging(true);
		//avStructV.setAveraging(true);
		//avStructC.setAveraging(true);
		
		System.out.println("equilibrating");
		while (sim.time() < params.fget("init time")) {
			sim.step();
			Job.animate();
		}
		
		System.out.println("running");
		double lastUpdate = 0;
		while (true) {
			while (sim.time() - lastUpdate < 2) {
				sim.step();
				Job.animate();
			}
			lastUpdate = sim.time();
			structure.getAccumulatorH().clear();
			structure.getAccumulatorV().clear();			
			structure.getAccumulatorC().clear();
			structure.accumulateAll(sim.getField(dx));
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
		}
	}
}
