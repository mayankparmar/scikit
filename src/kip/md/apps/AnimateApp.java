package kip.md.apps;

import static scikit.util.Utilities.format;

import kip.md.ParticleContext;
import scikit.graphics.Scene2D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;


public class AnimateApp extends Simulation {
	Scene2D canvas = new Scene2D("Particles");
	SnapshotArray snapshots;
	ParticleContext pc;
	double time;
	
	public static void main(String[] args) {
		new Control(new AnimateApp(), "Particle Animation").getJob().throttleAnimation(true);
	}
	
	public AnimateApp() {
		params.add("Input directory", "/Users/kbarros/Desktop/data/binary/A=0.75 B=0.1");
		params.add("time");
	}

	public void animate() {
		params.set("time", format(time));
		canvas.setDrawables(pc.getDrawables(snapshots.get(time)));
	}
	
	public void clear() {
		canvas.clear();
	}
	
	public void run() {
		snapshots = new SnapshotArray(params.sget("Input directory"));
		pc = snapshots.getContext();

		for (time = snapshots.t_i; time < snapshots.t_f; time += 10) {
			Job.animate();
		}
		Job.animate();
	}
}
