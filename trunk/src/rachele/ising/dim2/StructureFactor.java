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
	public double sFactor [];
	int Lp;                 // # elements per side
	double L;               // the actual system length, L = Lp*dx, where dx is lattice spacing
	double R;               // characteristic length.  x-axis is k*R.
	double kRmin, kRmax;
	static double squarePeakValue = 4.4934092;
	static double circlePeakValue = 5.135622302;
	int squarePeakInt, circlePeakInt;
	
	Accumulator accCircle;
	Accumulator accHorizontal;
	Accumulator accVertical;
	Accumulator accAvH;
	Accumulator accAvV;
	Accumulator accAvC;
	Accumulator accPeakH;
	Accumulator accPeakV;
	Accumulator accPeakC;
	
	public StructureFactor(int Lp, double L, double R, double kRbinWidth, double dt) {
		this.Lp = Lp;
		this.L = L;
		this.R = R;
		
		sFactor = new double [Lp*Lp];
		
		kRmin = (2*PI*2/L)*R; // explicitly exclude constant (k=0) mode
		kRmax = (2*PI*(Lp/2)/L)*R;
		
		double dblePeakLength = squarePeakValue*L/(2*PI*R);
		squarePeakInt = (int)dblePeakLength;
		if(abs(2*PI*squarePeakInt*R/L - squarePeakValue) >= abs(2*PI*(squarePeakInt+1)*R/L - squarePeakValue))
			squarePeakInt = squarePeakInt + 1;
//
//		dblePeakLength = circlePeakValue*L/(2*PI*R);
//		circlePeakInt = (int)dblePeakLength;
//		if(abs(2*PI*circlePeakInt*R/L - circlePeakValue) >= abs(2*PI*(circlePeakInt+1)*R/L - circlePeakValue))
//			circlePeakInt = circlePeakInt + 1;
		
		accCircle = new Accumulator(kRbinWidth);
		accHorizontal = new Accumulator(kRbinWidth);
		accVertical = new Accumulator(kRbinWidth);
		accAvH = new Accumulator(kRbinWidth);
		accAvV = new Accumulator(kRbinWidth);
		accAvC = new Accumulator(kRbinWidth);
		accPeakH = new Accumulator(dt);
		accPeakV = new Accumulator(dt);
		accPeakC = new Accumulator(dt);
		
		accAvH.setAveraging(true);		
		accAvV.setAveraging(true);
		accAvC.setAveraging(true);
		accCircle.setAveraging(true);
		accHorizontal.setAveraging(true);
		accVertical.setAveraging(true);
		accPeakH.setAveraging(true);
		accPeakV.setAveraging(true);
		accPeakC.setAveraging(true);
		
		fft = new ComplexDouble2DFFT(Lp, Lp);
		fftData = new double[2*Lp*Lp];
	}

	public Accumulator getPeakV() {
		return accPeakV;
	}
	
	public Accumulator getPeakH() {
		return accPeakH;
	}

	public Accumulator getPeakC() {
		return accPeakC;
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

	public void accumulateAll(double t, double[] data) {
		double dx = (L/Lp);
		for (int i = 0; i < Lp*Lp; i++) {
			fftData[2*i] = data[i]*dx*dx;
			fftData[2*i+1] = 0;
		}
		accumulateAllAux(t);
	}
	
	public void accumulateAllAux(double t) {
		// compute fourier transform
		fft.transform(fftData);
		fftData = fft.toWraparoundOrder(fftData);

		for (int i=0; i < Lp*Lp; i++){
			double re = fftData[2*i];
			double im = fftData[2*i+1];
			sFactor[i] = (re*re + im*im)/(L*L);
		}
		
		//Instead of a circular average, we want the structure factor in the vertical and
		//horizontal directions.

		//vertical component
		for (int y = -Lp/2; y < Lp/2; y++) {
			int x=0;
			double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
			if (kR >= kRmin && kR <= kRmax) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				accVertical.accum(kR, sFactor[i]);
				accAvV.accum(kR, sFactor[i]);
				if(abs(y) == squarePeakInt){
					accPeakV.accum(t, sFactor[i]);
				}
			}
		}
		
		//horizontal component
		for (int x = -Lp/2; x < Lp/2; x++) {
			int y=0;
			double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
			if (kR >= kRmin && kR <= kRmax) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				//double re = fftData[2*i];
				//double im = fftData[2*i+1];
				//double sfValue = (re*re + im*im)/(L*L);
				accHorizontal.accum(kR, sFactor[i]);
				accAvH.accum(kR, sFactor[i]);
				if(abs(x) == squarePeakInt){
					accPeakH.accum(t, sFactor[i]);
				}
			}
		}		
	
		//circularly averaged	
		double [] sfValues = new double [Lp*Lp];
		int count = 0;
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L)*R;
				if (kR >= kRmin && kR <= kRmax) {
					int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
//					double re = fftData[2*i];
//					double im = fftData[2*i+1];
//					double sfValue = (re*re + im*im)/(L*L);
					accCircle.accum(kR, sFactor[i]);
					accAvC.accum(kR, sFactor[i]);
					sfValues[count] = sFactor[i];
					count += 1;
					if(kR >= circlePeakValue - PI*R/(3.0*L) && kR <= circlePeakValue + PI*R/(3.0*L)){
						accPeakC.accum(t, sFactor[i]);
						//System.out.println(t + " " + kR);
					}
				}
			}
		}
		
		//accPeakC.accum(t, kip.util.DoubleArray.max(sfValues));
		
		//shift sfactor so that origin is in center
		double [] temp = new double [Lp*Lp];
		for (int i = 0; i<Lp*Lp; i++){
			int x = i%Lp;
			int y = i/Lp;
			x += Lp/2; y += Lp/2;
			x = x%Lp; y = y%Lp;
			int j = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
			temp[j] = sFactor[i];
		}
		for(int i = 0; i<Lp*Lp; i++)
			sFactor[i] = temp[i];
		
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
