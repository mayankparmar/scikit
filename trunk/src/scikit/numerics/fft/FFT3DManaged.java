package scikit.numerics.fft;

import scikit.numerics.fft.managed.ComplexDouble3DFFT;

public class FFT3DManaged extends FFT3D {
	ComplexDouble3DFFT fft;
	
	public FFT3DManaged(int dim1, int dim2, int dim3) {
		this.dim1 = dim1;
		this.dim2 = dim2;
		this.dim3 = dim3;
		fft = new ComplexDouble3DFFT(dim1, dim2, dim3);
		scratch = new double[2*dim1*dim2*dim3];
		dx1 = dx2 = dx3 = 1;
	}
	
	public void transform(double[] src, double[] dst) {
		for (int i = dim1*dim2*dim3-1; i >= 0; i--) {
			dst[2*i+0] = src[i]*dx1*dx2*dx3;
			dst[2*i+1] = 0;
		}
		fft.transform(dst);
		fft.toWraparoundOrder(dst);
	}
	
	public void backtransform(double[] src, double[] dst) {
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		double L3 = dim3*dx3;
		fft.backtransform(src);
		for (int i = 0; i < dim1*dim2*dim3; i++) {
			dst[i] = src[2*i+0] / (L1*L2*L3);
		}
	}
}
