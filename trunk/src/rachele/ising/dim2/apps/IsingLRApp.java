package rachele.ising.dim2.apps;

import kip.ising.dim2.IsingLR;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import static scikit.util.Utilities.format;


public class IsingLRApp extends Simulation {
	public static void main(String[] args) {
		new Control(new IsingLRApp(), "Ising Model");
	}
	
	FieldDisplay fieldDisplay = new FieldDisplay("Coarse Grained Display", true);
	int dx;
	IsingLR sim;
	
	public IsingLRApp() {
		params.addm("Dynamics", new ChoiceValue("Kawasaki Glauber", "Kawasaki Metropolis", "Ising Glauber", "Ising Metropolis"));
		params.addm("Scale colors", new ChoiceValue("False", "True"));
		params.add("Random seed", 0);
		params.add("L", 1<<8);
		params.add("R", 1<<4);
		params.add("Initial magnetization", 0.6);
		params.addm("T", 0.11);
		params.addm("J", -1.0);
		params.addm("h", 0.0);
		params.addm("dt", 0.1);
		params.add("time");
		params.add("magnetization");
		params.add("Lp");
	}
	
	
	public void animate() {
		params.set("time", format(sim.time()));
		//System.out.println("time " + sim.time());
		params.set("magnetization", format(sim.magnetization()));
		sim.setParameters(params);
		params.set("Lp", sim.L/dx);
		
		fieldDisplay.setData(sim.L/dx, sim.L/dx, sim.getField(dx));
		if (params.sget("Scale colors").equals("False"))
			fieldDisplay.setScale(-1, 1);
		else
			fieldDisplay.setAutoScale();
	}
	
	public void clear() {
	}
	
	public void run() {
		Job.addDisplay(fieldDisplay);
		
		sim = new IsingLR(params);
		sim.setField(params.fget("Initial magnetization"));
		dx = Math.max(Integer.highestOneBit(sim.R)/8, 1);
		
		double lastUpdate = 0;
		while (true) {
			while (sim.time() - lastUpdate < 2) {
				sim.step();
				Job.animate();
			}
			lastUpdate = sim.time();
			Job.animate();
		}
	}
}
