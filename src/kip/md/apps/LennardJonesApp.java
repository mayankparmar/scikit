package kip.md.apps;

import static java.lang.Math.*;
import java.awt.Color;
import java.io.File;


import scikit.graphics.Scene2D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.util.Dump;
import static scikit.util.Utilities.*;

//import kip.geometry.VoronoiGraphics;
import kip.md.LJParticle2D;
import kip.md.MolecularDynamics2D;
import kip.md.ParticleContext;
import kip.md.ParticleTag;


public class LennardJonesApp extends Simulation {
	Scene2D canvas = new Scene2D("Particles");
	MolecularDynamics2D<LJParticle2D> sim;

	public LennardJonesApp() {
		params.add("Output directory", "/Users/kbarros/Desktop/output");
		params.add("Topology", new ChoiceValue("Disk", "Torus"));
		params.add("Length", 70.0);
		params.add("Area fraction A", 0.8);
		params.add("Area fraction B", 0.1);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.5);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.0025);
		params.addm("Temperature", 2);
		params.addm("Bath coupling", 0.2);
		params.add("Time");
		params.add("Reduced K.E.");
	}


	public static void main(String[] args) {
		new Control(new LennardJonesApp(), "Particle Simulation");
	}

	public void animate() {
		sim.setStepSize(params.fget("dt"));
		sim.setTemperature(params.fget("Temperature"), params.fget("Bath coupling"));
		params.set("Time", format(sim.time()));
		params.set("Reduced K.E.", format(sim.reducedKineticEnergy()));

//		VoronoiGraphics voronoi = new VoronoiGraphics(sim.pc.getBounds());		
//		voronoi.construct(phase, 2, 0, NA+NB);

		canvas.setDrawables(sim.pc.boundaryDw(), sim.pc.particlesDw(sim.particles));
//		canvas.addGraphics(voronoi);
	}
	
	public void clear() {
		canvas.clear();
	}

	public void run() {
		double L = params.fget("Length");
		ParticleContext pc = new ParticleContext(L, ParticleContext.typeForString(params.sget("Topology")));
		double dt = params.fget("dt");		

		ParticleTag tagA = new ParticleTag(1);
		ParticleTag tagB = new ParticleTag(2);
		tagA.pc = pc;
		tagB.pc = pc;
		tagA.radius = params.fget("Radius A");
		tagB.radius = params.fget("Radius B");
		tagA.color = new Color(0f, 0f, 1f, 0.5f);
		tagB.color = new Color(0f, 1f, 0f, 0.5f);
		double particleAreaA = PI*sqr(tagA.radius);
		double particleAreaB = PI*sqr(tagB.radius);
		double DENSITY = 1;
		tagA.mass = DENSITY*particleAreaA;
		tagB.mass = DENSITY*particleAreaB;
		double range = 3*2*max(tagA.radius,tagB.radius);
		tagA.interactionRange = range;
		tagB.interactionRange = range;
		int NA = (int) (params.fget("Area fraction A")*pc.systemArea()/particleAreaA);
		int NB = (int) (params.fget("Area fraction B")*pc.systemArea()/particleAreaB);
		
		System.out.println(NA + " " + NB);
		LJParticle2D[] particles = new LJParticle2D[NA+NB];
		for (int i = 0; i < NA; i++) {
			particles[i] = new LJParticle2D();
			particles[i].tag = tagA;
		}
		for (int i = 0; i < NB; i++) {
			particles[NA+i] = new LJParticle2D();
			particles[NA+i].tag = tagB;
		}

		sim = new MolecularDynamics2D<LJParticle2D>(dt, pc, particles);

		String dir = params.sget("Output directory");
		if (dir.equals("")) {
			while (true) {
				maybeAnimate();
				sim.step();
			}
		}
		else {
			dir = dir + File.separator;
			Dump.dumpString(dir + "parameters.txt", params.toString());
		
			while (sim.timeCnt < round(2000/sim.dt)) {
				sim.step();
				maybeAnimate();
				if (sim.timeCnt % round(10/sim.dt) == 0)
					ParticleContext.dumpParticles(dir+"t="+format(sim.time()), particles);
			}
			while (sim.timeCnt < round(2200/sim.dt)) {
				sim.step();
				maybeAnimate();
				if (sim.timeCnt % round(1/sim.dt) == 0)
					ParticleContext.dumpParticles(dir+"t="+format(sim.time()), particles);
			}
			while (sim.timeCnt < round(2220/sim.dt)) {
				sim.step();
				maybeAnimate();
				if (sim.timeCnt % round(0.1/sim.dt) == 0)
					ParticleContext.dumpParticles(dir+"t="+format(sim.time()), particles);
			}
		}
	}
	
	void maybeAnimate() {
		if (sim.timeCnt % 10 == 0)
			Job.animate();
		else
			Job.yield();
	}
}
