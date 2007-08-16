package rachele.ising.dim1.apps;

import static java.lang.Math.floor;
import static scikit.util.Utilities.format;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import rachele.ising.dim1.PathSample1D;
import rachele.ising.dim1.StructureFactor1D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;
import scikit.plot.Plot;
import scikit.plot.FieldDisplay;

public class SaddlePoint1DApp extends Simulation{

	FieldDisplay grid = new FieldDisplay("Time vs Coarse Grained Field", true);
    //Plot SFPlot = new Plot("Structure factor", true);
	Plot timeSlice = new Plot("Configuration at Time Slice", true);
	Plot spaceSlice = new Plot("Path at Space Slice", true);
    Plot actionPlot = new Plot("Action", true);
    Plot freeEngPlot = new Plot("Free Energy", true);
	PathSample1D sim;
    StructureFactor1D sf;
	
	
	public static void main(String[] args) {
		new Control(new SaddlePoint1DApp(), "Saddle Point 1D");
	}

	public SaddlePoint1DApp(){
		params.addm("Sampling Noise", new ChoiceValue("On", "Off"));
		params.addm("Initial Conditions", new ChoiceValue("Noisy", "Artificial Droplett", "Read In", "Boundaries Only", "Step", "Constant Slope"));
		params.addm("Time to slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Position to slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("du", 0.004);
		params.addm("T", 0.85);
		params.addm("J", -1.0);
		params.addm("H", 0.0);
		params.addm("R", 100000.0);
		params.add("L/R", 8.0);
		params.add("R/dx", 4.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		//params.add("Density", -0.3);
		params.add("dt", 0.1);
		params.add("Time Interval", 60.0);
		params.add("Time Allocation");
		params.add("Lp");
		params.add("u");
		params.add("action");
		params.add("init denisty", -0.63);
		params.add("fin density", 0.63);
		params.addm("adjust term1", 1);
		params.addm("adjust term2", 1);
	}
	
	public void animate() {
		params.set("u", format(sim.u));
		params.set("action", format(sim.S));
		
		sim.measureAction();

		freeEngPlot.setDataSet(0, sim.getFreeEnergy());
		timeSlice.setDataSet(0, sim.getTimeSlice());
		spaceSlice.setDataSet(0, sim.getSpaceSlice());
		actionPlot.setDataSet(0, sim.getAccumulator());
		grid.setData(sim.Lp, sim.t_f, sim.copyField());
		sim.readParams(params);
		
		//SFPlot.setDataSet(0, sf.getAccumulator());
	
		//if (flags.contains("Clear S.F.")) {
		//	sf.getAccumulator().clear();
		//	System.out.println("clicked");
		//}
		//flags.clear();
	}
	
	public void readInputParams(String FileName){
		try {
			File inputFile = new File(FileName);
			DataInputStream dis = new DataInputStream(new FileInputStream(inputFile));
			double readData;
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("T", readData);
			dis.readChar();
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("J", readData);
			dis.readChar();				
		
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("H", readData);
			dis.readChar();
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("R", readData);
			dis.readChar();	
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("L/R", readData);
			dis.readChar();
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("R/dx", readData);
			dis.readChar();	
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("dt", readData);
			dis.readChar();
			
			readData = dis.readDouble();
			System.out.println(readData);
			params.set("Time Interval", readData);
			dis.close();
			System.out.println("input read");
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	public void run(){

		if(params.sget("Initial Conditions") == "Read In")
			readInputParams("racheleDataFiles/inputParams");
		if(params.sget("Initial Conditions") == "Boundaries Only")
			readInputParams("racheleDataFiles/inputParams");		
		sim = new PathSample1D(params);
		double KR_SP = PathSample1D.KR_SP;
		double binWidth = KR_SP / floor(KR_SP/params.fget("kR bin-width"));
		sf = new StructureFactor1D(sim.Lp, sim.L, sim.R, binWidth);
		Job.addDisplay(grid);
		Job.addDisplay(timeSlice);
		Job.addDisplay(spaceSlice);
		Job.addDisplay(actionPlot);
		Job.addDisplay(freeEngPlot);
		//timeSlice.setDataSet(0, sim.getTimeSlice());
		//spaceSlice.setDataSet(0, sim.getSpaceSlice());
		//actionPlot.setDataSet(0, sim.getAccumulator());
        //grid.setData(sim.Lp, sim.t_f, sim.copyField());
		//freeEngPlot.setDataSet(0, sim.getFreeEnergy());
        //Job.addDisplay(SFPlot);
		//sf.getAccumulator().clear();
		
		while (true) {
			sim.simulate();
			//sf.accumulate(sim.phi);
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
			//SFPlot.setDataSet(0, sf.getAccumulator());
		}
	}
	
}
