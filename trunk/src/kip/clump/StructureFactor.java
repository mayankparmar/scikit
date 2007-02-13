package kip.clump;

import jnt.FFT.ComplexDouble2DFFT;
import scikit.plot.Accumulator;
import static java.lang.Math.*;

/*
* Calculates the structure factor
*/
public class StructureFactor {
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftData;       // Fourier transform data
	int Lp;                 // # elements per side
	double L;               // the actual system length, L = Lp*dx, where dx is lattice spacing
	double R;               // characteristic length.  x-axis is k*R.
	double kRmin, kRmax;
	Accumulator acc;
	
	public StructureFactor(int Lp, double L, double R, double kRbinWidth) {
		this.Lp = Lp;
		this.L = L;
		this.R = R;
		
		kRmin = (2*PI*2/L)*R; // explicitly exclude constant (k=0) mode
		kRmax = (2*PI*(Lp/2)/L)*R;
		acc = new Accumulator(kRbinWidth*(L/R)/(2*PI));
		acc.setAveraging(true);
		fft = new ComplexDouble2DFFT(Lp, Lp);
		fftData = new double[2*Lp*Lp];
	}
	
	public Accumulator getAccumulator() {
		return acc;
	}
	
	public void setBounds(double kRmin, double kRmax) {
		this.kRmin = kRmin;
		this.kRmax = kRmax;
	}
	
	void accumulate(int[] data) {
		// compute fourier transform
		for (int i = 0; i < Lp*Lp; i++) {
			fftData[2*i] = data[i];
			fftData[2*i+1] = 0;
		}
		fft.transform(fftData);
		fftData = fft.toWraparoundOrder(fftData);
		
		// verify imaginary component of structure factor is zero
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				int j = Lp*((-y+Lp)%Lp) + (-x+Lp)%Lp;
				assert(abs(fftData[2*i+1] + fftData[2*j+1]) < 1e-11);
			}
		}
		
		// We calculate the structure factor s(k) by summing the fourier transform information
		// over all frequencies with equal magnitude (k).
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
				if (kR >= kRmin && kR <= kRmax) {
					int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double re = fftData[2*i];
					double im = fftData[2*i+1];
					acc.accum(kR, (re*re + im*im)/(L*L));
				}
			}
		}
	}
}
