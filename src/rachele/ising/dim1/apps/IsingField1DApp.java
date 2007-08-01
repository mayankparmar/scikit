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
import java.io.*;


public class IsingField1DApp extends Simulation{

	Plot fieldPlot = new Plot("Coarse Grained Field", true);
    Plot SFPlot = new Plot("Structure factor", true);
    FieldIsing1D ising;
    StructureFactor1D sf;
    public int timeCount;
	
	public static void main(String[] args) {
		new Control(new IsingField1DApp(), "Ising Field 1D");
	}
	
	public IsingField1DApp(){
//	Defoult parameters for nucleation
		params.addm("Zoom", new ChoiceValue("A", "B"));
		params.addm("T", 0.785);
		params.addm("J", -1.0);
		params.addm("dt", 0.1);
		params.addm("R", 100000);
		params.addm("H", 0.07);
		params.add("L/R", 16.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Density", -.3);
		params.add("Time");
		params.add("DENSITY");
		params.add("Lp");

//Default Parameters for clumps
//		params.addm("Zoom", new ChoiceValue("B", "A"));
//		params.addm("T", 0.1);
//		params.addm("J", 1.0);
//		params.addm("dt", 0.1);
//		params.addm("R", 3000);
//		params.addm("H", 0.00);
//		params.add("L/R", 16.0);
//		params.add("R/dx", 16.0);
//		params.add("kR bin-width", 0.1);
//		params.add("Random seed", 0);
//		params.add("Density", 0.0);
//		params.add("Time");
//		params.add("DENSITY");
//		params.add("Lp");		
		
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
	

	public void writeTestFile(){
		try {
			FileWriter fileWriter = new FileWriter ("inputPath.txt", true);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			for (int i = 0; i < ising.Lp; i++){
				writer.write(timeCount + " "+ i + " " + ising.phi[i]);
				writer.newLine();
			}
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	public void run(){
		//readAndPrintFile();
		ising = new FieldIsing1D(params);
		double KR_SP = FieldIsing1D.KR_SP;
		double binWidth = KR_SP / floor(KR_SP/params.fget("kR bin-width"));
		timeCount = -1;
		sf = new StructureFactor1D(ising.Lp, ising.L, ising.R, binWidth);
		fieldPlot.setYRange(-1, 1);
		Job.addDisplay(fieldPlot);
		Job.addDisplay(SFPlot);

		//sf.getAccumulator().clear();
		
		while (true) {
			ising.simulate();
			timeCount += 1;
			sf.accumulate(ising.phi);
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
			SFPlot.setDataSet(0, sf.getAccumulator());
			fieldPlot.setDataSet(0, new PointSet(0, ising.dx, ising.phi));
			//writeTestFile();
		}
	}
	
}
