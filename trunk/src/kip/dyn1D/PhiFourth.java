package kip.dyn1D;

import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class PhiFourth extends Dynamics1D {
	public double[] field, scratch;
	int N_dx;
	double h;
	
	public PhiFourth(Parameters params) {
		initialize(params);
	}
	
    public PhiFourth clone() {
		PhiFourth c = (PhiFourth)super.clone();
		c.field = (double[])field.clone();
		return c;
    }
	
	public void initialize(Parameters params) {
		super.initialize(params);
		
		R = params.iget("R");
		dx = (int)(R*params.fget("dx/R"));
		N = (int)(R*params.fget("N/R"));
		N -= N % dx;
		N_dx = N / dx;
		
		field = new double[N_dx];
		scratch = new double[N_dx];
	}
	
	public void setParameters(Parameters params) {
		super.setParameters(params);
		h  = params.fget("h", 0);
	}
	
	public void randomizeField(double m) {
		for (int i = 0; i < N_dx; i++) {
			field[i] = 0;
		}
	}
	
	public double magnetization() {
		double sum = 0;
		for (int i = 0; i < N_dx; i++)
			sum += field[i];
		return sum / N_dx;
	}
	
	public double[] copyField(double[] field_) {
		if (field_ == null)
			field_ = new double[N_dx];
		System.arraycopy(field, 0, field_, 0, field.length);
		return field;
	}

	protected void _step() {
        for (int i = 0; i < N_dx; i++) {
			double phi = field[i];
            double laplace = (field[(i+1)%N_dx]-2*phi+field[(i-1+N_dx)%N_dx])/(dx*dx);
            double phi3 = phi*phi*phi;
            double eta = random.nextGaussian() * sqrt(dt/(dx*R));
			scratch[i] += -dt*(-laplace + 2*phi + 4*phi3 - h) + eta;
        }
		System.arraycopy(scratch, 0, field, 0, N_dx);
	}
}
