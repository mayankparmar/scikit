package kip.md.apps;

import static java.lang.Math.*;
import java.awt.Color;
import java.io.File;


import scikit.graphics.Scene2D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import static scikit.util.Utilities.*;
//import scikit.util.Bounds;

//import kip.geometry.VoronoiGraphics;
import kip.md.LJParticle2D;
import kip.md.MolecularDynamics2D;
import kip.md.ParticleContext;
import kip.md.ParticleTag;
//import kip.md.StringAnalysis;


public class LennardJonesApp extends Simulation {
	Scene2D canvas = new Scene2D("Particles");
	MolecularDynamics2D<LJParticle2D> sim;
//	StringAnalysis strings;

	public LennardJonesApp() {
		params.add("Output directory", "/Users/kbarros/Desktop/output");
		params.add("Topology", new ChoiceValue("Disk", "Torus"));
		params.add("Length", 100.0);
		params.add("Area fraction A", 0.8);
		params.add("Area fraction B", 0.0);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.7);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.01);
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

//		VoronoiGraphics voronoi = new VoronoiGraphics(new Bounds(0, sim.pc.L, 0, sim.pc.L));		
//		voronoi.construct(phase, 2, 0, NA+NB);

		canvas.animate(sim.pc.getDrawables(sim.particles));
//		strings.addGraphicsToCanvas(canvas);
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

		LJParticle2D[] particles = new LJParticle2D[NA+NB];
		for (int i = 0; i < NA; i++) {
			particles[i] = new LJParticle2D();
			particles[i].tag = tagA;
		}
		for (int i = 0; i < NB; i++) {
			particles[NA+i] = new LJParticle2D();
			particles[NA+i].tag = tagB;
		}
		
		String dir = params.sget("Output directory") + File.separator;
		sim = new MolecularDynamics2D<LJParticle2D>(dt, pc, particles);
//		strings = new StringAnalysis(pc, params.fget("String memory time"), 0.1);
		
		Job.animate();
		while (true) {
			for (int i = 0; i < 10; i++) {
				sim.step();
				Job.yield();
			}
			Job.animate();
//			strings.addConfiguration(sim.time(), sim.particles);
			ParticleContext.dumpParticles(dir+"t="+format(sim.time()), particles);
		}
	}
}
