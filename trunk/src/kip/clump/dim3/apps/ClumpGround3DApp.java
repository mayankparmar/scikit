package kip.clump.dim3.apps;

import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim3.FieldClump3D;
import scikit.dataset.Accumulator;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;


public class ClumpGround3DApp extends Simulation {
	Grid3D grid = new Grid3D("Grid");
	Plot feplot = new Plot("Free energy");
	Accumulator fe_bcc, fe_fcc;
	
	FieldClump3D clump;

	public static void main(String[] args) {
		new Control(new ClumpGround3DApp(), "Clump Model Ground State");
	}
	
	public ClumpGround3DApp() {
		frame(grid, feplot);
		params.add("dt", 0.1);
		params.add("R", 1300.0);
		params.add("L", 2000.0);
		params.add("dx", 100.0);
		params.add("T");
//		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		params.add("Rx");
		params.add("Ry");
		params.add("Rz");
	}

	public void animate() {
		int Lp = clump.numColumns();
		grid.registerData(Lp, Lp, Lp, clump.coarseGrained());
		
		feplot.registerPoints("BCC", fe_bcc, Color.RED);
		feplot.registerPoints("FCC", fe_fcc, Color.BLUE);
		
		params.set("dx", clump.dx);
		params.set("Time", format(clump.time()));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		params.set("Rx", format(clump.Rx));
		params.set("Ry", format(clump.Ry));
		params.set("Rz", format(clump.Rz));
	}

	public void clear() {
		grid.clear();
	}

	public void run() {		
		fe_bcc = new Accumulator(0.01);
		fe_fcc = new Accumulator(0.01);
		params.set("T", 0.02);
		
		clump = new FieldClump3D(params);
		clump.initializeFieldWithSeed("BCC");
		clump.useNoiselessDynamics(true);
		clump.useFixedBoundaryConditions(false);
		Job.animate();
		
		for (int i = 0; i < 200; i++) {
//			clump.T += 0.01;
			params.set("T", clump.T);
			relax();
			fe_bcc.accum(clump.time(), clump.freeEnergyDensity);
		}
	}
	
	public void relax() {
		double t1 = clump.time();
		while (clump.time() - t1 < 10)
			step();
	}
	
	public void step() {
		clump.simulate();
		clump.Rx -= clump.dt*sqr(clump.Rx)*clump.dFdensity_dRx();
		clump.Ry -= clump.dt*sqr(clump.Ry)*clump.dFdensity_dRy();
		clump.Rz -= clump.dt*sqr(clump.Rz)*clump.dFdensity_dRz();
		Job.animate();
	}
}
