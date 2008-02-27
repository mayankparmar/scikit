package scikit.numerics.fft;

import static java.lang.Math.PI;
import scikit.numerics.fn.Function3D;
import jfftw.complex.nd.Plan;


public class FFT3DNative extends FFT3D {
	static int flags = Plan.ESTIMATE;
//	static int flags = Plan.MEASURE;
	
	Plan fftForward, fftBackward;
	
	public FFT3DNative(int dim1, int dim2, int dim3) {
		this.dim1 = dim1;
		this.dim2 = dim2;
		this.dim3 = dim3;
		fftForward = new jfftw.complex.nd.Plan(new int[]{dim1, dim2, dim3}, Plan.FORWARD, flags);
		fftBackward = new jfftw.complex.nd.Plan(new int[]{dim1, dim2, dim3}, Plan.BACKWARD, flags);
		scratch = new double[2*dim1*dim2*dim3];
		dx1 = dx2 = dx3 = 1;
	}
	
	public void transform(double[] src, double[] dst) {
		for (int i = dim1*dim2*dim3-1; i >= 0; i--) {
			dst[2*i+0] = src[i]*dx1*dx2*dx3;
			dst[2*i+1] = 0;
		}
		fftForward.transform(dst);
	}
	
	public void backtransform(double[] src, double[] dst) {
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		double L3 = dim3*dx3;
		fftBackward.transform(src);
		for (int i = 0; i < dim1*dim2*dim3; i++) {
			dst[i] = src[2*i+0] / (L1*L2*L3);
		}
	}
	
	
	public void convolve2(double[] src, double[] dst, Function3D fn) {
		transform(src, scratch);
		
		double L1 = dim1*dx1;
		double L2 = dim2*dx2;
		double L3 = dim3*dx3;
		for (int x3 = 0; x3 <= dim3/2; x3++) {
			for (int x2 = 0; x2 <= dim2/2; x2++) {
				for (int x1 = 0; x1 <= dim1/2; x1++) {
					double k1 = 2*PI*x1/L1;
					double k2 = 2*PI*x2/L2;
					double k3 = 2*PI*x3/L3;
					double J = fn.eval(k1, k2, k3);
					
					for (int s3 = -1; s3 <= 1; s3 += 2) {
						for (int s2 = -1; s2 <= 1; s2 += 2) { 
							for (int s1 = -1; s1 <= 1; s1 += 2) { 
								int i = dim1*dim2*((s3*x3+dim3)%dim3) + dim1*((s2*x2+dim2)%dim2) + (s1*x1+dim1)%dim1;
								scratch[2*i+0] *= J;
								scratch[2*i+1] *= J;
								if (x1 == 0 || x1 == dim1/2)
									break;
							}
							if (x2 == 0 || x2 == dim2/2)
								break;
						}
						if (x3 == 0 || x3 == dim3/2)
							break;
					}
				}
			}
		}
		
		backtransform(scratch, dst);
	}
}
