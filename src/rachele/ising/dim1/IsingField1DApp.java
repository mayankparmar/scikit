package rachele.ising.dim1;


import static java.lang.Math.floor;
import static scikit.util.Utilities.format;
import scikit.dataset.PointSet;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.plot.Plot;
import rachele.ising.dim1.FieldIsing1D;

public class IsingField1DApp extends Simulation{

	Plot fieldPlot = new Plot("Coarse Grained Field", true);
    Plot SFPlot = new Plot("Structure factor", true);
    FieldIsing1D ising;
    StructureFactor1D sf;
	
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
	
	public void animate() {
		params.set("Time", format(ising.t));
		//ising.setParameters(params);
		
		SFPlot.setDataSet(0, sf.getAccumulator());
	
		if (flags.contains("Clear S.F.")) {
			sf.getAccumulator().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void run(){
		ising = new FieldIsing1D(params);
		double binWidth = ising.KR_SP / floor(ising.KR_SP/params.fget("kR bin-width"));
		sf = new StructureFactor1D(ising.Lp, ising.L, ising.R, binWidth);
		fieldPlot.setYRange(-1, 1);
		Job.addDisplay(fieldPlot);
		Job.addDisplay(SFPlot);
		sf.getAccumulator().clear();
		
		while (true) {
			params.set("Time", ising.time());
			ising.simulate();
			sf.accumulate(ising.phi);
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
			fieldPlot.setDataSet(0, new PointSet(0, ising.dx, ising.phi));
		}
	}
	
}
