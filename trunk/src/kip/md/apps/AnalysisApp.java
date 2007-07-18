package kip.md.apps;

import static kip.util.MathPlus.sqr;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import kip.md.Particle;
import kip.md.ParticleContext;

import scikit.dataset.Accumulator;
import scikit.dataset.DataSet;
import scikit.dataset.DynamicArray;
import scikit.graphics.Scene2D;
import scikit.graphics.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;


public class AnalysisApp extends Simulation {
	Plot wplot = new Plot("Mean squared displacement");
	Plot alpha = new Plot("Non-Gaussian parameter");
	Scene2D canvas = new Scene2D("Particles");
	SnapshotArray snapshots;
	ParticleContext pc;
	Accumulator dx2, dx4;
	
	public static void main(String[] args) {
		new Control(new AnalysisApp(), "Particle Simulation");
	}
	
	public AnalysisApp() {
		params.addm("Log scale", new ChoiceValue("True", "False"));
		params.add("Input directory", "/Users/kbarros/Desktop/data/phi=0.85");
		params.add("time");
	}
	
	public void animate() {
//		params.set("time", format(time));
//		canvas.removeAllGraphics();
//		snapshots.getContext().addGraphicsToCanvas(canvas, snapshots.get(time));
//		canvas.animate();
		
		boolean ls = params.sget("Log scale").equals("True");
		wplot.setLogScale(ls, ls);
		wplot.registerPoints("Mean squared displacement", dx2, Color.BLUE);
		
		alpha.setLogScale(true, false);
		alpha.registerPoints("alpha", getAlpha(), Color.RED);
	}
	
	public void clear() {
		canvas.clear();
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


class SnapshotArray {
	ArrayList<Snapshot> snapshots;
	double t_i, t_f;
	
	public SnapshotArray(String dir) {
		String[] fs = (new File(dir)).list();
		if (fs == null)
			System.err.println("Directory '" + dir + "' doesn't exist.");
		
		snapshots = new ArrayList<Snapshot>();
		for (String f : fs) {
			if (!f.substring(0, 2).equals("t="))
				continue;
			double time = Double.valueOf(f.substring(2));
			snapshots.add(new Snapshot(dir+File.separator+f, time));
		}
	    Collections.sort(snapshots);
	    
	    t_i = snapshots.get(0).time;
	    t_f = snapshots.get(snapshots.size()-1).time;
	}
	
	private double time(int i) {
		return snapshots.get(i).time();
	}
	
	public Particle[] get(double t) {
		// int i = (int) ((snapshots.size()-1) * (t - t_i) / (t_f - t_i) + 0.5); 
		// return snapshots.get(i).particles();
		
		// perform a binary search for snapshot at nearest time
		int lb = 0; // lower bound
		int ub = snapshots.size() - 1; // upper bound
		while (ub - lb > 1) {
			int i = (lb + ub) / 2;
			if (time(i) > t)
				ub = i;
			else
				lb = i;
		}
		double dt1 = t - time(lb);
		double dt2 = time(ub) - t;
		return snapshots.get((dt1 < dt2) ? lb : ub).particles(); 
	}
	
	public ParticleContext getContext() {
		return snapshots.get(0).particles()[0].tag.pc;
	}
	
	
	class Snapshot implements Comparable<Snapshot> {
		private double time;
		private String fname;
		private Particle[] ps;
		
		public Snapshot(String fname, double time) {
			this.fname = fname;
			this.time = time;
		}
		
		public double time() {
			return time;
		}
		
		public Particle[] particles() {
			if (ps == null)
				ps = ParticleContext.readParticles(fname);
			return ps;
		}
		
		public int compareTo(Snapshot that) {
			return Double.compare(time, that.time);
		}
	}
}


