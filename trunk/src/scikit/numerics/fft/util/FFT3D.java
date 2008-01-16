package scikit.numerics.fft.util;

import static java.lang.Math.PI;
import scikit.numerics.fft.ComplexDouble3DFFT;
import scikit.numerics.fn.Function3D;

public class FFT3D {
	public interface MapFn {
		public void apply(double k1, double k2, double k3, double re, double im);
	};
	
	public int dim1, dim2, dim3;
	ComplexDouble3DFFT fft;
	double[] scratch;
	double dx1, dx2, dx3;
	
	public FFT3D(int dim1, int dim2, int dim3) {
		this.dim1 = dim1;
		this.dim2 = dim2;
		this.dim3 = dim3;
		fft = new ComplexDouble3DFFT(dim1, dim2, dim3);
		scratch = new double[2*dim1*dim2*dim3];
		dx1 = dx2 = dx3 = 1;
	}
	
	public void setLengths(double L1, double L2, double L3) {
		dx1 = L1/dim1;
		dx2 = L2/dim2;
		dx3 = L3/dim3;
	}
	
	public void transform(double[] phi, double[] dst) {
		for (int i = dim1*dim2*dim3-1; i >= 0; i--) {
			dst[2*i+0] = phi[i]*dx1*dx2*dx3;
			dst[2*i+1] = 0;
		}
		fft.transform(dst);
		fft.toWraparoundOrder(dst);
	}
	
	public void transform(double[] phi, MapFn fn) {
		transform(phi, scratch);
		
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		double L3 = dim3*dx3;
		for (int x3 = -dim3/2; x3 < dim3/2; x3++) {
			for (int x2 = -dim2/2; x2 < dim2/2; x2++) {
				for (int x1 = -dim1/2; x1 < dim1/2; x1++) {
					int i = dim1*dim2*((x3+dim3)%dim3) + dim1*((x2+dim2)%dim2) + (x1+dim1)%dim1;
					double k1 = 2*PI*x1/L1;
					double k2 = 2*PI*x2/L2;
					double k3 = 2*PI*x3/L3;
					fn.apply(k1, k2, k3, scratch[2*i+0], scratch[2*i+1]);
				}
			}
		}
	}
	
	public void convolve(double[] src, double[] dst, Function3D fn) {
		transform(src, scratch);
		
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		double L3 = dim3*dx3;
		for (int x3 = -dim3/2; x3 < dim3/2; x3++) {
			for (int x2 = -dim2/2; x2 < dim2/2; x2++) {
				for (int x1 = -dim1/2; x1 < dim1/2; x1++) {
					int i = dim1*dim2*((x3+dim3)%dim3) + dim1*((x2+dim2)%dim2) + (x1+dim1)%dim1;
					double k1 = 2*PI*x1/L1;
					double k2 = 2*PI*x2/L2;
					double k3 = 2*PI*x3/L3;
					double J = fn.eval(k1, k2, k3);
					scratch[2*i+0] *= J;
					scratch[2*i+1] *= J;
				}
			}
		}
		
		fft.backtransform(scratch);
		for (int i = 0; i < dim1*dim2*dim3; i++) {
			dst[i] = scratch[2*i+0] / (L1*L2*L3);
		}
	}
	
	public double[] getScratch() {
		return scratch;
	}
}
