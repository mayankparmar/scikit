package kip.clump.dim2;

import static java.lang.Math.*;
import static kip.util.MathPlus.*;
import static kip.util.DoubleArray.*;
import jnt.FFT.ComplexDouble2DFFT;
import scikit.params.Parameters;

public class FieldClump2D extends AbstractClump2D {
	int Lp;
	double dt, t;
	double[] phi, phi_bar, del_phi;
	boolean[] onBoundary;
	int elementsInsideBoundary;
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftScratch;
	
	boolean unstableDynamics = false;
	boolean noiselessDynamics = false;
	public boolean rescaleClipped = false;
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	
	public FieldClump2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("dx", dx);
		
		t = 0;

		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		del_phi = new double[Lp*Lp];
		onBoundary = new boolean[Lp*Lp];
		elementsInsideBoundary = Lp*Lp;
		
		fftScratch = new double[2*Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp, Lp);
		
		for (int i = 0; i < Lp*Lp; i++)
			phi[i] = DENSITY;
	}
	
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
	}
	
	
	public void initializeFieldWithSeed() {
		for (int i = 0; i < Lp*Lp; i++) {
			if (onBoundary[i])
				continue;
			
			double x = dx*(i%Lp - Lp/2);
			double y = dx*(i/Lp - Lp/2);
			double r = sqrt(x*x+y*y);
			double mag = 0.8 / (1+sqr(r/R));
			
			double kR = KR_SP; // it's fun to try different values
			double x1 = x*cos(1*PI/6) + y*sin(1*PI/6);
			double x2 = x*cos(3*PI/6) + y*sin(3*PI/6);
			double x3 = x*cos(5*PI/6) + y*sin(5*PI/6);
			phi[i] = DENSITY*(1+mag*(cos(x1*kR/R) + cos(x2*kR/R) + cos(x3*kR/R)));
			
			// uncomment for four fold symmetry 
//			phi[i] = DENSITY*(1+mag*(cos(x*kR/R) + cos(y*kR/R)));
			
			// uncomment for random initial condition
//			phi[i] = DENSITY*(1+mag*random.nextGaussian()/5);
		}
	}
	
	
	void convolveWithRange(double[] src, double[] dest, double R) {
		for (int i = 0; i < Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				double kR = (2*PI*sqrt(x*x+y*y)/L) * R;
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				double J = (kR == 0 ? 1 : 2*j1(kR)/kR);
				fftScratch[2*i] *= J;
				fftScratch[2*i+1] *= J;
			}
		}
		fft.backtransform(fftScratch);
		
		for (int i = 0; i < Lp*Lp; i++) {
			dest[i] = fftScratch[2*i] / (Lp*Lp);
		}		
	}
	
	
	public void useNoiselessDynamics() {
		noiselessDynamics = true;
	}
	
	
	public void useFixedBoundaryConditions() {
		int thickness = 4;
		for (int i = 0; i < thickness; i++) {
			int j = Lp-thickness+i;
			for (int k = 0; k < Lp; k++) {
				onBoundary[i*Lp+k] = onBoundary[j*Lp+k] = true;
				onBoundary[k*Lp+i] = onBoundary[k*Lp+j] = true;
			}
		}
		elementsInsideBoundary = Lp*Lp;
		for (int i = 0; i < Lp*Lp; i++) {
			if (onBoundary[i]) {
				phi[i] = DENSITY;
				elementsInsideBoundary--;
			}
		}
	}
	
	
	public double phiVariance() {
		double var = 0;
		for (int i = 0; i < Lp*Lp; i++)
			var += sqr(phi[i]-DENSITY);
		return var / (Lp*Lp);
	}
	
	
	public void scaleField(double scale) {
		// phi will not be scaled above PHI_UB or below PHI_LB
		double PHI_UB = 5;
		double PHI_LB = 0.01;
		double s1 = (PHI_UB-DENSITY)/(max(phi)-DENSITY+1e-10);
		double s2 = (PHI_LB-DENSITY)/(min(phi)-DENSITY-1e-10);
		rescaleClipped = scale > min(s1,s2);
		if (rescaleClipped)
			scale = min(s1,s2);
		for (int i = 0; i < Lp*Lp; i++) {
			phi[i] = (phi[i]-DENSITY)*scale + DENSITY;
		}
	}
	
	
	double noise() {
		return noiselessDynamics ? 0 : random.nextGaussian();
	}

	
	double mean(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i];
		return sum/elementsInsideBoundary; 
	}
	
	
	double meanSquared(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i]*a[i];
		return sum/elementsInsideBoundary;
	}
	
	
	public void simulate() {
		convolveWithRange(phi, phi_bar, R);
		
		if (unstableDynamics) {
			for (int i = 0; i < Lp*Lp; i++) {
				del_phi[i] = - dt*(phi_bar[i]+T*log(phi[i])) + sqrt(dt*2*T/dx)*noise();
			}
			double mu = mean(del_phi)-(DENSITY-mean(phi));
			for (int i = 0; i < Lp*Lp; i++) {
				del_phi[i] -= mu;
			}
		}
		else {
			for (int i = 0; i < Lp*Lp; i++) {
				double phi2 = phi[i] * phi[i];
				del_phi[i] = - phi2*dt*(phi_bar[i]+T*log(phi[i])) + sqrt(phi2*dt*2*T/(dx*dx))*noise();
			}
			double mu = (mean(del_phi)-(DENSITY-mean(phi))) / meanSquared(phi);
			for (int i = 0; i < Lp*Lp; i++) {
				del_phi[i] -= mu*phi[i]*phi[i];
			}
		}
		
		rms_dF_dphi = 0;
		freeEnergyDensity = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			if (!onBoundary[i]) {
				rms_dF_dphi += sqr(del_phi[i] / (dt*phi[i]*phi[i]));
				freeEnergyDensity += phi[i]*phi_bar[i]+T*phi[i]*log(phi[i]);
				phi[i] += del_phi[i];
			}
		}
		rms_dF_dphi = sqrt(rms_dF_dphi/elementsInsideBoundary);
		freeEnergyDensity /= elementsInsideBoundary;
		t += dt;
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
