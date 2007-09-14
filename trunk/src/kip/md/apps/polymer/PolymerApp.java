package kip.md.apps.polymer;

import static java.lang.Math.*;
import java.awt.Color;
import java.io.File;

import scikit.graphics.dim2.Scene2D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DirectoryValue;
import scikit.params.DoubleValue;
import scikit.util.Dump;
import static scikit.util.Utilities.*;

import kip.md.LJParticle2D;
import kip.md.MolecularDynamics2D;
import kip.md.Particle;
import kip.md.ParticleContext;
import kip.md.ParticleTag;


public class PolymerApp extends Simulation {
	Scene2D canvas = new Scene2D("Particles");
	MolecularDynamics2D<PolyParticle> sim;
	double lastAnimate;

	public static void main(String[] args) {
		new Control(new PolymerApp(), "Polymer Simulation");
	}
	
	public PolymerApp() {
		frame(canvas);
		params.add("Output directory", new DirectoryValue(""));
		params.add("Write files", new ChoiceValue("No", "Yes"));
		params.add("Topology", new ChoiceValue("Disk", "Torus"));
		params.add("Length", 30.0);
		params.add("Particles", 120);
		params.addm("Radius", new DoubleValue(1, 0.8, 1.2).withSlider());
		params.addm("Temperature", new DoubleValue(5, 0, 10).withSlider());
		params.addm("dt", 0.01);
		params.addm("Bath coupling", 0.2);
		params.add("Time");
		params.add("Reduced K.E.");
	}

	public void animate() {
		double r = params.fget("Radius");
		for (Particle p : sim.particles)
			p.tag.radius = r;
		sim.setStepSize(params.fget("dt"));
		sim.setTemperature(params.fget("Temperature"), params.fget("Bath coupling"));
		params.set("Time", format(sim.time()));
		params.set("Reduced K.E.", format(sim.reducedKineticEnergy()));
		canvas.setDrawables(asList(sim.pc.boundaryDw(), sim.pc.particlesLinkedDw(sim.particles)));
	}
	
	public void clear() {
		canvas.clear();
	}

	public void run() {
		double L = params.fget("Length");
		PolyContext pc = new PolyContext(L, ParticleContext.typeForString(params.sget("Topology")));
		double dt = params.fget("dt");		
		
		int N = params.iget("Particles");
		PolyParticle[] particles = new PolyParticle[N];
		
		for (int i = 0; i < N; i++) {
			ParticleTag tag = new PolyTag(i, particles);
			tag.pc = pc;
			tag.radius = params.fget("Radius");
			tag.color = new Color(1f-(float)i/N, 0f, (float)i/N, 0.5f);
			tag.mass = 1;
			tag.interactionRange = 6*tag.radius;
			particles[i] = new PolyParticle();
			particles[i].tag = tag;
		}
		
		pc.layOutParticlesSpiral(particles);		
		sim = new MolecularDynamics2D<PolyParticle>(dt, pc, particles);
		lastAnimate = 0;
		
		if (params.sget("Write files").equals("No")) {
			while (true) {
				maybeAnimate();
				sim.step();
			}
		}
		else {
			File dir = Dump.getEmptyDirectory(params.sget("Output directory"), "output");
			Dump.dumpString(dir+File.separator+"parameters.txt", params.toString());
			while (true) {
				sim.step();
				maybeAnimate();
				maybeDump(10, dir, particles);
			}
		}
	}
	
	void maybeAnimate() {
		long steps = round((sim.time()-lastAnimate)/sim.getStepSize()); 
		if (steps >= 10) {
			Job.animate();
			lastAnimate = sim.time();
		}
		else {
			Job.yield();
		}
	}
	
	void maybeDump(double del, File dir, LJParticle2D[] particles) {
		double dt = sim.getStepSize();
		int a = (int) ((sim.time() - dt/2)/del);
		int b = (int) ((sim.time() + dt/2)/del);
		if (b > a) {
			ParticleContext.dumpParticles(dir+File.separator+"t="+format(sim.time()), particles);			
		}
	}
}
