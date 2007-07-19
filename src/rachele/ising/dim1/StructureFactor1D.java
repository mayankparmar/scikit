package rachele.ising.dim1;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import scikit.dataset.Accumulator;
import scikit.numerics.fft.ComplexDouble2DFFT;
import scikit.numerics.fft.RealDoubleFFT_Radix2;

public class StructureFactor1D {
	RealDoubleFFT_Radix2 fft;	// Object to perform transforms
	double[] fftData;       // Fourier transform data
	int Lp;                 // # elements per side
	double L;               // the actual system length, L = Lp*dx, where dx is lattice spacing
	double R;               // characteristic length.  x-axis is k*R.
	double kRmin, kRmax;
	Accumulator acc;

	public StructureFactor1D(int Lp, double L, double R, double kRbinWidth) {
		this.Lp = Lp;
		this.L = L;
		this.R = R;
		
		kRmin = (2*PI*2/L)*R; // explicitly exclude constant (k=0) mode
		kRmax = (2*PI*(Lp/2)/L)*R;
		acc = new Accumulator(kRbinWidth);
		acc.setAveraging(true);
		fft = new RealDoubleFFT_Radix2(Lp);
		fftData = new double[Lp];
	}
	
	public Accumulator getAccumulator() {
		return acc;
	}
	
	public void accumulate(double[] xs) {
		for (int i = 0; i < Lp; i++)
			fftData[i] = xs[i];
		fft.transform(fftData);
		for (int y = -Lp/2; y < Lp/2; y++) {
			double kR = (2*PI*Math.abs(y))*R;
			if (kR >= kRmin && kR <= kRmax) {
				double re = fftData[y+Lp/2];
				acc.accum(kR, (re*re)/(L));
			}
		}
	}
	
}
