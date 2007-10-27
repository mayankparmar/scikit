package scikit.numerics.fft.util;

import static java.lang.Math.PI;
import scikit.numerics.fft.ComplexDouble2DFFT;
import scikit.numerics.fn.Function2D;

public class FFT2D {
	public interface MapFn {
		public void apply(double k1, double k2, double re, double im);
	};
	
	public int dim1, dim2;
	ComplexDouble2DFFT fft;
	double[] scratch;
	double dx1, dx2;
	
	public FFT2D(int dim1, int dim2) {
		this.dim1 = dim1;
		this.dim2 = dim2;
		fft = new ComplexDouble2DFFT(dim1, dim2);
		scratch = new double[2*dim1*dim2];
		dx1 = dx2 = 1;
	}
	
	public void setLengths(double L1, double L2) {
		dx1 = L1/dim1;
		dx2 = L2/dim2;
	}
	
	public void transform(double[] phi, MapFn fn) {
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		for (int i = dim1*dim2-1; i >= 0; i--) {
			scratch[2*i+0] = phi[i]*dx1*dx2;
			scratch[2*i+1] = 0;
		}
		
		fft.transform(scratch);
		scratch = fft.toWraparoundOrder(scratch);
		
		for (int x2 = -dim2/2; x2 < dim2/2; x2++) {
			for (int x1 = -dim1/2; x1 < dim1/2; x1++) {
				int i = dim1*((x2+dim2)%dim2) + (x1+dim1)%dim1;
				double k1 = 2*PI*x1/L1;
				double k2 = 2*PI*x2/L2;
				fn.apply(k1, k2, scratch[2*i+0], scratch[2*i+1]);
			}
		}
	}
	
	public void convolve(double[] src, double[] dst, Function2D fn) {
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		for (int i = dim1*dim2-1; i >= 0; i--) {
			scratch[2*i+0] = src[i]*dx1*dx2;
			scratch[2*i+1] = 0;
		}
		
		fft.transform(scratch);
		scratch = fft.toWraparoundOrder(scratch);

		for (int x2 = -dim2/2; x2 < dim2/2; x2++) {
			for (int x1 = -dim1/2; x1 < dim1/2; x1++) {
				int i = dim1*((x2+dim2)%dim2) + (x1+dim1)%dim1;
				double k1 = 2*PI*x1/L1;
				double k2 = 2*PI*x2/L2;
				double J = fn.eval(k1, k2);
				scratch[2*i+0] *= J;
				scratch[2*i+1] *= J;
			}
		}
		
		fft.backtransform(scratch);
		for (int i = 0; i < dim1*dim2; i++) {
			dst[i] = scratch[2*i+0] / (L1*L2);
		}
	}
	
	public double[] getScratch() {
		return scratch;
	}
}
