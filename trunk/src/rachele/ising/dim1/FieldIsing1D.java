package rachele.ising.dim1;

import kip.util.Random;
import scikit.numerics.fft.ComplexDoubleFFT;
import scikit.numerics.fft.ComplexDoubleFFT_Mixed;
import scikit.params.Parameters;
import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.DoubleArray.*;
import static kip.util.MathPlus.atanh;

//import java.util.*;

public class FieldIsing1D{
	public int Lp;
	public double dt, t;
	public double[] phi;
	double DENSITY;
	double [] phi_bar, del_phi;
	boolean modelA;
	
	public double L, R, T, J, dx, H;
	Random random = new Random();
	
	//public static final double DENSITY = -0;
	public static final double KR_SP = 4.4934102;
	
	ComplexDoubleFFT fft;
	private double[] fftScratch;
	public double freeEnergyDensity;
	
	public FieldIsing1D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		DENSITY = params.fget("Density");
		H = params.fget("H");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		params.set("Lp", Lp);
		if (params.sget("Zoom").equals("A"))
			modelA = true;
		else
			modelA = false;
		
		t = 0;

		phi = new double[Lp];
		phi_bar = new double[Lp];
		del_phi = new double[Lp];
		
		fftScratch = new double[2*Lp];
		fft = new ComplexDoubleFFT_Mixed(Lp);
		
		for (int i = 0; i < Lp; i++)
			phi[i] = DENSITY;
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		J = params.fget("J");
		dt = params.fget("dt");
		R = params.fget("R");
		H = params.fget("H");
		L = R*params.fget("L/R");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("R/dx", R/dx);
		if (params.sget("Zoom").equals("A"))
			modelA = true;
		else
			modelA = false;
		params.set("DENSITY", mean(phi));
	}
	
	public double time() {
		return t;
	}
	
	void convolveWithRange(double[] src, double[] dest, double R) {
		// write real and imaginary components into scratch
		for (int i = 0; i < Lp; i++) {
			fftScratch[2*i+0] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		// multiply real and imaginary components by the fourier transform
		// of the potential, V(k), a real quantity.  this corresponds to
		// a convolution in "x" space.
		fft.transform(fftScratch);
		for (int x = -Lp/2; x < Lp/2; x++) {
			double kR = (2*PI*x/L) * R;
			int i = (x + Lp) % Lp;
			double V = (kR == 0 ? 1 : sin(kR)/kR);
			fftScratch[2*i+0] *= J*V;
			fftScratch[2*i+1] *= J*V;
		}
		fft.backtransform(fftScratch);
		
		// after reverse fourier transformation, return the real result.  the
		// imaginary component will be zero.
		for (int i = 0; i < Lp; i++) {
			dest[i] = fftScratch[2*i+0] / Lp;
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

		if (modelA){
			for (int i = 0; i < Lp; i++) {
				//del_phi[i] = - dt*( phi_bar[i]-H-T*log(1.0-phi[i])/2.0+T*log(1.0+phi[i])/2.0) + sqrt(dt*2*T/dx)*random.nextGaussian();
				del_phi[i] = - dt*( phi_bar[i]-H + T*atanh(phi[i])) + sqrt(dt*2*T/dx)*random.nextGaussian();
			}
			//double mu = mean(del_phi)-(DENSITY-mean(phi));
			for (int i = 0; i < Lp; i++) {
				phi[i] += del_phi[i];	
			}		
		}else{
			for (int i = 0; i < Lp; i++) {
				del_phi[i] = - dt*( phi_bar[i]-H-T*log(1.0-phi[i])+T*log(1.0+phi[i])) + sqrt(dt*2*T/dx)*random.nextGaussian();
			}
			double mu = mean(del_phi)-(DENSITY-mean(phi));
			for (int i = 0; i < Lp; i++) {
				phi[i] += del_phi[i] - mu;	
			}			
		}
		t += dt;
	}
}