package rachele.ising.dim2.apps;

/* The Langevin Dynamics part of this is basically the same as kip.clump.dim2.FiledClump2D.java */

//import static kip.util.MathPlus.j1;

import static java.lang.Math.*;
import static scikit.util.Utilities.asList;
import static scikit.util.Utilities.frameTogether;
import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import rachele.ising.dim2.*;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;
import scikit.graphics.dim2.Geom2D;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim2.Grid;

public class IsingField2DApp extends Simulation {
    Grid grid = new Grid("Phi(x)");
    Grid sfGrid = new Grid("S(k)");
    Grid delPhiGrid = new Grid("DelPhi");
	Plot hSlice = new Plot("Horizontal Slice");
    Plot vSlice = new Plot("Vertical Slice");
	Plot structurePeakV = new Plot("Ver Structure Factor");
	Plot structurePeakH = new Plot("Hor Structure factor");
	Plot sfPlot = new Plot("Structure Factor");
	Plot sfHPlot = new Plot("Structure Factor Hor");
	Plot freeEnergyPlot = new Plot("Free Energy");
	Plot freeEnergyTempPlot = new Plot("Free Energy vs Temp");
	Plot sfSlopePlot = new Plot("SF peak slope");
	StructureFactor sf;
    IsingField2D ising;
    
    

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Ising Field");
	}
	
	public IsingField2DApp() {
		frameTogether("Grids", grid, delPhiGrid, sfGrid, freeEnergyPlot);
		frameTogether("Plots", vSlice, sfPlot, structurePeakV, hSlice, sfHPlot, structurePeakH);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Square", "Circle"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Steepest Decent", "Conjugate Gradient Min", "Langevin Conserve M", "Langevin No M Conservation"));
		params.add("Init Conditions", new ChoiceValue("Random Gaussian", "Artificial Stripe 3", "Read From File", "Constant" ));
		params.addm("Approx", new ChoiceValue("Exact Stable", "Exact SemiStable", "Exact", "Linear", "Phi4"));
		params.addm("Plot FEvT", new ChoiceValue("Off", "On"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("T", 0.1);
		params.addm("dT", 0.001);
		params.addm("tolerance", 0.0001);
		params.addm("dt", 1.0);
		params.addm("H", 0.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 4.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.6);
		params.add("Time");
		params.add("Mean Phi");
		params.add("Lp");
		params.add("Free Energy");
		params.add("dF_dt");

		flags.add("Write Config");
		flags.add("Clear");
		flags.add("Accept F");

	}
	
	public void animate() {

		ising.readParams(params);
		//params.set("dF_dt", ising.dF_dt);
		
		sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		if (params.sget("Zoom").equals("Yes")) {
			grid.registerData(ising.Lp, ising.Lp, ising.phi);
			delPhiGrid.registerData(ising.Lp, ising.Lp, ising.del_phiSq);
		}
		else {
			grid.registerData(ising.Lp, ising.Lp, ising.phi, -1, 1);
			delPhiGrid.registerData(ising.Lp, ising.Lp, ising.del_phiSq, 0, 1);
		}
		
		grid.setDrawables(asList(
				Geom2D.line(0, ising.horizontalSlice, 1, ising.horizontalSlice, Color.GREEN),
				Geom2D.line(ising.verticalSlice, 0, ising.verticalSlice, 1, Color.BLUE)));

		delPhiGrid.setDrawables(asList(
				Geom2D.line(0, ising.horizontalSlice, 1, ising.horizontalSlice, Color.GREEN),
				Geom2D.line(ising.verticalSlice, 0, ising.verticalSlice, 1, Color.BLUE)));
		
		hSlice.registerLines("Slice", ising.getHslice(), Color.BLACK);
		vSlice.registerLines("Slice", ising.getVslice(), Color.BLACK);
		
		freeEnergyPlot.registerLines("Free Energy", ising.getFreeEnergyAcc(), Color.MAGENTA);
		
		if(ising.circleInt() == true){
			sfPlot.registerLines("Structure Function", sf.getAccumulatorC(), Color.YELLOW);
			structurePeakV.registerLines("Peak Value", sf.getPeakC(), Color.YELLOW);
		}else{
			structurePeakV.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
			structurePeakH.registerLines("Structure Fucntion", sf.getPeakH(), Color.ORANGE);
			sfPlot.registerLines("Vertical SF", sf.getAccumulatorV(), Color.CYAN);
			sfHPlot.registerLines("Horizontal SF", sf.getAccumulatorH(), Color.ORANGE);
			sfSlopePlot.registerLines("Vertical Slope", sf.getPeakVslope(), Color.CYAN);
			sfSlopePlot.registerLines("Horizontal Slope", sf.getPeakHslope(), Color.ORANGE);			
		}	
 
		if (flags.contains("Accept F")){
			ising.accumFreeEnergy();
			System.out.println("accum free energy T = " + ising.T);
			//double temp = params.fget("T");
			//temp += params.fget("dT");
			//params.set("T", temp);			
		}

		
		if (flags.contains("Clear")) {
			ising.getFreeEnergyAcc().clear();
			ising.getDF_dtAcc().clear();
			ising.getAccEntropy().clear();
			ising.getAccPotential().clear();
			sf.getPeakH().clear();
			sf.getPeakV().clear();
			sf.getPeakHslope().clear();
			sf.getPeakVslope().clear();
			ising.aveCount = 0;
			ising.F_ave=0;
//			sf.getAccumulatorCA().clear();
//			sf.getAccumulatorHA().clear();
//			sf.getAccumulatorVA().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void clear() {
	}
	
	public void run() {
		
		if(params.sget("Init Conditions") == "Read From File")
			readInputParams("../../../research/javaData/configs/inputParams");
			
		ising = new IsingField2D(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
        boolean equilibrating = true;
		if(params.sget("Dynamics?") == "Conjugate Gradient Min")
			ising.initializeConjGrad();        
        while (true) {
        	if (flags.contains("Write Config")){
        		writeConfiguration();
        	}
			params.set("Time", ising.time());
			params.set("Mean Phi", ising.mean(ising.phi));
			if(params.sget("Dynamics?") == "Conjugate Gradient Min"){
				ising.getConjGradMin();
			}else if(params.sget("Dynamics?") == "Steepest Decent"){
				ising.steepestDecent();
			}else{
				ising.simulate();
			}
			if (equilibrating && ising.time() >= .5) {
				equilibrating = false;
			}
			sf.accumulateAll(ising.time(), ising.coarseGrained());
			//System.out.println(Math.abs(ising.dF_dt));
//			if(params.sget("Plot FEvT") == "On"){
//				if(Math.abs(ising.dF_dt) < params.fget("tolerance")){
//					System.out.println("input FE");
//					ising.accumFreeEnergy();
//					double temp = params.fget("T");
//					temp += params.fget("dT");
//					params.set("T", temp);
//				}				
//			}

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

//			readData = dis.readDouble();
//			System.out.println(readData);
//			params.set("Magnetization", readData);
//			dis.readChar();	
			
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
//			dos.writeDouble(params.fget("Magnetization"));
//			dos.writeChar('\t');			
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

}
 