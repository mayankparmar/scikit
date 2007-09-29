package kip.clump.dim3.apps;

//import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;
import kip.clump.dim3.ScalableFieldClump3D;
import scikit.graphics.dim3.Grid3D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.DoubleValue;

public class ClumpGround3DApp extends Simulation {
	Grid3D grid = new Grid3D("Grid");
	ScalableFieldClump3D clump;

	public static void main(String[] args) {
		new Control(new ClumpGround3DApp(), "Clump Model Ground State");
	}
	
	public ClumpGround3DApp() {
		frame(grid);
		params.addm("T", new DoubleValue(0.09, 0.0, 0.15).withSlider());
		params.addm("dt", 1.0);
		params.addm("R_x", 1000.0);
		params.addm("R_y", 1000.0);
		params.addm("R_z", 1000.0);
		params.add("L", 2000.0);
		params.add("dx", 100.0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		flags.add("Res up");
		flags.add("Res down");
	}

	public void animate() {
		if (flags.contains("Res up"))
			clump.doubleResolution();
		if (flags.contains("Res down"))
			clump.halveResolution();
		flags.clear();
		params.set("dx", clump.dx);
//		params.set("R_x", format(clump.Rx));
//		params.set("R_y", format(clump.Ry));
//		params.set("R_z", format(clump.Rz));
		params.set("Time", format(clump.t));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		
		grid.registerData(clump.Lp, clump.Lp, clump.Lp, clump.phi);
		clump.readParams(params);
	}

	public void clear() {
		grid.clear();
	}

	public void run() {
		clump = new ScalableFieldClump3D(params);
		clump.initializeFieldWithSeed();
		clump.noiselessDynamics = true;
		Job.animate();
		
		while (true) {
			clump.simulate();
//			double[] dF_dR = clump.dFdensity_dR();
//			clump.Rx -= 0.001*sqr(clump.Rx)*dF_dR[0];
//			clump.Ry -= 0.001*sqr(clump.Ry)*dF_dR[1];
//			clump.Rz -= 0.001*sqr(clump.Rz)*dF_dR[2];
			Job.animate();
		}
	}
}
