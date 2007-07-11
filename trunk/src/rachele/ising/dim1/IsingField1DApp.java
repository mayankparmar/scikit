package rachele.ising.dim1;


import static kip.util.MathPlus.j1;
import static scikit.util.Utilities.format;
import scikit.dataset.Function;
import scikit.dataset.PointSet;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;
import rachele.ising.dim1.AbstractIsing1D;
import rachele.ising.dim1.FieldIsing1D;

public class IsingField1DApp extends Simulation{

	Plot fieldPlot = new Plot("Coarse Grained Field", true);
    //Plot plot = new Plot("Structure factor", true);
    //StructureFactor sf;
    AbstractIsing1D ising;
	
	public static void main(String[] args) {
		new Control(new IsingField1DApp(), "Ising Field 1D");
	}
	
	public IsingField1DApp(){
		//params.addm("Zoom", new ChoiceValue("Yes", "No"));//don't know what this does
		params.addm("T", 0.15);
		params.addm("dt", 1.0);
		params.add("R", 1000);
		params.add("L/R", 16.0);
		params.add("dx", 125.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void run(){
		ising = new FieldIsing1D(params);
		fieldPlot.setYRange(-1, 1);
		Job.addDisplay(fieldPlot);
	
		while (true) {
			params.set("Time", ising.time());
			ising.simulate();
			//fieldPlot.setDataSet(0, new PointSet(0, ising.dx, ising.copyField()));
		}
	}
	
}
