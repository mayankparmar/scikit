package kip.md.apps;

import static scikit.util.Utilities.format;

import java.util.ArrayList;

import kip.md.Particle;
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
	Particle[] particles;
	
	public static void main(String[] args) {
		new Control(new AnimateApp(), "Particle Animation").getJob().throttleAnimation(true);
	}
	
	public AnimateApp() {
		params.add("t*", 30.0);
		params.add("r*", 0);
//		params.add("Input directory", "/Users/kbarros/Desktop/data/binary/A=0.75 B=0.1");
//		params.add("Input directory", "/Users/kbarros/Desktop/data/unary/phi=0.85");
		params.add("Input directory", "/Users/kbarros/Desktop/data/binary/A=0.8 B=0.1");
		params.add("time");
	}

	public void animate() {
		params.set("time", format(time));
		canvas.setDrawables(pc.particlesDw(particles), pc.boundaryDw());
	}
	
	public void clear() {
		canvas.clear();
	}
	
	public void run() {
		snapshots = new SnapshotArray(params.sget("Input directory"));
		pc = snapshots.getContext();
		
		double tstar = params.fget("t*");
		double rstar = params.fget("r*");
		
		for (time = snapshots.t_i; time < snapshots.t_f; time += 10) {
			if (rstar == 0) {
				particles = snapshots.get(time);
			}
			else {
				ArrayList<Particle> res = new ArrayList<Particle>();
				Particle[] ps1 = snapshots.get(time - tstar);
				Particle[] ps2 = snapshots.get(time);
				for (int i = 0; i < ps1.length; i++) {
					if (pc.displacement(ps1[i],ps2[i]).norm2() > rstar*rstar)
						res.add(ps2[i]); 
				}
				particles = res.toArray(new Particle[0]);
			}
			Job.animate();
		}
		Job.animate();
	}
}
