package rachele.ising.dim2.apps;


import static java.lang.Math.floor;
import static scikit.util.Utilities.*;
import rachele.ising.dim2.IsingField2Dopt;
import rachele.ising.dim2.StructureFactor;
//import rachele.util.FileUtil;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;

public class IsingField2DoptApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
    Grid sfGrid = new Grid("S(k)");
	StructureFactor sf;
    IsingField2Dopt ising;
	boolean initFile = false;
    
    public static void main(String[] args) {
		new Control(new IsingField2DoptApp(), "Ising Field");
	}
	
	public IsingField2DoptApp(){
		frameTogether("Grids", grid, sfGrid);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Square", "Circle"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Langevin Conserve M","Langevin No M Convervation"));
		params.addm("Plot FEvT", new ChoiceValue("Off", "On"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("T", 0.09);
		params.addm("H", 0.0);
		params.addm("tolerance", 0.0001);
		params.addm("dt", 1.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 10.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.7);
		params.add("Time");
		//params.add("Lp");
		params.add("vert sf");
		params.add("hor sf");
		
		flags.add("Write Config");
		flags.add("Clear");
		flags.add("SF");

	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes")) 
			grid.setAutoScale();
		else 
			grid.setScale(-1, 1);
		sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		params.set("vert sf", sf.peakValueV());
		params.set("hor sf", sf.peakValueH());
	}
	
	public void clear() {
	}
	
	public void run() {
		ising = new IsingField2Dopt(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2Dopt.KR_SP / floor(IsingField2Dopt.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
        while (true) {
        	ising.readParams(params);
			params.set("Time", ising.time());
			ising.simulate();
			//writeDataToFile();
			//sf.accumulateAll(ising.time(), ising.coarseGrained());
			Job.animate();
		}
 	}
//	public void writeDataToFile(){
//		boolean SvH = false;
//		if (params.sget("Interaction")=="Square"){
//				String dataFileV = "../../../research/javaData/sfData/dataFileV";
//				String dataFileH = "../../../research/javaData/sfData/dataFileH";
//				if (initFile == false){
//					initFile(dataFileV, SvH);
//					initFile(dataFileH, SvH);
//					initFile = true;
//				}
//				if (SvH){
//					FileUtil.printlnToFile(dataFileH, ising.H, sf.peakValueH(), ising.freeEnergy, ising.time());
//					FileUtil.printlnToFile(dataFileV, ising.H, sf.peakValueV(), ising.freeEnergy, ising.time());					
//				}else{
//					FileUtil.printlnToFile(dataFileH, ising.T, sf.peakValueH(), ising.freeEnergy, ising.time());
//					FileUtil.printlnToFile(dataFileV, ising.T, sf.peakValueV(), ising.freeEnergy, ising.time());
//				}			
//				System.out.println("Data written to file for time = " + ising.time());
//		}else if(params.sget("Interaction")== "Circle"){
//			String dataStripe = "../../../research/javaData/sfData/dataStripe";
//			String dataClump = "../../../research/javaData/sfData/dataClump";
//			if (initFile == false){
//				initFile(dataStripe, SvH);
//				initFile(dataClump, SvH);
//				initFile = true;
//			}
//			if(SvH){
//				FileUtil.printlnToFile(dataClump, ising.H, sf.peakValueC(), ising.freeEnergy, ising.time());
//				FileUtil.printlnToFile(dataStripe, ising.H, sf.peakValueS(), ising.freeEnergy, ising.time());					
//			}else{
//				FileUtil.printlnToFile(dataClump, ising.T, sf.peakValueC(), ising.freeEnergy, ising.time());
//				FileUtil.printlnToFile(dataStripe, ising.T, sf.peakValueS(), ising.freeEnergy, ising.time());
//			}
//			System.out.println("Data written to file for time = " + ising.time());
//		}
	
}
