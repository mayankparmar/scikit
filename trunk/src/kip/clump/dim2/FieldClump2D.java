package kip.clump.dim2;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.hypot;
import static kip.util.MathPlus.sqr;
import scikit.dataset.Accumulator;
import scikit.numerics.fft.util.FFT2D;
import scikit.numerics.fn.Function2D;
import scikit.params.Parameters;
import scikit.util.DoubleArray;

public class FieldClump2D extends AbstractClump2D {
	int Lp;
	double dt, t;
	double[] del_phi;
	boolean[] onBoundary;
	int elementsInsideBoundary;
	FFT2D fft;
	boolean fixedBoundary = false;
	boolean noiselessDynamics = false;
	
	public double[] phi, phi_bar;
	public boolean rescaleClipped = false; // indicates saddle point invalid
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	
	public FieldClump2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		R = params.fget("R");
		L = params.fget("L");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
		Lp = Integer.highestOneBit((int)rint(L/dx));
		dx = L / Lp;
		params.set("dx", dx);
		allocate();
		
		t = 0;
		for (int i = 0; i < Lp*Lp; i++)
			phi[i] = DENSITY;
	}
	
	public void halveResolution() {
		int old_Lp = Lp;
		double[] old_phi = phi; 
		Lp /= 2;
		dx *= 2.0;
		allocate();
		for (int y = 0; y < Lp; y++) {
			for (int x = 0; x < Lp; x++) {
				phi[y*Lp+x] = old_phi[2*y*old_Lp + 2*x];
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
		for (int y = 0; y < Lp; y++) {
			for (int x = 0; x < Lp; x++) {
				phi[y*Lp+x] = old_phi[(y/2)*old_Lp + (x/2)];
			}
		}
		fixBoundaryConditions();
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
			phi[i] = DENSITY*(1+mag*random.nextGaussian()/5);
		}
		
		double mean = mean(phi);
		for (int i = 0; i < Lp*Lp; i++)
			if (!onBoundary[i])
				phi[i] += (DENSITY-mean);
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
		for (int i = 0; i < Lp*Lp; i++)
			var += sqr(phi[i]-DENSITY);
		return var / (Lp*Lp);
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
		for (int i = 0; i < Lp*Lp; i++) {
			phi[i] = (phi[i]-DENSITY)*scale + DENSITY;
		}
	}
	
	public void circularAverage() {
		Accumulator acc = new Accumulator(1);
		acc.setAveraging(true);
		for (int i = 0; i < Lp*Lp; i++) {
			double x = i%Lp - Lp/2.;
			double y = i/Lp - Lp/2.;
			acc.accum(sqrt(x*x+y*y), phi[i]);
		}
		for (int i = 0; i < Lp*Lp; i++) {
			double x = i%Lp - Lp/2.;
			double y = i/Lp - Lp/2.;
			phi[i] = acc.eval(sqrt(x*x+y*y));
		}
	}
	
	public double mean(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i];
		return sum/elementsInsideBoundary; 
	}
	
	
	public double meanSquared(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
			if (!onBoundary[i])
				sum += a[i]*a[i];
		return sum/elementsInsideBoundary;
	}
	
	
	public void simulate() {
		fft.convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				return potential(hypot(k1,k2)*R);
			}
		});
		
		for (int i = 0; i < Lp*Lp; i++) {
			del_phi[i] = - dt*(phi_bar[i]+T*log(phi[i])) + sqrt(dt*2*T/(dx*dx))*noise();
		}
		double mu = mean(del_phi)-(DENSITY-mean(phi));
		for (int i = 0; i < Lp*Lp; i++) {
			// clip del_phi to ensure phi(t+dt) > phi(t)/2
			del_phi[i] = max(del_phi[i]-mu, -phi[i]/2.);
		}
		
		rms_dF_dphi = 0;
		freeEnergyDensity = 0;
		for (int i = 0; i < Lp*Lp; i++) {
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
		fft.convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				double k = hypot(k1, k2);
				return dpotential_dkR(k*R)*k;
			}
		});
		double ret = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			ret += 0.5*phi[i]*dphibar_dR[i];
		}
		return ret / (Lp*Lp);
	}
	
	public Accumulator newStructureAccumulator(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		Accumulator ret = new Accumulator(binWidth);
		ret.setAveraging(true);
		return ret;
	}
	
	public void accumulateStructure(final Accumulator sf) {
		fft.transform(phi, new FFT2D.MapFn() {
			public void apply(double k1, double k2, double re, double im) {
				double k = hypot(k1, k2);
				double kR = k*R;
				if (kR > 0 && kR <= 4*KR_SP)
					sf.accum(kR, (re*re+im*im)/(L*L));
			}
		});
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
		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		del_phi = new double[Lp*Lp];
		onBoundary = new boolean[Lp*Lp];
		elementsInsideBoundary = Lp*Lp;
		fft = new FFT2D(Lp, Lp);
		fft.setLengths(L, L);
	}
	
	private double noise() {
		return noiselessDynamics ? 0 : random.nextGaussian();
	}
	
	private void fixBoundaryConditions() {
		int thickness = (int)ceil(0.5*R/dx);
		for (int i = 0; i < thickness; i++) {
			int j = Lp-thickness+i;
			for (int k = 0; k < Lp; k++) {
				onBoundary[i*Lp+k] = onBoundary[j*Lp+k] = fixedBoundary;
				onBoundary[k*Lp+i] = onBoundary[k*Lp+j] = fixedBoundary;
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
}
