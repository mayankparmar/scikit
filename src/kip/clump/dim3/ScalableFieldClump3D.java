package kip.clump.dim3;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;
import kip.util.Random;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.ComplexDouble3DFFT;
import scikit.util.DoubleArray;

public class ScalableFieldClump3D {
	public static final double DENSITY = 1;
	public static final double KR_SP = 5.76345919689454979140645;
	public static final double T_SP = 0.08617089416190739793014991;

	public boolean noiselessDynamics = false;
	public double Rx, Ry, Rz;
	public double L, T, dx;	
	public double dt, t;
	public int Lp;
	public double[] phi;
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	double[] phi_bar, del_phi;
	ComplexDouble3DFFT fft;
	double[] fftScratch;
	Random random = new Random();
	
	
	public double potential(double kR) {
		return (kR == 0) ? 1 : (3/(kR*kR))*(sin(kR)/kR - cos(kR));
	}
	
	public double dpotential_dkR(double kR) {
		double kR2 = kR*kR;
		double kR3 = kR2*kR;
		double kR4 = kR2*kR2;
		return (kR == 0) ? 0 : (9*cos(kR)/kR3 + 3*(kR2-3)*sin(kR)/kR4);
	}

	public ScalableFieldClump3D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		Rx = params.fget("R_x");
		Ry = params.fget("R_y");
		Rz = params.fget("R_z");
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
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
		Rx = params.fget("R_x");
		Ry = params.fget("R_y");
		Rz = params.fget("R_z");
	}
	
	public void initializeFieldWithSeed() {
  		for (int i = 0; i < Lp*Lp*Lp; i++) {
			double x = dx*(i%Lp - Lp/2);
			double y = dx*((i%(Lp*Lp))/Lp - Lp/2);
			double z = dx*((i/(Lp*Lp)) - Lp/2);
			double k = KR_SP/Rx; // TODO fixme
			// BCC (reciprocal lattice is FCC)
			double field = 0;
			field += 0.1*cos(k * ( x + z) / sqrt(3.));
			field += 0.1*cos(k * (-x + z) / sqrt(3.));
			field += 0.1*cos(k * ( y + z) / sqrt(3.));
			field += 0.1*cos(k * (-y + z) / sqrt(3.));
			phi[i] = DENSITY*(1+field);
		}
	}
	
	private void calculateFreeEnergyDensity() {
		freeEnergyDensity = 0;
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			freeEnergyDensity += 0.5*phi[i]*phi_bar[i]+T*phi[i]*log(phi[i]);
		}
		freeEnergyDensity /= Lp*Lp*Lp;
		freeEnergyDensity -= 0.5;
	}
	
	public void simulate() {
		convolveWithRange(phi, phi_bar);
		calculateFreeEnergyDensity();
		
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			del_phi[i] = - dt*(phi_bar[i]+T*log(phi[i])) + sqrt(dt*2*T/(dx*dx*dx))*noise();
		}
		double mu = DoubleArray.mean(del_phi)-(DENSITY-DoubleArray.mean(phi));
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			del_phi[i] -= mu;
			// clip del_phi to ensure phi(t+dt) > phi(t)/2
			del_phi[i] = max(del_phi[i], phi[i]/2 - phi[i]);
			phi[i] += del_phi[i];
		}
		
		convolveWithRange(phi, phi_bar);
		calculateFreeEnergyDensity();
		rms_dF_dphi = sqrt(DoubleArray.variance(del_phi));
		t += dt;
	}
	
	public double[] dFdensity_dR() {
		double[] ret = new double[3];
		
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			fftScratch[2*i] = phi[i];
			fftScratch[2*i+1] = 0;
		}
		fft.transform(fftScratch);
		
		for (int z = -Lp/2; z < Lp/2; z++) {
			for (int y = -Lp/2; y < Lp/2; y++) {
				for (int x = -Lp/2; x < Lp/2; x++) {
					if (x*y*z == 0)
						continue;
					int i = Lp*Lp*((z+Lp)%Lp) + Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double phi2 = sqr(fftScratch[2*i]) + sqr(fftScratch[2*i+1]);
					double kR = (2*PI/L)*sqrt(sqr(x*Rx)+sqr(y*Ry)+sqr(z*Rz));
					double temp = (2*PI/L)/sqrt(sqr(x*Rx)+sqr(y*Ry)+sqr(z*Rz));
					double dkR_dRx = temp * x*x*Rx;
					double dkR_dRy = temp * y*y*Ry;
					double dkR_dRz = temp * z*z*Rz;
					ret[0] += 0.5*phi2*dpotential_dkR(kR)*dkR_dRx;
					ret[1] += 0.5*phi2*dpotential_dkR(kR)*dkR_dRy;
					ret[2] += 0.5*phi2*dpotential_dkR(kR)*dkR_dRz;	
				}
			}
		}
		ret[0] /= (Lp*Lp*Lp);
		ret[1] /= (Lp*Lp*Lp);
		ret[2] /= (Lp*Lp*Lp);
		return ret;
	}	
	
	private void allocate() {
		phi = new double[Lp*Lp*Lp];
		phi_bar = new double[Lp*Lp*Lp];
		del_phi = new double[Lp*Lp*Lp];
		fftScratch = new double[2*Lp*Lp*Lp];
		fft = new ComplexDouble3DFFT(Lp, Lp, Lp);
	}
	
	private double noise() {
		return noiselessDynamics ? 0 : random.nextGaussian();
	}
	
	private void convolveWithRange(double[] src, double[] dest) {
		for (int i = 0; i < Lp*Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int z = -Lp/2; z < Lp/2; z++) {
			for (int y = -Lp/2; y < Lp/2; y++) {
				for (int x = -Lp/2; x < Lp/2; x++) {
					int i = Lp*Lp*((z+Lp)%Lp) + Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
					double kR = 2*PI*sqrt(sqr(x*Rx)+sqr(y*Ry)+sqr(z*Rz))/L;
					double J = potential(kR);
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
}