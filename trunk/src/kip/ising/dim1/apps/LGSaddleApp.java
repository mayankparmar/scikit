package kip.ising.dim1.apps;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.awt.Color;

import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.DoubleValue;
import scikit.util.Utilities;

public class LGSaddleApp extends Simulation{
	Plot profile = new Plot("Droplet profile");
	Plot free = new Plot("Free energy");
	double[] fe;
	double[] xs; 
	double eps, u;
	
	public static void main (String[] args) {
		new Control(new LGSaddleApp(), "Landau Ginzburg Saddle");
	}
	
	// phi0 -0.32055757614
	// eps 0.1
	public void load(Control c) {
		c.frame(profile, free);
		profile.setAutoScale(true);
		free.setAutoScale(true);
		params.addm("phi0", new DoubleValue(-0.03196, -2, 0).withSlider());
		params.addm("eps", 1.0);
		params.addm("u", 1.0);
		params.addm("dim", 7.0);
		params.addm("dt", 0.002);
		params.add("len", 10000);
		params.add("free energy");
	}
	
	// solution to (u x^2 + x - eps = 0)
	double background() {
		if (u == 0)
			return eps;
		else
			return (-1 + sqrt(1 + 4*u*eps)) / (2*u);
	}
	
	double freeEnergy(double x, double v) {
		return v*v/2 - eps*x*x/2 + x*x*x/3 + u*x*x*x*x/4;
	}
	
	public void animate() {
		eps = params.fget("eps");
		u = params.fget("u");
		double dim = params.fget("dim");
		double dt = params.fget("dt");
		double fe_net = 0;
		double freeEnergy_bg = freeEnergy(background(), 0);
		
		double x = params.fget("phi0");
		double v = 0;
		for (int i = 0; i < xs.length; i++) {
			double t = (i+1)*dt;
			double a = -((dim-1)/t)*v - eps*x + x*x + u*x*x*x;
			v += a*dt; 
			x += v*dt;
			x = Math.min(x, 1000);
			xs[i] = x;
			fe[i] = dt*(freeEnergy(x, v)-freeEnergy_bg)*pow(t, dim-1);
			fe_net += fe[i];
		}
		
		profile.registerLines("profile", new PointSet(0, dt, xs), Color.BLACK);
		free.registerLines("free", new PointSet(0, dt, fe), Color.RED);
		params.set("free energy", Utilities.format(fe_net));
	}

	public void run() {
		xs = new double[params.iget("len")];
		fe = new double[xs.length];
		
		while (true)
			Job.animate();
	}
	
	public void clear() {
	}
}
