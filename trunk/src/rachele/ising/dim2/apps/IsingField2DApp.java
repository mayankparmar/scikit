
package rachele.ising.dim2.apps;

/* The Langevin Dynamics part of this is basically the same as kip.clump.dim2.FiledClump2D.java */


import static java.lang.Math.floor;
import static scikit.util.Utilities.*;
import scikit.dataset.Accumulator;

import java.awt.Color;
import java.io.*;
import rachele.ising.dim2.ConjugateGradientMin;
import rachele.ising.dim2.IsingField2D;
import rachele.ising.dim2.SteepestDescentMin;
import rachele.ising.dim2.StructureFactor;
import scikit.graphics.dim2.Geom2D;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;
import rachele.util.FileUtil;


public class IsingField2DApp extends Simulation {
    Grid grid = new Grid("Phi(x)");
    Grid sfGrid = new Grid("S(k)");
    Grid delPhiGrid = new Grid("DelPhi");
	Plot hSlice = new Plot("Horizontal Slice");
	Plot vSlice = new Plot("Vertical Slice");    
	Plot del_hSlice = new Plot("Horizontal Slice Change");
	Plot del_vSlice = new Plot("Vertical Slice Change");
	Plot sfHor = new Plot("H SF");
	Plot sfVert = new Plot("V SF");
	Plot structurePeakV = new Plot("Ver Structure Factor");
	Plot structurePeakH = new Plot("Hor Structure factor");
	Plot sfPeakBoth = new Plot("Both Structure factors");
	Plot freeEnergyPlot = new Plot("Free Energy");
	Plot freeEnergyTempPlot = new Plot("Free Energy vs Temp");
	Plot landscape = new Plot("Free Energy Landscape");
	Plot brLandscape = new Plot("Br Free Energy Landscape");
	Plot ring = new Plot("Circle SF Ring");
	Plot ringInput = new Plot("Input for Ring");
	StructureFactor sf;
    IsingField2D ising;
    SteepestDescentMin opt;
    ConjugateGradientMin min;
    boolean cgInitialized = false;
	boolean initFile = false;
    Accumulator landscapeFiller;
    Accumulator brLandscapeFiller;
    public int lastClear;

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Ising Field");
	}
	
	public IsingField2DApp() {
		frameTogether("Grids", grid, delPhiGrid);
		//frame(grid);
		//frameTogether("ring", ring, ringInput);
		//frameTogether("landscapes", landscape, brLandscape);
		//frameTogether("Plots", vSlice, sfPlot, structurePeakV, hSlice, sfHPlot, structurePeakH, del_hSlice, del_vSlice, landscape);
		//frameTogether("Slices", vSlice, hSlice, del_hSlice, del_vSlice);
		//frameTogether("SF", structurePeakV, 
			//	 structurePeakH, freeEnergyPlot, sfPeakBoth, sfHor, sfVert);
		frameTogether("SF", structurePeakV, structurePeakH);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Square", "Circle"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Langevin Conserve M","Langevin No M Convervation", "Conjugate Gradient Min", 
				"Steepest Decent" ));
		params.add("Init Conditions", new ChoiceValue("Random Gaussian", 
				"Artificial Stripe 3", "Read From File", "Constant" ));
		params.addm("Approx", new ChoiceValue("Exact Stable",
				"Avoid Boundaries", "Exact SemiStable", "Exact", "Linear",  "Phi4"));
		params.addm("Plot FEvT", new ChoiceValue("Off", "On"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("kR", new DoubleValue(5.135622302, 0.0, 6.0).withSlider());
		params.addm("T", 0.09);
		params.addm("H", 0.0);
		params.addm("dT", 0.001);
		params.addm("tolerance", 0.0001);
		params.addm("dt", 1.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 5.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.7);
		params.add("Time");
		params.add("Mean Phi");
		params.add("Lp");
		params.add("Free Energy");

		flags.add("Write Config");
		flags.add("Clear");
		flags.add("SF");
		//flags.add("Stripe");
		//flags.add("Clump");
		//flags.add("ClearFT");
		//params.addm("Slow CG?", new ChoiceValue( "Yes, some", "No", "Yes, lots"));
		landscapeFiller = new Accumulator(.01);
		brLandscapeFiller = new Accumulator(.01);

	}
	
	public void animate() {

		ising.readParams(params);
		
		if (params.sget("Zoom").equals("Yes")) {
			grid.setAutoScale();
			delPhiGrid.setAutoScale();
		}
		else {
			grid.setScale(-1, 1);
			delPhiGrid.setScale(0, 1);
		}
		
		freeEnergyPlot.setAutoScale(true);
		del_hSlice.setAutoScale(true);
		del_vSlice.setAutoScale(true);
		hSlice.setAutoScale(true);
		vSlice.setAutoScale(true);
		structurePeakV.setAutoScale(true);
		structurePeakH.setAutoScale(true);
		sfPeakBoth.setAutoScale(true);
		landscape.setAutoScale(true);
		brLandscape.setAutoScale(true);
		sfVert.setAutoScale(true);
		sfHor.setAutoScale(true);
		ring.setAutoScale(true);
		

		sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		String dyn = params.sget("Dynamics?");
		landscape.registerLines("FE landscape", landscapeFiller, Color.BLACK);
		brLandscape.registerLines("FE landscape", brLandscapeFiller, Color.BLUE);
		if (dyn.equals("Steepest Decent")) {
			delPhiGrid.registerData(ising.Lp, ising.Lp, opt.direction);
			del_hSlice.registerLines("Slice", opt.get_delHslice(), Color.RED);
			del_vSlice.registerLines("Slice", opt.get_delVslice(), Color.YELLOW);
		} else if (dyn.equals("Conjugate Gradient Min")) {
			delPhiGrid.registerData(ising.Lp, ising.Lp, min.xi);
			del_hSlice.registerLines("Slice", min.get_delHslice(), Color.RED);
			del_vSlice.registerLines("Slice", min.get_delVslice(), Color.YELLOW);			
		} else {
			delPhiGrid.registerData(ising.Lp, ising.Lp, ising.phiVector);
			del_hSlice.registerLines("Slice", ising.get_delHslice(), Color.RED);
			del_vSlice.registerLines("Slice", ising.get_delVslice(), Color.YELLOW);
		}
		
		grid.setDrawables(asList(
				Geom2D.line(0, ising.horizontalSlice, 1, ising.horizontalSlice, Color.GREEN),
				Geom2D.line(ising.verticalSlice, 0, ising.verticalSlice, 1, Color.BLUE)));

		delPhiGrid.setDrawables(asList(
				Geom2D.line(0, ising.horizontalSlice, 1, ising.horizontalSlice, Color.RED),
				Geom2D.line(ising.verticalSlice, 0, ising.verticalSlice, 1, Color.YELLOW)));
		
		hSlice.registerLines("Slice", ising.getHslice(), Color.GREEN);
		vSlice.registerLines("Slice", ising.getVslice(), Color.BLUE);
		freeEnergyPlot.registerLines("Free Energy", ising.getFreeEnergyAcc(), Color.MAGENTA);
		
		if(ising.circleInt() == true){
			ring.registerLines("RING", sf.getRingFT(), Color.black);
			//ringInput.registerLines("Input", sf.getRingInput(), Color.black);
			structurePeakV.registerLines("Peak Value", sf.getPeakC(), Color.BLACK);
		}else{
			structurePeakV.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
			sfPeakBoth.registerLines("Hortizontal Peak", sf.getPeakH(), Color.ORANGE);
			sfPeakBoth.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
			structurePeakH.registerLines("Horizontal Peak", sf.getPeakH(), Color.ORANGE);
			sfHor.registerLines("Hor SF Ave", sf.getAccumulatorHA(), Color.BLACK);
			sfHor.registerLines("Hor SF", sf.getAccumulatorH(), Color.ORANGE);
			sfVert.registerLines("Vert SF Ave", sf.getAccumulatorVA(), Color.BLACK);
			sfVert.registerLines("Vert SF", sf.getAccumulatorV(), Color.CYAN);

		}	
 
		if (flags.contains("Stripe")){
			ising.accumStripeFreeEnergy();
			System.out.println("accum Stripe free energy T = " + ising.T);
		}else if(flags.contains("Clump")){
			ising.accumClumpFreeEnergy();
			System.out.println("accum Clump free energy T = " + ising.T);
		}

		freeEnergyTempPlot.registerLines("Stripe FE", ising.getStripeFreeEnergyAcc(), Color.GREEN);
		freeEnergyTempPlot.registerLines("Clump FE", ising.getClumpFreeEnergyAcc(),Color.PINK);
		freeEnergyTempPlot.registerLines("fe", ising.getEitherFreeEnergyAcc(), Color.BLACK);
		
		if (flags.contains("Clear")){// || lastClear > 1000) {
			ising.getFreeEnergyAcc().clear();
			sf.getPeakH().clear();
			sf.getPeakV().clear();
			sf.getPeakC().clear();
			sf.getPeakHslope().clear();
			sf.getPeakVslope().clear();
			sf.getAccumulatorVA().clear();
			sf.getAccumulatorHA().clear();
			ising.aveCount = 0;
			lastClear = 0;
		}
		if(flags.contains("SF")){
			writeDataToFile();
			System.out.println("SF clicked");
		}
		flags.clear();

//		if(flags.contains("ClearFT")){
//			ising.getClumpFreeEnergyAcc().clear();
//			ising.getStripeFreeEnergyAcc().clear();
//		}
		
	}
	
	public void clear() {
		cgInitialized = false;
		initFile = false;
	}
	
	public void run() {
		if(params.sget("Init Conditions") == "Read From File")
			readInputParams("../../../research/javaData/configs/inputParams");
			
		ising = new IsingField2D(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
		opt = new SteepestDescentMin(ising.phi, ising.verticalSlice, ising.horizontalSlice, ising.dx) {
			public double freeEnergyCalc(double[] point) {
				return ising.isingFreeEnergyCalc(point);
			}
			public double[] steepestAscentCalc(double[] point) {
				return ising.steepestAscentCalc(point);
			}
		};
		
		min = new ConjugateGradientMin(ising.phi,ising.verticalSlice, ising.horizontalSlice, ising.dx) {
			public double freeEnergyCalc(double[] point) {
				return ising.isingFreeEnergyCalc(point);
			}
			public double[] steepestAscentCalc(double[] point) {
				return ising.steepestAscentCalc(point);
			}
		};
        while (true) {
        	ising.readParams(params);
        	if (flags.contains("Write Config"))	writeConfiguration();
			params.set("Time", ising.time());
			params.set("Mean Phi", ising.mean(ising.phi));
			if(params.sget("Dynamics?") == "Conjugate Gradient Min"){
				if(cgInitialized == false){
					min.initialize();   
					System.out.println("CG initialized");
					cgInitialized = true;					
				}
				min.step(ising.t);
				ising.accFreeEnergy.accum(ising.t, min.freeEnergy);
				landscapeFiller = min.getLandscape();					
				brLandscapeFiller = min.getBracketLandscape();
				ising.t += 1;
			}else if(params.sget("Dynamics?") == "Steepest Decent"){
				opt.step();
				ising.t += 1;
				ising.accFreeEnergy.accum(ising.t, opt.freeEnergy);
				cgInitialized = false;
				landscapeFiller = opt.getLandscape();
				brLandscapeFiller = opt.getBracketLandscape();
			}else{
				cgInitialized = false;
				ising.simulate();
			}
			//sf.getAccumulatorV().clear();
			//sf.getAccumulatorH().clear();
			//sf.accumulateAll(ising.time(), ising.coarseGrained());
			if (ising.time()%100 == 0){
				sf.accumulateAll(ising.time(), ising.coarseGrained());
//				//sf.accumMin(ising.coarseGrained(), params.fget("kR"));
				writeDataToFile();
			}
			Job.animate();
		}
 	}
	
	public void readInputParams(String FileName){
		try {
			File inputFile = new File(FileName);
			DataInputStream dis = new DataInputStream(new FileInputStream(inputFile));
			double readData;
			
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
			
			System.out.println("input read");
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	public void writeConfiguration(){
		String configFileName = "../../../research/javaData/configs/inputConfig";
		String inputFileName = "../../../research/javaData/configs/inputParams";
		FileUtil.deleteFile(configFileName);
		FileUtil.deleteFile(inputFileName);
		writeInputParams(inputFileName);	
		writeConfigToFile(configFileName);
	}
	
	public void writeConfigToFile(String FileName){
		try {
			File pathFile = new File(FileName);
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(pathFile, true));
			for (int i = 0; i < ising.Lp*ising.Lp; i ++){
				dos.writeInt(i);
				dos.writeChar('\t');
				dos.writeDouble(ising.phi[i]);
				dos.writeChar('\n');
			}
			dos.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	public void writeInputParams(String FileName){
		try {
			File inputFile = new File(FileName);
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(inputFile, true));
			
			dos.writeDouble(params.fget("J"));
			dos.writeChar('\t');
			dos.writeDouble(params.fget("H"));
			dos.writeChar('\t');
			dos.writeDouble(params.fget("R"));
			dos.writeChar('\t');
			dos.writeDouble(params.fget("L/R"));
			dos.writeChar('\t');
			dos.writeDouble(params.fget("R/dx"));
			dos.writeChar('\t');
			dos.writeChar('\n');
			dos.close();
		}catch(IOException ex){
			ex.printStackTrace();
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
	
	public void writeDataToFile(){
		boolean SvH = true;
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
}
 