package rachele.ising.dim2;


import scikit.dataset.Accumulator;
import static java.lang.Math.*;
import scikit.numerics.fft.ComplexDouble2DFFT;

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
	
	Accumulator accCircle;
	Accumulator accHorizontal;
	Accumulator accVertical;
	Accumulator accAvH;
	Accumulator accAvV;
	Accumulator accAvC;
	
	public StructureFactor(int Lp, double L, double R, double kRbinWidth) {
		this.Lp = Lp;
		this.L = L;
		this.R = R;
		
		kRmin = (2*PI*2/L)*R; // explicitly exclude constant (k=0) mode
		kRmax = (2*PI*(Lp/2)/L)*R;
		
		accCircle = new Accumulator(kRbinWidth);
		accHorizontal = new Accumulator(kRbinWidth);
		accVertical = new Accumulator(kRbinWidth);
		accAvH = new Accumulator(kRbinWidth);
		accAvV = new Accumulator(kRbinWidth);
		accAvC = new Accumulator(kRbinWidth);
		
		accAvH.setAveraging(true);		
		accAvV.setAveraging(true);
		accAvC.setAveraging(true);
		accCircle.setAveraging(true);
		accHorizontal.setAveraging(true);
		accVertical.setAveraging(true);
		
		fft = new ComplexDouble2DFFT(Lp, Lp);
		fftData = new double[2*Lp*Lp];
	}
	
	public Accumulator getAccumulatorC() {
		return accCircle;
	}

	public Accumulator getAccumulatorH() {
		return accHorizontal;
	}
	
	public Accumulator getAccumulatorV() {
		return accVertical;
	}

	public Accumulator getAccumulatorVA() {
		return accAvV;
	}

	public Accumulator getAccumulatorHA() {
		return accAvH;
	}
	
	public Accumulator getAccumulatorCA() {
		return accAvC;
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
	
	public void accumulate(double[] xs, double[] ys) {
		for (int i = 0; i < Lp*Lp; i++)
			fftData[2*i] = fftData[2*i+1] = 0;
		for (int k = 0; k < xs.length; k++) {
			int i = (int)(Lp*xs[k]/L);
			int j = (int)(Lp*ys[k]/L);
			assert(i < Lp && j < Lp);
			fftData[2*(Lp*j+i)]++;
		}
		accumulateAux();
	}
	
	public void accumulate(double[] data) {
		double dx = (L/Lp);
		for (int i = 0; i < Lp*Lp; i++) {
			fftData[2*i] = data[i]*dx*dx;
			fftData[2*i+1] = 0;
		}
		accumulateAux();
	}

	public void accumulateAll(double[] data) {
		double dx = (L/Lp);
		for (int i = 0; i < Lp*Lp; i++) {
			fftData[2*i] = data[i]*dx*dx;
			fftData[2*i+1] = 0;
		}
		accumulateAllAux();
	}
	
	public void accumulateAllAux() {
		// compute fourier transform
		fft.transform(fftData);
		fftData = fft.toWraparoundOrder(fftData);

		//Instead of a circular average, we want the structure factor in the vertical and
		//horizontal directions.

		//vertical component
		for (int y = -Lp/2; y < Lp/2; y++) {
			int x=0;
			double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
			if (kR >= kRmin && kR <= kRmax) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				double re = fftData[2*i];
				double im = fftData[2*i+1];
				accVertical.accum(kR, (re*re + im*im)/(L*L));
				accAvV.accum(kR, (re*re + im*im)/(L*L));
			}
		}
		//horizontal component
		for (int x = -Lp/2; x < Lp/2; x++) {
			int y=0;
			double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
			if (kR >= kRmin && kR <= kRmax) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				double re = fftData[2*i];
				double im = fftData[2*i+1];
				accHorizontal.accum(kR, (re*re + im*im)/(L*L));
				accAvH.accum(kR, (re*re + im*im)/(L*L));
			}
		}		
	
		//circularly averaged
		
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
				if (kR >= kRmin && kR <= kRmax) {
					int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double re = fftData[2*i];
					double im = fftData[2*i+1];
					accCircle.accum(kR, (re*re + im*im)/(L*L));
					accAvC.accum(kR, (re*re + im*im)/(L*L));
				}
			}
		}		
		
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
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
				if (kR >= kRmin && kR <= kRmax) {
					int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double re = fftData[2*i];
					double im = fftData[2*i+1];
					accCircle.accum(kR, (re*re + im*im)/(L*L));
				}
			}
		}
	}
}
