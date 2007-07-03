package kip.md.apps;

import static java.lang.Math.*;
import java.awt.Color;

import geometry.VoronoiGraphics;

import scikit.graphics.Canvas2D;
import scikit.graphics.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import static scikit.util.Utilities.*;
import scikit.util.Bounds;

import kip.md.LJParticle2D;
import kip.md.MolecularDynamics2D;
import kip.md.ParticleTag;
import kip.md.StringAnalysis;


public class LennardJonesApp extends Simulation {
	Plot alphaplot = new Plot("Total energy");
	Canvas2D canvas = new Canvas2D("Particles");
	MolecularDynamics2D<LJParticle2D> sim;
	StringAnalysis<LJParticle2D> strings;
	

	public LennardJonesApp() {
		params.add("Topology", new ChoiceValue("Torus", "Disk"));
		params.add("Length", 20.0);
		params.add("Area fraction A", 0.7);
		params.add("Area fraction B", 0.0);
		params.add("Radius A", 1.0);
		params.add("Radius B", 0.7);
		params.add("Epsilon", 1.0);
		params.addm("dt", 0.01);
		params.addm("Temperature", 1.0);
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

		VoronoiGraphics voronoi = new VoronoiGraphics(new Bounds(0, sim.L, 0, sim.L));		
//		voronoi.construct(phase, 2, 0, NA+NB);

		canvas.removeAllGraphics();
		sim.addGraphicsToCanvas(canvas);
		canvas.addGraphics(voronoi);
		
		alphaplot.removeAllGraphics();
		alphaplot.addLines(strings.getAveragedAlpha(), Color.BLUE);
	}

	public void run() {
		Job.addDisplay(canvas);
		Job.addDisplay(alphaplot);
		
		double L = params.fget("Length");
		boolean inDisk = params.sget("Topology").equals("Disk");
		double dt = params.fget("dt");
		
		ParticleTag tagA = new ParticleTag();
		ParticleTag tagB = new ParticleTag();
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
		
		double systemArea = inDisk ? (PI*(L/2.)*(L/2.)) : L*L;
		int NA = (int) (params.fget("Area fraction A")*systemArea/particleAreaA);
		int NB = (int) (params.fget("Area fraction B")*systemArea/particleAreaB);

		LJParticle2D[] particles = new LJParticle2D[NA+NB];
		for (int i = 0; i < NA; i++)
			particles[i] = new LJParticle2D(tagA);
		for (int i = 0; i < NB; i++)
			particles[NA+i] = new LJParticle2D(tagB);

		sim = new MolecularDynamics2D<LJParticle2D>(L, inDisk, dt, particles);
		strings = new StringAnalysis<LJParticle2D>(2, 0.1);
		
		Job.animate();
		while (true) {
			sim.step();
			strings.addConfiguration(sim.time(), sim.particles);
			Job.animate();
		}
	}
}
