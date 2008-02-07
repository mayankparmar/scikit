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

public class LGSaddleApp extends Simulation{
	Plot profile = new Plot("Droplet profile");
	Plot free = new Plot("Free energy");
	double[] fe;
	double[] xs; 
	
	public static void main (String[] args) {
		new Control(new LGSaddleApp(), "Landau Ginzburg Saddle");
	}
	
	// phi0 -0.32055757614
	// eps 0.1
	public void load(Control c) {
		c.frame(profile, free);
		profile.setAutoScale(true);
		free.setAutoScale(true);
		params.addm("phi0", new DoubleValue(-0.03196, -1, 0).withSlider());
		params.addm("eps", 0.01);
		params.addm("dim", 3);
		params.addm("dt", 0.01);
		params.add("len", 10000);
	}
	
	double freeEnergy(double x, double v, double eps) {
		return v*v/2 + x*x*x/3 - eps*x*x/2;
	}
	
	public void animate() {
		double eps = params.fget("eps");
		double dim = params.fget("dim");
		double dt = params.fget("dt");
		
		double x = params.fget("phi0");
		double v = 0;
		for (int i = 0; i < xs.length; i++) {
			double t = (i+1)*dt;
			double a = -((dim-1)/t)*v + x*x - eps*x;
			v += a*dt; 
			x += v*dt;
			x = Math.min(x, 10);
			xs[i] = x/eps;
			fe[i] = dt*(freeEnergy(x, v, eps)-freeEnergy(eps,0, eps))*pow(t, dim-1);
		}
		
		profile.registerLines("profile", new PointSet(0, dt*sqrt(eps), xs), Color.BLACK);
		free.registerLines("free", new PointSet(0, dt, fe), Color.RED);
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
