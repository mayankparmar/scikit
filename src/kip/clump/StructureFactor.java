package kip.clump;

import jnt.FFT.ComplexDouble2DFFT;
import scikit.plot.Accumulator;
import static java.lang.Math.*;

/*
* Calculates the structure factor
*/
class StructureFactor {
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftData;       // Fourier transform data
	int L;
	Accumulator acc;
	
	StructureFactor(int L, double binWidth) {
		this.L = L;
		acc = new Accumulator(binWidth);
		acc.setAveraging(true);
		fft = new ComplexDouble2DFFT(L, L);
		fftData = new double[2*L*L];
	}

	Accumulator calculate(int[] data) {
		// compute fourier transform
		for (int i = 0; i < L*L; i++) {
			fftData[2*i] = data[i];
			fftData[2*i+1] = 0;
		}
		fft.transform(fftData);
		fftData = fft.toWraparoundOrder(fftData);
		
		// verify imaginary component of structure factor is zero
		for (int y = -L/2; y < L/2; y++) {
			for (int x = -L/2; x < L/2; x++) {
				int i = L*((y+L)%L) + (x+L)%L;
				int j = L*((-y+L)%L) + (-x+L)%L;
				assert(abs(fftData[2*i+1] + fftData[2*j+1]) < 1e-11);
			}
		}
		
		// We calculate the structure factor s(k) by summing the fourier transform information
		// over all frequencies with equal magnitude (k).
		for (int y = -L/2; y < L/2; y++) {
			for (int x = -L/2; x < L/2; x++) {
				double k = sqrt(x*x+y*y);
				int i = L*((y+L)%L) + (x+L)%L;
				double re = fftData[2*i];
				double im = fftData[2*i+1];
				acc.accum(k, (re*re + im*im)/(L*L));
			}
		}
		return acc;
	}
}
