package kip.dyn1D;

import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class FieldIsing extends AbstractIsing {
	public double[] field, scratch1, scratch2;
	
	public FieldIsing(Parameters params) {
		initialize(params);
	}
	
	
    public FieldIsing clone() {
		FieldIsing c = (FieldIsing)super.clone();
		c.field = (double[])field.clone();
		return c;
    }
	
	
	// reset time, set random number seed, initialize fields to down
	public void initialize(Parameters params) {
		super.initialize(params);
		field = new double[N/dx];
		scratch1 = new double[N/dx];
		scratch2 = new double[N/dx];		
	}
	
	
	public void randomizeField(double m) {
		assert (m == 0);
		
		for (int i = 0; i < N/dx; i++) {
			field[i] = random.nextGaussian() / sqrt(dx);
		}
	}
	
	
	public double magnetization() {
		double sum = 0;
		for (int i = 0; i < N/dx; i++)
			sum += field[i];
		return sum / (N/dx);
	}
	
	
	public double fieldElement(int i) {
		return field[i];
	}
	
	
	private double bar(double[] field, int i) {
		int nblocks = (int)round(2.0*R/dx);
		
		double acc = field[i];
		for (int j = 1; j <= nblocks/2; j++) {
			acc += field[(i+j)%(N/dx)];
			acc += field[(i-j+N/dx)%(N/dx)];
		}
		return acc / (1 + 2*(nblocks/2));
	}
	
	
	protected void _step() {
		// break update into "steps" number of substeps. each one with time interval dt_
		double dx_ = dx;
		int steps = (int) max(sqrt(dt) * 100 / sqrt(dx_), 1);
		double dt_ = dt / steps;
		
		double K = J / T;
		double H = h / T;
		
		/*
		// ----------------------------------------- linear theory
		for (int cnt = 0; cnt < steps; cnt++) {
			for (int i = 0; i < N/dx; i++) {
				double f = field[i];
				double g = K*bar(field, i) + H;
				double U = g - f;
				double V = sqrt(2);
				double eta = random.nextGaussian();
				scratch1[i] = f + dt_*U + sqrt(dt_/dx_)*eta*V;
			}
			for (int i = 0; i < N/dx; i++) {
				field[i] = scratch1[i];
			}
		}
		*/
		
		// ----------------------------------------- heun scheme for perturbation in phi^2
		for (int cnt = 0; cnt < steps; cnt++) {
			// get euler predictor
			for (int i = 0; i < N/dx; i++) {
				double f = field[i];
				double g = K*bar(field, i) + H;
				double U = g - (g*g*g/3) - f;
				double V = sqrt(2) * (1 /* - sqr(g)/4 - sqr(f)/4 */);
				double eta = random.nextGaussian();
				scratch1[i] = f + dt_*U + sqrt(dt_/dx_)*eta*V;
			}
			
			// take step based on predictor
			for (int i = 0; i < N/dx; i++) {
				double f1 = field[i];
				double f2 = scratch1[i];
				double g1 = K*bar(field, i) + H;
				double g2 = K*bar(scratch1, i) + H;
				double U1 = g1 - (g1*g1*g1/3) - f1;
				double U2 = g2 - (g2*g2*g2/3) - f2;
				double V1 = sqrt(2) * (1 /* - sqr(g1)/4 - sqr(f1)/4 */);
				double V2 = sqrt(2) * (1 /* - sqr(g2)/4 - sqr(f2)/4 */);
				
				double eta = random.nextGaussian();
				scratch2[i] = f1 + dt_*(U1+U2)/2 + sqrt(dt_/dx_)*eta*(V1+V2)/2;
			}
			
			// copy back to field
			for (int i = 0; i < N/dx; i++) {
				field[i] = scratch2[i];
			}
		}
		
		/*
		// ----------------------------------------- full heun scheme
		for (int cnt = 0; cnt < steps; cnt++) {
			// get euler predictor
			for (int i = 0; i < N/dx; i++) {
				double f = field[i];
				double g = K*bar(field, i) + H;
				double U = tanh(g) - f;
				double V = sqrt(2 - sqr(tanh(g)) - sqr(f));
				double eta = random.nextGaussian();
				scratch1[i] = f + dt_*U + sqrt(dt_/dx_)*eta*V;
			}
			
			// take step based on predictor
			for (int i = 0; i < N/dx; i++) {
				double f1 = field[i];
				double f2 = scratch1[i];
				double g1 = K*bar(field, i) + H;
				double g2 = K*bar(scratch1, i) + H;
				double U1 = tanh(g1) - f1;
				double U2 = tanh(g2) - f2;
				double V1 = sqrt(2 - sqr(tanh(g1)) - sqr(f1));
				double V2 = sqrt(2 - sqr(tanh(g2)) - sqr(f2));
				
				double eta = random.nextGaussian();
				scratch2[i] = f1 + dt_*(U1+U2)/2 + sqrt(dt_/dx_)*eta*(V1+V2)/2;
			}
			
			// copy back to field
			for (int i = 0; i < N/dx; i++) {
				field[i] = scratch2[i];
			}
		}
		*/
	}
}