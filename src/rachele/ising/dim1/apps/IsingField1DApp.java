package rachele.ising.dim1.apps;


import static java.lang.Math.floor;
import static scikit.util.Utilities.format;
import scikit.dataset.PointSet;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.Plot;
import rachele.ising.dim1.FieldIsing1D;
import rachele.ising.dim1.StructureFactor1D;

public class IsingField1DApp extends Simulation{

	Plot fieldPlot = new Plot("Coarse Grained Field", true);
    Plot SFPlot = new Plot("Structure factor", true);
    FieldIsing1D ising;
    StructureFactor1D sf;
	
	public static void main(String[] args) {
		new Control(new IsingField1DApp(), "Ising Field 1D");
	}
	
	public IsingField1DApp(){
		params.addm("Zoom", new ChoiceValue("A", "B"));
		params.addm("T", 0.86);
		params.addm("J", 2.0);
		params.addm("dt", 0.1);
		params.addm("R", 2000);
		params.addm("H", 0.07);
		params.add("L/R", 300.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Density", -.3);
		params.add("Time");
		params.add("DENSITY");
	}
	
	public void animate() {
		params.set("Time", format(ising.t));
		ising.readParams(params);
		
		SFPlot.setDataSet(0, sf.getAccumulator());
	
		if (flags.contains("Clear S.F.")) {
			sf.getAccumulator().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void run(){
		ising = new FieldIsing1D(params);
		double KR_SP = FieldIsing1D.KR_SP;
		double binWidth = KR_SP / floor(KR_SP/params.fget("kR bin-width"));
		sf = new StructureFactor1D(ising.Lp, ising.L, ising.R, binWidth);
		fieldPlot.setYRange(-1, 1);
		Job.addDisplay(fieldPlot);
		Job.addDisplay(SFPlot);
		//sf.getAccumulator().clear();
		
		while (true) {
			ising.simulate();
			sf.accumulate(ising.phi);
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
			SFPlot.setDataSet(0, sf.getAccumulator());
			fieldPlot.setDataSet(0, new PointSet(0, ising.dx, ising.phi));
		}
	}
	
}
