package rachele.ising.dim2.apps;


import static java.lang.Math.floor;
import static scikit.util.Utilities.*;
//import rachele.ising.dim2.IsingField2D;
import rachele.ising.dim2.IsingField2Dopt;
import rachele.ising.dim2.StructureFactor;
import rachele.util.FileUtil;
//import rachele.ising.dim2.StructureFactor;
//import rachele.util.FileUtil;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
//import scikit.dataset.Accumulator;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;
import scikit.graphics.dim2.Plot;
//import java.awt.Color;

public class IsingField2DoptApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
    Grid delPhiGrid = new Grid("delPhi(x)");
    Grid sfGrid = new Grid("S(k)");
    Plot fePlot = new Plot("Free Energy");
	StructureFactor sf;
    IsingField2Dopt ising;
	boolean initFile = false;
	//Accumulator freeEnergy;
    
    public static void main(String[] args) {
		new Control(new IsingField2DoptApp(), "Ising Field");
	}
	
	public IsingField2DoptApp(){
		frameTogether("Grids", grid, sfGrid, delPhiGrid, fePlot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Theory", new ChoiceValue("Slow Near Edge", "Exact", "Dynamic dt"));
		params.addm("Dynamics?", new ChoiceValue("Langevin No M Convervation", "Langevin Conserve M"));
		params.addm("Noise", new DoubleValue(0, 0, 1.0).withSlider());
		params.addm("T", 0.08);
		params.addm("H", 0.0);
		params.addm("Rx", 400.0);
		params.addm("Ry", 400.0);
		params.add("L", 1000.0);
		params.add("dx", 10.0);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.0);
		params.addm("range change", 0.01);
		params.add("dt", 1.0);
		params.add("Time");
		params.add("Free Energy");
		params.add("Pot");
		params.add("Ent");
		flags.add("Write Config");
		flags.add("Record FE");
		flags.add("Clear");

	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes"))grid.setAutoScale();
		else grid.setScale(-1, 1);
		fePlot.setAutoScale(true);
		//freeEnergy.accum(ising.t, ising.freeEnergy);
		//fePlot.registerLines("FE", freeEnergy, Color.RED);
		//sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		delPhiGrid.registerData(ising.Lp, ising.Lp, ising.phiVector);
		params.set("Rx", ising.Rx);
		params.set("Ry", ising.Ry);
		params.set("Free Energy", ising.freeEnergy);
		params.set("Pot", ising.potAccum);
		params.set("Ent", ising.entAccum);
		params.set("dt", ising.dt);
		//if(flags.contains("Clear")) freeEnergy.clear();
		if(flags.contains("Record FE")) recordTvsFE();
		if(ising.recordTvsFE == true){
			recordTvsFE();
			double newT = ising.T - 0.001;
			params.set("T", newT);			
			ising.recordTvsFE = false;
		}else if(ising.recordHvsFE){
			recordHvsFE();
			double newH = ising.H - 0.001;
			params.set("H", newH);			
			ising.recordHvsFE = false;			
		}
		flags.clear();
	}
	
	public void clear() {
	}
	
	public void run() {
		ising = new IsingField2Dopt(params);
		double binWidth = 0.1;//params.fget("kR bin-width");
		binWidth = IsingField2Dopt.KR_SP / floor(IsingField2Dopt.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.Rx, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		//int maxi=sf.clumpsOrStripes(ising.phi);
		//freeEnergy = new Accumulator(1.0);
		int steps = 1;
		if (ising.t < 500.0){
			params.set("Time", ising.time());
			ising.simulate();
			ising.adjustRanges();			
		}
		steps = 500;
        while (true) {
        	ising.readParams(params);
			params.set("Time", ising.time());
			ising.simulate();
			ising.adjustRanges();
//			if (ising.time()%10 == 0){
//				boolean circleOn=false;
//				sf.accumulateMelt(circleOn, ising.phi, maxi);
//				writeDataToFile();
//			}
			if (ising.t > steps){
				Job.animate();
				steps += 1;
			}
		}
 	}
	
	public void recordTvsFE(){
		String file = "../../../research/javaData/feData/fe";
		FileUtil.printlnToFile(file, ising.T, ising.freeEnergy);
		System.out.println("Wrote to file: T = " + ising.T + " FE = " + ising.freeEnergy);
	}

	public void recordHvsFE(){
		String file = "../../../research/javaData/feData/fe";
		FileUtil.printlnToFile(file, ising.H, ising.freeEnergy);
		System.out.println("Wrote to file: H = " + ising.H + " FE = " + ising.freeEnergy);
	}
	
	public void writeDataToFile(){
		boolean SvH = false;
		if (params.sget("Interaction")=="Square"){
				String dataFileV = "../../../research/javaData/sfData/dataFileV";
				String dataFileH = "../../../research/javaData/sfData/dataFileH";
				if (initFile == false){
					initFile(dataFileV, SvH);
					initFile(dataFileH, SvH);
					initFile = true;
				}
				if (SvH){
					FileUtil.printlnToFile(dataFileH, ising.H, sf.peakValueH(), ising.freeEnergy, ising.time());
					FileUtil.printlnToFile(dataFileV, ising.H, sf.peakValueV(), ising.freeEnergy, ising.time());					
				}else{
					FileUtil.printlnToFile(dataFileH, ising.T, sf.peakValueH(), ising.freeEnergy, ising.time());
					FileUtil.printlnToFile(dataFileV, ising.T, sf.peakValueV(), ising.freeEnergy, ising.time());
				}			
				System.out.println("Data written to file for time = " + ising.time());
		}else if(params.sget("Interaction")== "Circle"){
			String dataStripe = "../../../research/javaData/sfData/dataStripe";
			String dataClump = "../../../research/javaData/sfData/dataClump";
			if (initFile == false){
				initFile(dataStripe, SvH);
				initFile(dataClump, SvH);
				initFile = true;
			}
			if(SvH){
				FileUtil.printlnToFile(dataClump, ising.H, sf.peakValueC(), ising.freeEnergy, ising.time());
				FileUtil.printlnToFile(dataStripe, ising.H, sf.peakValueS(), ising.freeEnergy, ising.time());					
			}else{
				FileUtil.printlnToFile(dataClump, ising.T, sf.peakValueC(), ising.freeEnergy, ising.time());
				FileUtil.printlnToFile(dataStripe, ising.T, sf.peakValueS(), ising.freeEnergy, ising.time());
			}
			System.out.println("Data written to file for time = " + ising.time());
		}
	}
	
	public void initFile(String file, boolean SvH){
		FileUtil.deleteFile(file);
		if(SvH){
			FileUtil.printlnToFile(file, " # SF vs H data ");			
			FileUtil.printlnToFile(file, " # Temperature = ", ising.T);
			FileUtil.printlnToFile(file, " # Data = H, S(k*), Free Energy, time");
		}else{
			FileUtil.printlnToFile(file, " # SF vs T data ");				
			FileUtil.printlnToFile(file, " # External field = ", ising.H);
			FileUtil.printlnToFile(file, " # Data = H, S(k*), Free Energy, time");
		}
		FileUtil.printlnToFile(file, " # Density = ", ising.DENSITY);		
	}
}
