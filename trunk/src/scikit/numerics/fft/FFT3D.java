package scikit.numerics.fft;

import static java.lang.Math.PI;

import java.lang.reflect.InvocationTargetException;

import scikit.numerics.fn.Function3D;

public abstract class FFT3D {
	public interface MapFn {
		public void apply(double k1, double k2, double k3, double re, double im);
	};
	
	public int dim1, dim2, dim3;
	protected double[] scratch;
	protected double dx1, dx2, dx3;
	
	abstract public void transform(double[] src, double[] dst);
	abstract public void backtransform(double[] src, double[] dst);
	
	public static FFT3D create(int dim1, int dim2, int dim3) {
		try {
			Class<?> c = Class.forName("scikit.numerics.fft.FFT3DManaged");
			return (FFT3D)c.getConstructor(int.class, int.class, int.class).newInstance(dim1, dim2, dim3);
		}
		catch (InvocationTargetException e) {
			System.out.println(e.getCause());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return new FFT3DManaged(dim1, dim2, dim3);
	}
	
	public void setLengths(double L1, double L2, double L3) {
		dx1 = L1/dim1;
		dx2 = L2/dim2;
		dx3 = L3/dim3;
	}
	
	public void transform(double[] src, MapFn fn) {
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
					fn.apply(k1, k2, k3, scratch[2*i+0], scratch[2*i+1]);
				}
			}
		}
	}
	
	/**
	 * Convolves source array src with function fn into destination dst
	 * It is permissible for src and dst to reference the same array.
	 * @param src
	 * @param dst
	 * @param fn
	 */
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
		
		backtransform(scratch, dst);
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

	public double[] getScratch() {
		return scratch;
	}
}
