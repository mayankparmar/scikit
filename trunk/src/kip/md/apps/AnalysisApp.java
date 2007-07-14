package kip.md.apps;

import static scikit.util.Utilities.format;
import static java.lang.Math.*;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import kip.md.Particle;
import kip.md.ParticleContext;

import scikit.dataset.Accumulator;
import scikit.graphics.Canvas2D;
import scikit.graphics.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;


public class AnalysisApp extends Simulation {
	Plot wplot = new Plot("Mean squared displacement");
	Canvas2D canvas = new Canvas2D("Particles");
	SnapshotArray snapshots;
	Accumulator dx2;
	
	public static void main(String[] args) {
		new Control(new AnalysisApp(), "Particle Simulation");
	}
	
	public AnalysisApp() {
		params.add("Input directory", "/Users/kbarros/Desktop/output");
		params.add("time");
	}
	
	public void animate() {
//		params.set("time", format(time));
//		canvas.removeAllGraphics();
//		snapshots.getContext().addGraphicsToCanvas(canvas, snapshots.get(time));
//		canvas.animate();
		
		wplot.removeAllGraphics();
		wplot.addLines(dx2, Color.BLUE);
		wplot.animate();
	}
	
	public void run() {
		snapshots = new SnapshotArray(params.sget("Input directory"));
		
		dx2 = new Accumulator(0.1);
		dx2.setAveraging(true);
		
		for (double time = 0.1; time < 30; time += 0.1) {
			dx2.accum(time, secondMoment(time));
			Job.animate();
		}
	}
	
	public double secondMoment(double t) {
		double dx2 = 0;
		
		Particle[] ps1 = snapshots.get(snapshots.t_f - t);
		Particle[] ps2 = snapshots.get(snapshots.t_f);
		ParticleContext pc = ps1[0].tag.pc;
		
		for (int i = 0; i < ps1.length; i++) {
			dx2 += pc.displacement(ps1[i],ps2[i]).norm2();
		}
		return dx2 / ps1.length;
	}
}


class SnapshotArray {
	ArrayList<Snapshot> snapshots;
	double t_i, t_f, dt;
	
	public SnapshotArray(String dir) {
		String[] fs = (new File(dir)).list();
		
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
	    dt = (t_f - t_i) / (snapshots.size()-1);
	}
	
	public Particle[] get(double t) {
		int i = (int) ((snapshots.size()-1) * (t - t_i) / (t_f - t_i) + 0.5); 
		return snapshots.get(i).particles();
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


