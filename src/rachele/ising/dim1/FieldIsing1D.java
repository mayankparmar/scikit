package rachele.ising.dim1;

import kip.util.Random;
import scikit.params.Parameters;
import rachele.ising.dim1.AbstractIsing1D;
import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.DoubleArray.*;

public class FieldIsing1D extends AbstractIsing1D{
	int Lp;
	double dt, t;
	double[] phi,phi_bar, del_phi;
	
	public double L, R, T, dx;
	Random random = new Random();
	
	public static final double DENSITY = -.01;
	
	scikit.numerics.fft.RealDoubleFFT fft;
	private double[] fftScratch;
	public double freeEnergyDensity;
	
	public FieldIsing1D(Parameters params) {
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

		phi = new double[Lp];
		phi_bar = new double[Lp];
		del_phi = new double[Lp];
		
		fftScratch = new double[Lp];
		fft = new scikit.numerics.fft.RealDoubleFFT_Radix2(Lp);	
		
		for (int i = 0; i < Lp; i++)
			phi[i] = DENSITY;
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
	}
	
	public double time() {
		return t;
	}
	
	void convolveWithRange(double[] src, double[] dest, double R) {
		for (int i = 0; i < Lp; i++) {
			fftScratch[i] = src[i];
		}
		
		fft.transform(fftScratch);
		for (int x = -Lp/2; x < Lp/2; x++) {
			double kR = (2*PI*x/L) * R;
				int i = x + Lp/2;
				double J = (kR == 0 ? 1 : sin(kR)/kR);
				fftScratch[i] *= J;
		}
		fft.backtransform(fftScratch);
		
		for (int i = 0; i < Lp; i++) {
			dest[i] = fftScratch[i] / (Lp);
			//check this normalization
		}		
	}
	
	public double[] copyField() {
		double ret[] = new double[Lp];
		for (int i = 0; i < Lp; i++)
			ret[i] = phi[i];
		return ret;
	}
	
	public void simulate() {
		convolveWithRange(phi, phi_bar, R);

		//System.out.println("random = " + random.nextGaussian());
		
		for (int i = 0; i < Lp; i++) {
			//double phi2 = phi[i] * phi[i];
			//del_phi[i] = - dt*phi2*(phi_bar[i]+T*log(1.0-phi[i])-T*log(1.0+phi[i])) + sqrt(phi2*dt*2*T/(dx*dx))*random.nextGaussian();
			del_phi[i] = - dt*(phi_bar[i]+T*log(1.0-phi[i])-T*log(1.0+phi[i])) + sqrt(dt*2*T/(dx))*random.nextGaussian();
			//System.out.println(i + " phi = " + phi[i] + " dphi = " + del_phi[i] + " phi2 = " + phi2);
		}
		double mu = (mean(del_phi)-(DENSITY-mean(phi))) / meanSquared(phi);
		//System.out.println("mu = " + mu + " mean(dphi) = " + mean(del_phi) + " density = " + DENSITY + " mean(phi) = " + mean(phi) + " meanSq(phi) = " + meanSquared(phi) );
		for (int i = 0; i < Lp; i++) {
			del_phi[i] -= mu*phi[i]*phi[i];
			phi[i] += del_phi[i];
		}
		//System.out.println("ave phi deviation= " +  (mean(phi) - DENSITY));
		
		
		//rms_dF_dphi = 0;
		//freeEnergyDensity = 0;
		//for (int i = 0; i < Lp; i++) {
		//		rms_dF_dphi += sqr(del_phi[i] / (dt*phi[i]*phi[i]));
		//		freeEnergyDensity += phi[i]*phi_bar[i]+T*((1-phi[i])*log(1-phi[i])+(1+phi[i])*log(1+phi[i]));
		//		phi[i] += del_phi[i];
		//	}
		//rms_dF_dphi = sqrt(rms_dF_dphi/elementsInsideBoundary);
		t += dt;
		//for(int i = 0; i < Lp; i++){
		//	System.out.println("t = " + t + " " + i + " phi = " + phi[i] + " mu = " + mu);
		//}
	}
}
