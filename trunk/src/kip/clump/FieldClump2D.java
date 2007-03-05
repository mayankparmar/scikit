package kip.clump;

import static java.lang.Math.*;
import static kip.util.MathPlus.j1;
import jnt.FFT.ComplexDouble2DFFT;
import scikit.jobs.Parameters;

public class FieldClump2D extends AbstractClump2D {
	int Lp;
	double dx;
	double t;
	boolean noiseless;
	double[] phi, phibar;
	double[] scratch;
	ComplexDouble2DFFT fft;	// Object to perform transforms

	
	public FieldClump2D(Parameters params, boolean noiseless) {
		this.noiseless = noiseless;
		random.setSeed(params.iget("Random seed", 0));
		
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = params.fget("dx");
		
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("dx", dx);
		
		t = 0;

		phi = new double[Lp*Lp];
		phibar = new double[Lp*Lp];
		for (int i = 0; i < Lp*Lp; i++)
			phi[i] = DENSITY*(1 + (random.nextDouble()-0.5)/10);
		
		scratch = new double[2*Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp, Lp);
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
	}
	
	
	void convolveWithRange(double[] src, double[] dest, double R) {
		for (int i = 0; i < Lp*Lp; i++) {
			scratch[2*i] = src[i];
			scratch[2*i+1] = 0;
		}
		
		fft.transform(scratch);
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L) * R;
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				double J = (kR == 0 ? 1 : 2*j1(kR)/kR);
				scratch[2*i] *= J;
				scratch[2*i+1] *= J;
			}
		}
		fft.backtransform(scratch);
		
		for (int i = 0; i < Lp*Lp; i++) {
			dest[i] = scratch[2*i] / (Lp*Lp);
		}		
	}
	
	double smallest(double[] a) {
		double m = a[0];
		for (int i = 0; i < a.length; i++)
			if (a[i] < m)
				m = a[i];
		return m;
	}
	
	double mean(double[] a, int len) {
		double s = 0;
		for (int i = 0; i < len; i++)
			s += a[i];
		return s / len;
	}
	
	double meanSquared(double[] a, int len) {
		double s = 0;
		for (int i = 0; i < len; i++)
			s += a[i]*a[i];
		return s / len;
	}
	
	double noise() {
		return noiseless ? 0 : random.nextGaussian();
	}


	public void simulate(double mcs) {
		int iters = max((int) (mcs / 0.01), 1);
		double dt = mcs / iters;

//		for (int j = 0; j < iters; j++) {
//			convolveWithRange(phi, phibar, R);
//			for (int i = 0; i < Lp*Lp; i++) {
//				scratch[i] = - dt*(phibar[i]+T*log(phi[i])); //+ sqrt(dt*phi2*2*T/dx)*noise();
//			}
//			double a = mean(scratch, Lp*Lp);
//			for (int i = 0; i < Lp*Lp; i++) {
//				phi[i] += scratch[i] - a;
//			}
//		}

		for (int j = 0; j < iters; j++) {
			convolveWithRange(phi, phibar, R);
			for (int i = 0; i < Lp*Lp; i++) {
				double phi2 = phi[i] * phi[i];
				scratch[i] = - phi2*dt*(phibar[i]+T*log(phi[i])); // + sqrt(dt*phi2*2*T/dx)*noise();
			}
			double a = mean(scratch, Lp*Lp);
			for (int i = 0; i < Lp*Lp; i++) {
				phi[i] += scratch[i] - a;
			}
//			double a = mean(scratch, Lp*Lp) / meanSquared(phi, Lp*Lp);
//			for (int i = 0; i < Lp*Lp; i++) {
//				double phi2 = phi[i] * phi[i];
//				phi[i] += scratch[i] - a * phi2;
//			}
		}
		
		System.out.println(mean(phi, Lp*Lp));
		System.out.println(smallest(phi));
		t += mcs;
	}
	
	public StructureFactor newStructureFactor(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		return new StructureFactor(Lp, L, R, binWidth);
	}
	
	public void accumulateIntoStructureFactor(StructureFactor sf) {
		sf.accumulate(phi);
	}
	
	public double[] coarseGrained() {
		return phi;
	}
	
	public int numColumns() {
		return Lp;
	}
	
	public double time() {
		return t;
	}
}
