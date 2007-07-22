package rachele.ising.dim2;

/* This is a slight modification of kip.clump.dim2.FiledClump2D.java  */

import static java.lang.Math.*;
import static kip.util.MathPlus.*;
import static kip.util.DoubleArray.*;
import kip.util.Random;
import scikit.numerics.fft.ComplexDouble2DFFT;
import scikit.params.Parameters;

public class IsingField2D {
	public double L, R, T, dx, J;
	public int Lp;
	double dt, t;
	public double[] phi;
	double [] phi_bar, del_phi;
	boolean[] onBoundary;
	int elementsInsideBoundary;
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	
	boolean unstableDynamics = false;
	boolean noiselessDynamics = false;
	public boolean rescaleClipped = false;
	public double rms_dF_dphi;
	public double freeEnergyDensity;
	
	Random random = new Random();
	
	//public static final double DENSITY = -0.5;
	double DENSITY;
	
	public IsingField2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		DENSITY = params.fget("Density");
		
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		
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
		//for (int i = 0; i < Lp*Lp; i++){
		//	int x = i % Lp;
		//	phi [i] = Math.cos(2*PI*x/(R/dx))*.1;
		//}
		//DENSITY = mean (phi);

		
	}
	
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("R/dx", R/dx);
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
				double V = (kR == 0 ? 1 : 2*j1(kR)/kR);
				fftScratch[2*i] *= V*J;
				fftScratch[2*i+1] *= V*J;
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
		
		for (int i = 0; i < Lp*Lp; i++) {
			del_phi[i] = - dt*(-phi_bar[i]-T*log(1.0-phi[i])+T*log(1.0+phi[i])) + sqrt(dt*2*T/dx)*random.nextGaussian();
		}
		double mu = mean(del_phi)-(DENSITY-mean(phi));
		for (int i = 0; i < Lp*Lp; i++) {
			phi[i] += del_phi[i] - mu;
			//System.out.println("phi " + i + " = " + phi[i] + " " + ising.Lp);
		}
		t += dt;
	}
	
	
	//public StructureFactor newStructureFactor(double binWidth) {
	//	// round binwidth down so that it divides KR_SP without remainder.
	//	binWidth = KR_SP / floor(KR_SP/binWidth);
	//	return new StructureFactor(Lp, L, R, binWidth);
	//}
	
	
	//public void accumulateIntoStructureFactor(StructureFactor sf) {
	//	sf.accumulate(phi);
	//}
	
	
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
