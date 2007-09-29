package kip.clump.dim3;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.rint;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;
import kip.util.DoubleArray;
import scikit.numerics.fft.ComplexDouble3DFFT;
import scikit.params.Parameters;

public class FieldClump3D extends AbstractClump3D {
	int Lp;
	double dt, t;
	double[] phi, phi_bar, del_phi;
	boolean[] onBoundary;
	int elementsInsideBoundary;
	ComplexDouble3DFFT fft;	// Object to perform transforms
	double[] fftScratch;
	
	boolean fixedBoundary = false;
	boolean noiselessDynamics = false;
	public boolean rescaleClipped = false; // indicates saddle point invalid
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	
	public FieldClump3D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		R = params.fget("R");
		L = params.fget("L");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("dx", dx);
		allocate();
		
		t = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++)
			phi[i] = DENSITY;
	}
	
	public void halveResolution() {
		int old_Lp = Lp;
		double[] old_phi = phi; 
		Lp /= 2;
		dx *= 2.0;
		allocate();
		for (int z = 0; z < Lp; z++) {
			for (int y = 0; y < Lp; y++) {
				for (int x = 0; x < Lp; x++) {
					phi[z*Lp*Lp + y*Lp + x] = old_phi[2*z*old_Lp*old_Lp + 2*y*old_Lp + 2*x];
				}
			}
		}
		fixBoundaryConditions();
	}
	
	public void doubleResolution() {
		int old_Lp = Lp;
		double[] old_phi = phi; 
		Lp *= 2;
		dx /= 2.0;
		allocate();
		for (int z = 0; z < Lp; z++) {
			for (int y = 0; y < Lp; y++) {
				for (int x = 0; x < Lp; x++) {
					phi[z*Lp*Lp + y*Lp + x] = old_phi[(z/2)*old_Lp*old_Lp + (y/2)*old_Lp + (x/2)];
				}
			}
		}
		fixBoundaryConditions();
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
	}
	
	
	public void initializeFieldWithSeed(String type) {
  		for (int i = 0; i < Lp*Lp*Lp; i++) {
			if (onBoundary[i])
				continue;
			
			double x = dx*(i%Lp - Lp/2);
			double y = dx*((i%(Lp*Lp))/Lp - Lp/2);
			double z = dx*((i/(Lp*Lp)) - Lp/2);
			double field = 0;
			double k = KR_SP/R;
			if (type.equals("BCC")) {
				field = 0;
				// BCC (reciprocal lattice is FCC)
				field += 0.1*cos(k * ( x + z) / sqrt(3.));
				field += 0.1*cos(k * (-x + z) / sqrt(3.));
				field += 0.1*cos(k * ( y + z) / sqrt(3.));
				field += 0.1*cos(k * (-y + z) / sqrt(3.));
			}
			else if (type.equals("Noise")) {
				// random
				field = random.nextGaussian();
				break;
			}
			phi[i] = DENSITY*(1+field);
		}
	}
	
	public void useNoiselessDynamics(boolean b) {
		noiselessDynamics = b;
	}
	
	public void useFixedBoundaryConditions(boolean b) {
		fixedBoundary = b;
		fixBoundaryConditions();
	}
	
	public double phiVariance() {
		double var = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++)
			var += sqr(phi[i]-DENSITY);
		return var / (Lp*Lp*Lp);
	}
	
	
	public void scaleField(double scale) {
		// phi will not be scaled above PHI_UB or below PHI_LB
		double PHI_UB = 5;
		double PHI_LB = 0.01;
		double s1 = (PHI_UB-DENSITY)/(DoubleArray.max(phi)-DENSITY+1e-10);
		double s2 = (PHI_LB-DENSITY)/(DoubleArray.min(phi)-DENSITY-1e-10);
		rescaleClipped = scale > min(s1,s2);
		if (rescaleClipped)
			scale = min(s1,s2);
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			phi[i] = (phi[i]-DENSITY)*scale + DENSITY;
		}
	}
	
	
	public double mean(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i];
		return sum/elementsInsideBoundary; 
	}
	
	
	public double meanSquared(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i]*a[i];
		return sum/elementsInsideBoundary;
	}
	
	
	public void simulate() {
		convolveWithRange(phi, phi_bar, R);
		
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			del_phi[i] = - dt*(phi_bar[i]+T*log(phi[i])) + sqrt(dt*2*T/(dx*dx*dx))*noise();
		}
		double mu = mean(del_phi)-(DENSITY-mean(phi));
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			// clip del_phi to ensure phi(t+dt) > phi(t)/2
			del_phi[i] = max(del_phi[i]-mu, -phi[i]/2.);
		}
		
		rms_dF_dphi = 0;
		freeEnergyDensity = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			if (!onBoundary[i]) {
				rms_dF_dphi += sqr(del_phi[i] / dt);
				freeEnergyDensity += 0.5*phi[i]*phi_bar[i]+T*phi[i]*log(phi[i]);
				phi[i] += del_phi[i];
			}
		}
		rms_dF_dphi = sqrt(rms_dF_dphi/elementsInsideBoundary);
		freeEnergyDensity /= elementsInsideBoundary;
		freeEnergyDensity -= 0.5;
		t += dt;
	}
	
	public double dFdensity_dR() {
		double[] dphibar_dR = phi_bar;
		convolveWithRangeDerivative(phi, dphibar_dR, R);
		double ret = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			ret += 0.5*phi[i]*dphibar_dR[i];
		}
		return ret / (Lp*Lp*Lp);
	}
	
	public StructureFactor3D newStructureFactor(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		return new StructureFactor3D(Lp, L, R, binWidth);
	}
	
	
	public void accumulateIntoStructureFactor(StructureFactor3D sf) {
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
	
	
	private void allocate() {
		phi = new double[Lp*Lp*Lp];
		phi_bar = new double[Lp*Lp*Lp];
		del_phi = new double[Lp*Lp*Lp];
		onBoundary = new boolean[Lp*Lp*Lp];
		elementsInsideBoundary = Lp*Lp*Lp;
		fftScratch = new double[2*Lp*Lp*Lp];
		fft = new ComplexDouble3DFFT(Lp, Lp, Lp);
	}
	
	private double noise() {
		return noiselessDynamics ? 0 : random.nextGaussian();
	}

	static private int minSeq(Integer... vals) {
		int ret = Integer.MAX_VALUE;
		for (int v : vals)
			ret = Math.min(ret, v);
		return ret;
	}
	
	private void fixBoundaryConditions() {
		elementsInsideBoundary = Lp*Lp*Lp;
		int thickness = (int)ceil(0.5*R/dx);
		
		for (int x = 0; x < Lp; x++) {
			for (int y = 0; y < Lp; y++) {
				for (int z = 0; z < Lp; z++) {
					int d = minSeq(x, y, z, Lp-x, Lp-y, Lp-z);
					if (d < thickness && fixedBoundary) {
						int i = Lp*Lp*z + Lp*y + x;
						onBoundary[i] = true;
						phi[i] = DENSITY;
						elementsInsideBoundary--;
					}
				}
			}
		}
	}
	
	private void convolveWithRange(double[] src, double[] dest, double R) {
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int z = -Lp/2; z < Lp/2; z++) {
			for (int y = -Lp/2; y < Lp/2; y++) {
				for (int x = -Lp/2; x < Lp/2; x++) {
					int i = Lp*Lp*((z+Lp)%Lp) + Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double k = 2*PI*sqrt(x*x+y*y+z*z)/L;
					double J = potential(k*R);
					fftScratch[2*i] *= J;
					fftScratch[2*i+1] *= J;
				}
			}
		}
		fft.backtransform(fftScratch);
		
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			dest[i] = fftScratch[2*i] / (Lp*Lp*Lp);
		}		
	}

	private void convolveWithRangeDerivative(double[] src, double[] dest, double R) {
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int z = -Lp/2; z < Lp/2; z++) {
			for (int y = -Lp/2; y < Lp/2; y++) {
				for (int x = -Lp/2; x < Lp/2; x++) {
					int i = Lp*Lp*((z+Lp)%Lp) + Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double k = 2*PI*sqrt(x*x+y*y+z*z)/L;
					double dJ_dR = dpotential_dkR(k*R)*k;
					fftScratch[2*i] *= dJ_dR;
					fftScratch[2*i+1] *= dJ_dR;
				}
			}
		}
		fft.backtransform(fftScratch);
		
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			dest[i] = fftScratch[2*i] / (Lp*Lp*Lp);
		}		
	}
}