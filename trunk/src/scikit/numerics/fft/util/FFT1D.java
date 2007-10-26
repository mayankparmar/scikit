package scikit.numerics.fft.util;

import static java.lang.Math.PI;
import scikit.numerics.fft.ComplexDoubleFFT;
import scikit.numerics.fft.ComplexDoubleFFT_Mixed;
import scikit.numerics.fn.Function1D;

public class FFT1D {
	public interface MapFn {
		public void apply(double k1, double re, double im);
	};
	
	int dim1;
	ComplexDoubleFFT fft;	// Object to perform transforms
	double[] scratch;
	double dx;
	
	public FFT1D(int dim1) {
		this.dim1 = dim1;
		fft = new ComplexDoubleFFT_Mixed(dim1);
		scratch = new double[2*dim1];
		dx = 1; 
	}
	
	public void setLength(double L) {
		dx = L/dim1;
	}
	
	public void transform(double[] phi, MapFn fn) {
		double L = dim1*dx;
		for (int i = 0; i < dim1; i++) {
			scratch[2*i] = phi[i]*dx;
			scratch[2*i+1] = 0;
		}
		
		fft.transform(scratch);
		scratch = fft.toWraparoundOrder(scratch);
		// verify imaginary component of structure factor is zero
		/*
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				int j = Lp*((-y+Lp)%Lp) + (-x+Lp)%Lp;
				assert(abs(scratch[2*i+1] + scratch[2*j+1]) < 1e-11);
			}
		}
		*/
		
		for (int x = -dim1/2; x < dim1/2; x++) {
			int i = (x+dim1)%dim1;
			double k = 2*PI*x/L;
			fn.apply(k, scratch[2*i], scratch[2*1+1]);
		}
	}
	
	public void convolve(double[] phi, Function1D fn, double[] res) {
		double L = dim1*dx;
		for (int i = 0; i < dim1; i++) {
			scratch[2*i] = phi[i];
			scratch[2*i+1] = 0;
		}
		
		fft.transform(scratch);
		for (int x = -dim1/2; x < dim1/2; x++) {
			int i = (x+dim1)%dim1;
			double k = 2*PI*x/L;
			double J = fn.eval(k);
			scratch[2*i] *= J;
			scratch[2*i+1] *= J;
		}
		
		fft.backtransform(scratch);
		for (int i = 0; i < dim1; i++) {
			res[i] = scratch[2*i] / dim1;
		}		
	}
}
