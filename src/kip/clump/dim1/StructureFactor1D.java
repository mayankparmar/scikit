package kip.clump.dim1;

import static java.lang.Math.PI;
import scikit.dataset.Accumulator;
import scikit.numerics.fft.ComplexDoubleFFT;
import scikit.numerics.fft.ComplexDoubleFFT_Mixed;


/*
* Calculates the structure factor
*/
public class StructureFactor1D {
	ComplexDoubleFFT fft;	// Object to perform transforms
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
		fft = new ComplexDoubleFFT_Mixed(Lp);
		fftData = new double[2*Lp];
	}
	
	public Accumulator getAccumulator() {
		return acc;
	}
	
	public double kRmin() {
		return kRmin;
	}
	
	public double kRmax() {
		return kRmax;
	}
	
	public void setBounds(double kRmin, double kRmax) {
		this.kRmin = kRmin;
		this.kRmax = kRmax;
	}
	
	public void accumulate(double[] data) {
		double dx = (L/Lp);
		for (int i = 0; i < Lp; i++) {
			fftData[2*i] = data[i]*dx;
			fftData[2*i+1] = 0;
		}
		accumulateAux();
	}
	
	public void accumulateAux() {
		// compute fourier transform
		fft.transform(fftData);
		fftData = fft.toWraparoundOrder(fftData);
		
		// verify imaginary component of structure factor is zero
		/*
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				int j = Lp*((-y+Lp)%Lp) + (-x+Lp)%Lp;
				assert(abs(fftData[2*i+1] + fftData[2*j+1]) < 1e-11);
			}
		}
		*/
		
		// We calculate the structure factor s(k) by summing the fourier transform information
		// over all frequencies with equal magnitude (k).
		for (int x = -Lp/2; x < Lp/2; x++) {
			double kR = (2*PI*x/L)*R;
			if (kR >= kRmin && kR <= kRmax) {
				int i = (x+Lp)%Lp;
				double re = fftData[2*i];
				double im = fftData[2*i+1];
				acc.accum(kR, (re*re + im*im)/L);
			}
		}
	}
}
