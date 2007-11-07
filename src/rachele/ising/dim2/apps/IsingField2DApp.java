
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
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;


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
		frame(ring);
		//frameTogether("landscapes", landscape, brLandscape);
		//frameTogether("Plots", vSlice, sfPlot, structurePeakV, hSlice, sfHPlot, structurePeakH, del_hSlice, del_vSlice, landscape);
		//frameTogether("Slices", vSlice, hSlice, del_hSlice, del_vSlice);
		//frameTogether("SF", structurePeakV, 
			//	 structurePeakH, freeEnergyPlot, sfPeakBoth, sfHor, sfVert);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Langevin No M Convervation", "Conjugate Gradient Min", 
				"Steepest Decent",  "Langevin Conserve M"));
		params.add("Init Conditions", new ChoiceValue("Random Gaussian", 
				"Artificial Stripe 3", "Read From File", "Constant" ));
		params.addm("Approx", new ChoiceValue("Exact Stable",
				"Avoid Boundaries", "Exact SemiStable", "Exact", "Linear",  "Phi4"));
		params.addm("Plot FEvT", new ChoiceValue("Off", "On"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("kR", new DoubleValue(5.135622302, 0.0, 6.0).withSlider());
		params.addm("T", 0.05);
		params.addm("H", 0.3);
		params.addm("dT", 0.001);
		params.addm("tolerance", 0.0001);
		params.addm("dt", 1.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 4.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.0);
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
		params.addm("Slow CG?", new ChoiceValue( "Yes, some", "No", "Yes, lots"));
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
		
		//if (flags.contains("Clear")){// || lastClear > 1000) {
			ising.getFreeEnergyAcc().clear();
			sf.getPeakH().clear();
			sf.getPeakV().clear();
			sf.getPeakC().clear();
			sf.getPeakHslope().clear();
			sf.getPeakVslope().clear();
			sf.getAccumulatorVA().clear();
			sf.getAccumulatorHA().clear();
			
			ising.aveCount = 0;
			//System.out.println("cleared");
			lastClear = 0;
		//}
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
		
        boolean equilibrating = true;
        //int feCounter = 0;  //counts iterations before taking FE vs T data point
        
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
				//double minPhi = DoubleArray.min(ising.phi);
				//double maxPhi = DoubleArray.max(ising.phi);
				//System.out.println("min = " + minPhi);
				//System.out.println("max = " + maxPhi);
			}else if(params.sget("Dynamics?") == "Steepest Decent"){
				opt.step();
				//ising.getPhiFrA();
				ising.t += 1;
				ising.accFreeEnergy.accum(ising.t, opt.freeEnergy);
				cgInitialized = false;
				landscapeFiller = opt.getLandscape();
				brLandscapeFiller = opt.getBracketLandscape();
			}else{
				cgInitialized = false;
				ising.simulate();
				//sf.accumulateAll(ising.t, ising.phi);
				
//				if(params.sget("Plot FEvT")=="On"){
//					feCounter += 1;
//					if(feCounter == 500){
//						//ising.accumEitherFreeEnergy();
//						feCounter = 0;
//						System.out.println( feCounter + " " + ising.t + " Free Energy accum at " + ising.T);
//						ising.changeT(); 
//					}
//				}

			}
			if (equilibrating && ising.time() >= .5) equilibrating = false;
			sf.getAccumulatorV().clear();
			sf.getAccumulatorH().clear();
			//sf.accumulateAll(ising.time(), ising.coarseGrained());

			lastClear += 1;
			if (ising.time()%1 == 0){
				sf.accumMin(ising.coarseGrained(), params.fget("kR"));
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
		deleteFile(configFileName);
		deleteFile(inputFileName);
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
	
	public void deleteFile(String fileName){
		File file = new File(fileName);
		boolean success = file.delete();
		if (success)
			System.out.println("File deleted");
		else
			System.out.println("File delete failed");			
	}

	public void writeDataToFile(){
		if (params.sget("Interaction")=="Square"){
			try{
				String dataFileV = "../../../research/javaData/sfData/dataFileV";
				String dataFileH = "../../../research/javaData/sfData/dataFileH";
				if (initFile == false){
					deleteFile(dataFileH);
					deleteFile(dataFileV);
					File fileV = new File(dataFileV);
					File fileH = new File(dataFileH);
					PrintWriter outV = new PrintWriter(new FileWriter(fileV, true), true);
					PrintWriter outH = new PrintWriter(new FileWriter(fileH, true), true);
					outH.println(" # SF vs H data ");
					outV.println(" # SF vs V data ");
					outV.println(" # Temperature = " + ising.T);
					outH.println(" # Temperature = " + ising.T);
					initFile = true;
				}
				File fileV = new File(dataFileV);
				File fileH = new File(dataFileH);
				PrintWriter outV = new PrintWriter(new FileWriter(fileV, true), true);
				PrintWriter outH = new PrintWriter(new FileWriter(fileH, true), true);
				outH.println(ising.H + " " + sf.peakValueH() + " " +ising.freeEnergy + " " + ising.time());
				outV.println(ising.H + " " + sf.peakValueV() + " " +ising.freeEnergy + " " + ising.time());
				System.out.println("Data written to file for time = " + ising.time());
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}else if(params.sget("Interaction")== "Circle"){
			try{
				String dataFileMax = "../../../research/javaData/sfData/dataMax";
				//String dataFileMin = "../../../research/javaData/sfData/dataMin";
				if (initFile == false){
					deleteFile(dataFileMax);
					//deleteFile(dataFileMin);
					File fileMax = new File(dataFileMax);
					//File fileMin = new File(dataFileMin);
					PrintWriter outMax = new PrintWriter(new FileWriter(fileMax, true), true);
					//PrintWriter outMin = new PrintWriter(new FileWriter(fileMin, true), true);
					outMax.println(" # SF vs H data  for circle interaction");
					outMax.println(" # Temperature = " + ising.T);
					//outMin.println(" # SF vs H data  for circle interaction");
					//outMin.println(" # Temperature = " + ising.T);
					initFile = true;
				}
				File fileMax = new File(dataFileMax);
				//File fileMin = new File(dataFileMin);
				PrintWriter outMax = new PrintWriter(new FileWriter(fileMax, true), true);
				//PrintWriter outMin = new PrintWriter(new FileWriter(fileMin, true), true);
				outMax.println(ising.H + " " + sf.peakValueC() + " " +ising.freeEnergy + " " + ising.time());
				//outMin.println(ising.H + " " + sf.minC() + " " +ising.freeEnergy + " " + ising.time());
				//System.out.println("Data written to file for time = " + ising.time());
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}
	}
}
 