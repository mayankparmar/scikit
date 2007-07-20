package kip.md.apps;

import static kip.util.MathPlus.sqr;

import java.awt.Color;

import kip.md.Particle;
import kip.md.ParticleContext;

import scikit.dataset.Accumulator;
import scikit.dataset.DataSet;
import scikit.dataset.DynamicArray;
import scikit.graphics.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;


public class AnalysisApp extends Simulation {
	Plot wplot = new Plot("Mean squared displacement");
	Plot alpha = new Plot("Non-Gaussian parameter");
	SnapshotArray snapshots;
	ParticleContext pc;
	Accumulator dx2, dx4;
	
	public static void main(String[] args) {
		new Control(new AnalysisApp(), "Particle Analysis");
	}
	
	public AnalysisApp() {
		params.addm("Log scale", new ChoiceValue("True", "False"));
		params.add("Input directory", "/Users/kbarros/Desktop/data/phi=0.85");
	}
	
	public void animate() {
		boolean ls = params.sget("Log scale").equals("True");
		wplot.setLogScale(ls, ls);
		wplot.registerPoints("Mean squared displacement", dx2, Color.BLUE);
		
		alpha.setLogScale(true, false);
		alpha.registerPoints("alpha", getAlpha(), Color.RED);
	}
	
	public void clear() {
		wplot.clear();
		alpha.clear();
	}
	
	public void run() {
		snapshots = new SnapshotArray(params.sget("Input directory"));
		pc = snapshots.getContext();
		
		dx2 = new Accumulator(0.1);
		dx2.setAveraging(true);
		dx4 = new Accumulator(0.1);
		dx4.setAveraging(true);
		
		for (int i = 0; i < 10; i++) {
			double tf = snapshots.t_f - 1*i;
			for (double time = 0.1; time < 2; time += 0.1) {
				dx2.accum(time, secondMoment(tf-time, tf));
				dx4.accum(time, fourthMoment(tf-time, tf));
				Job.animate();
			}
		}
		
		for (int i = 0; i < 10; i++) {
			double tf = snapshots.t_f - 10*i;
			for (double time = 1; time < 100; time += 1) {
				dx2.accum(time, secondMoment(tf-time, tf));
				dx4.accum(time, fourthMoment(tf-time, tf));
				Job.animate();
			}
		}
	}
	
	public DataSet getAlpha() {
		DynamicArray ret = new DynamicArray();		
		for (double t : dx2.keys()) {
			double d = pc.dim();
			double d2 = dx2.eval(t);
			double d4 = dx4.eval(t);
			ret.append2(t, (1/(1.+2./d)) * (d4/(d2*d2)) - 1.0);
		}
		return ret;
	}
	
	public double secondMoment(double t1, double t2) {	
		Particle[] ps1 = snapshots.get(t1);
		Particle[] ps2 = snapshots.get(t2);
		double dx2 = 0;
		for (int i = 0; i < ps1.length; i++) {
			dx2 += pc.displacement(ps1[i],ps2[i]).norm2();
		}
		return dx2 / ps1.length;
	}
	
	public double fourthMoment(double t1, double t2) {
		Particle[] ps1 = snapshots.get(t1);
		Particle[] ps2 = snapshots.get(t2);
		double dx4 = 0;
		for (int i = 0; i < ps1.length; i++) {
			dx4 += sqr(pc.displacement(ps1[i],ps2[i]).norm2());
		}
		return dx4 / ps1.length;
	}
}



