package kip.clump.dim2.apps;

import static kip.util.MathPlus.j1;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;

import java.awt.Color;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import kip.clump.dim2.AbstractClump2D;
import kip.clump.dim2.Clump2D;
import kip.clump.dim2.FieldClump2D;
import kip.clump.dim2.StructureFactor;
import scikit.dataset.Function;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;
import scikit.util.FileUtil;


public class Clump2DApp extends Simulation {
	final static boolean WRITE_FILES = false;
	Grid grid = new Grid("Grid");
    Plot plot = new Plot("Structure factor");
    StructureFactor sf;
    AbstractClump2D clump;
    boolean fieldDynamics = true;
	
	public static void main(String[] args) {
		new Control(new Clump2DApp(), "Clump Model");
	}
	
	public Clump2DApp() {
		frame(grid, plot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		if (fieldDynamics) {
			params.addm("T", new DoubleValue(0.15, 0, 0.4).withSlider());
			params.addm("Noisy", new ChoiceValue("Yes", "No"));
			params.add("R", 1000);
			params.add("L", 8000.0);
			params.add("dx", 100.0);
		}
		else {
			params.addm("T", 0.15);
			params.add("R", 12.0);
			params.add("L", 192.0);
			params.add("dx", 3.0);
		}
		params.addm("dt", 1.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
		flags.add("Clear S.F.");
	}
	
	public void animate() {
		clump.readParams(params);
		if (fieldDynamics) {
			((FieldClump2D)clump).useNoiselessDynamics(!params.sget("Noisy").equals("Yes"));
		}
		if (flags.contains("Clear S.F."))
			sf.getAccumulator().clear();
		flags.clear();
		
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0, 5);
		grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		
        plot.registerLines("Structure Data", sf.getAccumulator(), Color.BLACK);
        plot.registerLines("Structure Theory", new Function(sf.kRmin(), sf.kRmax()) {
        	public double eval(double kR) {
        		double V = 2*j1(kR)/kR;
        		return 1/(V/clump.T+1);
        	}
        }, Color.BLUE);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
	}
	
	public void run() {
		clump = fieldDynamics ? new FieldClump2D(params) : new Clump2D(params);
        sf = clump.newStructureFactor(params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
        
		File dir = makeDirectory();
        while (true) {
			params.set("Time", clump.time());
			clump.simulate();
			clump.accumulateIntoStructureFactor(sf);
			writeField(dir);
			Job.animate();
		}
 	}
	
	File makeDirectory() {
		if (WRITE_FILES) {
			File dir = FileUtil.getEmptyDirectory(System.getProperty("user.home"), "output");
			FileUtil.dumpString(dir+File.separator+"parameters.txt", params.toString());
			return dir;
		}
		return null;
	}
	
	void writeField(File dir) {
		if (WRITE_FILES) {
			String fname = dir+File.separator+"t="+format(clump.time());
			try {
				DataOutputStream dos = FileUtil.dosFromString(fname);
				dos.writeInt(clump.numColumns());
				dos.writeInt(clump.numColumns());
				double[] phi = clump.coarseGrained();
				for (int i = 0; i < phi.length; i++)
					dos.writeFloat((float)phi[i]);
				dos.close();
			}
			catch (IOException e) {}
		}
	}
}
