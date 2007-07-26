package kip.md.apps;

import static scikit.util.Utilities.format;

import java.util.ArrayList;

import kip.md.Particle;
import kip.md.ParticleContext;
import scikit.graphics.Scene2D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.DirectoryValue;


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
		params.add("Input directory", new DirectoryValue("/Users/kbarros/Desktop/data/binary/A=0.8 B=0.1 more"));
		params.add("t start", 4000.0);
		params.add("t finish", 4500.0);
		params.add("dt", 1.0);
		params.addm("t*", 30.0);
		params.addm("r*", 0.0);
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
		
		double ti = params.fget("t start");
		double tf = params.fget("t finish");
		double dt = params.fget("dt");
		
		for (time = ti; time < tf; time += dt) {
			double tstar = params.fget("t*");
			double rstar = params.fget("r*");
			
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
