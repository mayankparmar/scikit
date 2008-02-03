package kip.clump.dim3.apps;

import static scikit.numerics.Math2.max;
import static scikit.numerics.Math2.min;
import static scikit.numerics.Math2.sqr;
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


public class ClumpPhase3DApp extends Simulation {
	Grid3D grid = new Grid3D("Grid");
	Plot feplot = new Plot("Free energy");
	Plot relplot = new Plot("Relaxation");
	Accumulator rel;
	Accumulator fe_bcc, fe_fcc;
	
	FieldClump3D clump;

	public static void main(String[] args) {
		new Control(new ClumpPhase3DApp(), "Clump Model Stable Phase");
	}
	
	public ClumpPhase3DApp() {
		frame(grid, feplot, relplot);
		params.add("dt", 0.1);
		params.add("R", 1300.0);
		params.add("L", 2000.0);
		params.add("dx", 100.0);
		params.add("T", 0.0);
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
		
		relplot.registerPoints("", rel, Color.BLACK);
		
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
		rel = new Accumulator(1);
		fe_bcc = new Accumulator(0.0001);
		fe_fcc = new Accumulator(0.0001);
		
		clump = new FieldClump3D(params);
		clump.initializeFieldWithSeed("BCC");
		clump.useNoiselessDynamics(true);
		Job.animate();
		
		setTemperature(0.09);
		simulate(0.5, 500);
		setTemperature(0.1);
		for (int i = 0; i < 14; i++) {
			relax();
			setTemperature(clump.T + 0.001);
		}
		
		clump.Rx = clump.Ry = 1400;
		clump.Rz = 1000;
		setTemperature(0.08);
		simulate(0.5, 500);
		setTemperature(0.1);
		for (int i = 0; i < 14; i++) {
			relax();
			setTemperature(clump.T + 0.001);
		}
	}
	
	void setTemperature(double T) {
		params.set("T", format(T));
		clump.T = T;
	}
	
	public void accumFE() {
		double rmax = max(clump.Rx, clump.Ry, clump.Rz);
		double rmin = min(clump.Rx, clump.Ry, clump.Rz);
		if (rmax / rmin > 1.4)
			fe_fcc.accum(clump.T, clump.freeEnergyDensity);
		else if (rmax / rmin < 1.02) {
			System.out.println(clump.T +  " " + clump.freeEnergyDensity);
			fe_bcc.accum(clump.T, clump.freeEnergyDensity);
		}
		else
			System.out.println("Weird configuration, Rmax="+rmax+" Rmin="+rmin);
	}
	
	public void relax() {
		rel.clear();
		simulate(0.5, 2000);
		simulate(0.01, 10);
		simulate(0.001, 1);
		accumFE();
	}
	
	void simulate(double dt, double time) {
		clump.dt = dt;
		double t1 = clump.time();
		while (clump.time() - t1 < time) {
			step();
			rel.accum(clump.time(), clump.freeEnergyDensity);
		}
	}
	
	public void step() {
		clump.simulate();
		clump.Rx -= clump.dt*sqr(clump.Rx)*clump.dFdensity_dRx();
		clump.Ry -= clump.dt*sqr(clump.Ry)*clump.dFdensity_dRy();
		clump.Rz -= clump.dt*sqr(clump.Rz)*clump.dFdensity_dRz();
		Job.animate();
	}
}
