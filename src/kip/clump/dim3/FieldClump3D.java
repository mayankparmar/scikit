package kip.clump.dim3;

import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.rint;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.hypot;
import static kip.util.MathPlus.sqr;
import scikit.dataset.Accumulator;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.util.FFT3D;
import scikit.numerics.fn.Function3D;
import scikit.util.DoubleArray;

public class FieldClump3D extends AbstractClump3D {
	int Lp;
	double t;
	double[] phi, phi_bar, del_phi;
	boolean[] onBoundary;
	int elementsInsideBoundary;
	FFT3D fft;
	boolean fixedBoundary = false;
	boolean noiselessDynamics = false;

	public double dt;
	public double Rx, Ry, Rz;
	public boolean rescaleClipped = false; // indicates saddle point invalid
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	
	public FieldClump3D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		Rx = Ry = Rz = params.fget("R");
		L = params.fget("L");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
		Lp = Integer.highestOneBit((int)rint(L/dx));
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
			
			double R = Rx;
			double x = dx*(i%Lp - Lp/2);
			double y = dx*((i%(Lp*Lp))/Lp - Lp/2);
			double z = dx*((i/(Lp*Lp)) - Lp/2);
			double field = 0;
			double k = KR_SP/R;
			if (type.equals("BCC")) {
				field = 0;
				// BCC (reciprocal lattice is FCC)
				field += cos(k * ( x + z) / sqrt(2));
				field += cos(k * (-x + z) / sqrt(2));
				field += cos(k * ( y + z) / sqrt(2));
				field += cos(k * (-y + z) / sqrt(2));
			}
			else if (type.equals("Triangle")) {
				double rad = 0.2*R;
				double sigma = 0.05*R;
				double x0 = rad, y0 = 0, z0 = 0;
				field += Math.exp((-sqr(x-x0)-sqr(y-y0)-sqr(z-z0))/(2*sqr(sigma)));				
				x0 = rad*cos(2*Math.PI/3);
				y0 = rad*Math.sin(2*Math.PI/3);
				field += Math.exp((-sqr(x-x0)-sqr(y-y0)-sqr(z-z0))/(2*sqr(sigma)));
				x0 = rad*cos(4*Math.PI/3);
				y0 = rad*Math.sin(4*Math.PI/3);
				field += Math.exp((-sqr(x-x0)-sqr(y-y0)-sqr(z-z0))/(2*sqr(sigma)));
			}
			else if (type.equals("Noise")) {
				// random
				field = random.nextGaussian();
			}
			double r = sqrt(x*x+y*y+z*z);
			double mag = 0.2 / (1+sqr(r/R));
			phi[i] = DENSITY*(1+mag*field);
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
//		double PHI_UB = 1.5*DENSITY;
//		double PHI_LB = 0.5*DENSITY;
//		double s1 = (PHI_UB-DENSITY)/(DoubleArray.max(phi)-DENSITY+1e-10);
//		double s2 = (PHI_LB-DENSITY)/(DoubleArray.min(phi)-DENSITY-1e-10);
//		rescaleClipped = scale > min(s1,s2);
//		if (rescaleClipped)
//			scale = min(s1,s2);
//		System.out.println(scale);
		
		double PHI_UB = 20;
		double PHI_LB = 0.01;
		for (int i = 0; i < Lp*Lp*Lp; i++)
			phi[i] = (phi[i]-DENSITY)*scale + DENSITY;
		rescaleClipped = DoubleArray.min(phi) < PHI_LB || DoubleArray.max(phi) > PHI_UB;
		if (rescaleClipped) {
			for (int i = 0; i < Lp*Lp*Lp; i++)
				phi[i] = min(max(phi[i], PHI_LB), PHI_UB);
//			double mean = DoubleArray.mean(phi);
//			for (int i = 0; i < Lp*Lp*Lp; i++)
//				phi[i] = DENSITY + (phi[i] - mean);
		}
//		if (DoubleArray.min(phi) < PHI_LB || DoubleArray.max(phi) > PHI_UB) {
//			throw new IllegalArgumentException("doh");
//		}
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
		fft.convolve(phi, phi_bar, new Function3D() {
			public double eval(double k1, double k2, double k3) {
				return potential(hypot(k1*Rx,k2*Ry,k3*Rz));
			}
		});
		
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
	
	public double dFdensity_dRx() {
		double[] dphibar_dR = phi_bar;
		fft.convolve(phi, phi_bar, new Function3D() {
			public double eval(double k1, double k2, double k3) {
				double kR = hypot(k1*Rx, k2*Ry, k3*Rz);
				double dkR_dRx = k1 == 0 ? 0 : (k1*k1*Rx / kR);
				return dpotential_dkR(kR)*dkR_dRx;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp*Lp);
	}
	
	public double dFdensity_dRy() {
		double[] dphibar_dR = phi_bar;
		fft.convolve(phi, phi_bar, new Function3D() {
			public double eval(double k1, double k2, double k3) {
				double kR = hypot(k1*Rx, k2*Ry, k3*Rz);
				double dkR_dRy = k2 == 0 ? 0 : (k2*k2*Ry / kR);
				return dpotential_dkR(kR)*dkR_dRy;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp*Lp);
	}
	
	public double dFdensity_dRz() {
		double[] dphibar_dR = phi_bar;
		fft.convolve(phi, phi_bar, new Function3D() {
			public double eval(double k1, double k2, double k3) {
				double kR = hypot(k1*Rx, k2*Ry, k3*Rz);
				double dkR_dRz = k3 == 0 ? 0 : (k3*k3*Rz / kR);
				return dpotential_dkR(kR)*dkR_dRz;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp*Lp);
	}
	
	public Accumulator newStructureAccumulator(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		Accumulator ret = new Accumulator(binWidth);
		ret.setAveraging(true);
		return ret;
	}
	
	public void accumulateStructure(final Accumulator sf) {
		fft.transform(phi, new FFT3D.MapFn() {
			public void apply(double k1, double k2, double k3, double re, double im) {
				double kR = hypot(Rx*k1, Ry*k2, Rz*k3);
				if (kR > 0 && kR <= 4*KR_SP)
					sf.accum(kR, (re*re+im*im)/(L*L*L));
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
		phi = new double[Lp*Lp*Lp];
		phi_bar = new double[Lp*Lp*Lp];
		del_phi = new double[Lp*Lp*Lp];
		onBoundary = new boolean[Lp*Lp*Lp];
		elementsInsideBoundary = Lp*Lp*Lp;
		fft = new FFT3D(Lp, Lp, Lp);
		fft.setLengths(L, L, L);
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
}