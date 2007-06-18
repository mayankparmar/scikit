package mina;



import static java.lang.Math.*;
import scikit.params.DoubleValue;
import scikit.params.IntValue;
import scikit.plot.*;
import scikit.dataset.PointSet;
import scikit.jobs.*;

public class Gravity extends Simulation {
	public static void main(String[] args) {
		new Control(new Gravity(), "Particles in a box");
	}

	Plot particles = new Plot("Particles", true);
	
	int N;					// number of particles
	double t, dt;			// time, time step
	double g;
	double[] x, y, vx, vy;	// gas phase coordinates
	
	public Gravity() {
		params.add("# of particles", new IntValue(1000, 1, 100000));
		params.add("Gravity", new DoubleValue(0.00001, 0,  0.00002));
		params.addm("dt", new DoubleValue(0.05, 0, 0.2));

		Job.addDisplay(particles);
	}


	public void animate() {
		dt	= params.fget("dt");
	}


	public void run() {
		N = params.iget("# of particles");
		g = params.fget("Gravity");
		dt	= params.fget("dt");
		
		t = 0;
		x = new double[N];
		y = new double[N];
		vx = new double[N];
		vy = new double[N];
		for (int i = 0; i < N; i++) {
			x[i] = random();
			y[i] = random();
			vx[i] = 0.001 * (2*random() - 1);
		}
		
		particles.setStyle(0, Plot.Style.MARKS);
		particles.setDataSet(0, new PointSet(x, y));
		
		while (true) {
			
			for (int i = 0; i < N; i++) {
				vy[i] += - g * dt;
				x[i] += vx[i]*dt;
				y[i] += vy[i]*dt;
				
				if (x[i] < 0 && vx[i] < 0) {
					x[i] = 0;
					vx[i] = -vx[i];
				}
				if (x[i] > 1 && vx[i] > 0) {
					x[i] = 1;
					vx[i] = -vx[i];
				}
				if (y[i] < 0 && vy[i] < 0) {
					y[i] = 0;
					vy[i] = -vy[i];
				}
			}
			
			Job.animate();
		}
	}
}