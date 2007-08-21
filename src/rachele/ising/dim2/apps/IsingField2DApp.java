package rachele.ising.dim2.apps;

/* This is basically the same as kip.clump.dim2.FiledClump2D.java */

//import static kip.util.MathPlus.j1;

import static java.lang.Math.*;

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
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class IsingField2DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Phi(x)", true);
    FieldDisplay sfGrid = new FieldDisplay("S(k)", true);
    Plot hSlice = new Plot("Horizontal Slice", true);
    Plot vSlice = new Plot("Vertical Slice", true);
	Plot structurePeak = new Plot("Structure Peak vs Time", true);
	Plot freeEnergyPlot = new Plot("Free Energy", true);
	//Plot circleSF = new Plot("Circle Structure factor", true);
	Plot sfPlot = new Plot("Structure Factor", true);
    StructureFactor sf;
    IsingField2D ising;

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Ising Field");
	}
	
	public IsingField2DApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.add("Init Conditions", new ChoiceValue("Random Gaussian", "Read From File", "Constant"));
		params.addm("Approx", new ChoiceValue("Exact", "Exact SemiStable", "Exact Stable", "Linear", "Phi4"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("T", 0.06);
		params.addm("dt", 0.1);
		params.addm("H", 0.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 8.0);
		params.add("R/dx", 8.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.0);
		params.add("Time");
		params.add("Mean Phi");
		params.add("Lp");
		params.add("dF_dt");

		flags.add("Write Config");
		flags.add("Clear S.F.");

	}
	
	public void animate() {
		
		ising.readParams(params);
		
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(-1.0, 1.0);

		if (params.sget("Zoom").equals("Yes"))
			sfGrid.setAutoScale();
		else
			sfGrid.setScale(-1.0, 1.0);
		
		//sfGrid.setAutoScale();
				
		hSlice.clear();
		vSlice.clear();
		
		hSlice.setDataSet(0, ising.getHslice());
		vSlice.setDataSet(0, ising.getVslice());

		freeEnergyPlot.setDataSet(6, ising.getFreeEnergyAcc());
		
		if (ising.circleInt() == true){
			sfPlot.setDataSet(5, sf.getAccumulatorC());
			structurePeak.setDataSet(5, sf.getPeakC());
		}else{
			structurePeak.setDataSet(3, sf.getPeakV());
			structurePeak.setDataSet(4, sf.getPeakH());
			sfPlot.setDataSet(3, sf.getAccumulatorV());
			sfPlot.setDataSet(4, sf.getAccumulatorH());
		}

        
		if (flags.contains("Clear S.F.")) {
			ising.getFreeEnergyAcc().clear();
//			sf.getAccumulatorCA().clear();
//			sf.getAccumulatorHA().clear();
//			sf.getAccumulatorVA().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void run() {
		
		if(params.sget("Init Conditions") == "Read From File")
			readInputParams("../../../research/javaData/configs/inputParams");
		Job.addDisplay(grid);
		Job.addDisplay(sfGrid);
		Job.addDisplay(hSlice);
		Job.addDisplay(vSlice);
		Job.addDisplay(structurePeak);
		Job.addDisplay(freeEnergyPlot);
		Job.addDisplay(sfPlot);

		ising = new IsingField2D(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
		grid.setData(ising.Lp,ising.Lp,ising.phi);
		sfGrid.setData(ising.Lp, ising.Lp,sf.sFactor);
		
        boolean equilibrating = true;
        while (true) {
        	if (flags.contains("Write Config")){
        		writeConfiguration();
        	}
			params.set("Time", ising.time());
			params.set("Mean Phi", ising.mean(ising.phi));
			ising.simulate();
			if (equilibrating && ising.time() >= .5) {
			equilibrating = false;
			}
			sf.accumulateAll(ising.time(), ising.coarseGrained());
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
}
