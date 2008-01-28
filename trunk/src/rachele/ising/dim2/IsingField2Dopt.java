package rachele.ising.dim2;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.j1;
import static kip.util.MathPlus.sqr;
import kip.util.Random;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.ComplexDouble2DFFT;

public class IsingField2Dopt {
	public double L, R, T, dx, J, H, dT;
	public int Lp, N;
	public double DENSITY;
	public double dt, t;
	public double[] phi, phiVector;
	public double freeEnergy;
	double [] phi_bar, delPhi, Lambda, A;
	ComplexDouble2DFFT fft;
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	public double lambda;
	

	boolean noiselessDynamics = false;
	boolean circleInteraction = false;
	boolean magConservation = false;
	String theory;
	
	Random random = new Random();
	

	
	public IsingField2Dopt(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		H = params.fget("H");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		DENSITY = params.fget("Magnetization");
		if(params.sget("Interaction") == "Circle")
			circleInteraction = true;
		if(params.sget("Noise") == "Off"){
			noiselessDynamics = true;
		}else{
			noiselessDynamics = false;
		}
		if(params.sget("Dynamics?") == "Langevin Conserve M") magConservation = true;
		else if(params.sget("Dynamics?") == "Langevin No M Conservation") magConservation = false;
		theory = params.sget("Approx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		//params.set("Lp", Lp);
		N = Lp*Lp;
		t = 0;

		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		delPhi = new double[Lp*Lp];
		Lambda = new double [Lp*Lp];
		phiVector = new double[Lp*Lp];
//		A = new double [Lp*Lp];	
		fftScratch = new double[2*Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp, Lp);
		randomizeField(DENSITY);
	}
	
	public void randomizeField(double m) {
		for (int i = 0; i < Lp*Lp; i++)
			//phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
			phi[i] = random.nextGaussian()/(dx);
	}
	
	public void readParams(Parameters params) {
		//if (params.sget("Plot FEvT") == "Off") T = params.fget("T");
		dt = params.fget("dt");
		H = params.fget("H");
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		T = params.fget("T");
		//dT = params.fget("dT");
		
		params.set("R/dx", R/dx);
		//params.set("Lp", Lp);
		//params.set("Free Energy", freeEnergy);
		
		if(params.sget("Interaction") == "Circle"){
			circleInteraction = true;
		}else{
			circleInteraction = false;
		}

		if(params.sget("Noise") == "Off"){
			noiselessDynamics = true;
		}else{
			noiselessDynamics = false;
		}

		if(params.sget("Dynamics?") == "Langevin Conserve M")
			magConservation = true;
		else if(params.sget("Dynamics?") == "Langevin No M Conservation")
			magConservation = false;
		
		theory = params.sget("Approx");
	}
	
	
	void convolveWithRange(double[] src, double[] dest, double R) {
		double V;
		for (int i = 0; i < Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				if(circleInteraction == true){
					double kR = (2*PI*sqrt(x*x+y*y)/L) * R;
					V = (kR == 0 ? 1 : 2*j1(kR)/kR);
				}else{
					double k_xR = (2*PI*x/L)*R;
					double k_yR =(2*PI*y/L)*R;
					V = (k_xR == 0 ? 1 : sin(k_xR)/k_xR);
					V *= (k_yR == 0 ? 1 : sin(k_yR)/k_yR);
					
				}
				fftScratch[2*i] *= V*J;
				fftScratch[2*i+1] *= V*J;
			}
		}
		fft.backtransform(fftScratch);
		
		for (int i = 0; i < Lp*Lp; i++) {
			dest[i] = fftScratch[2*i] / (Lp*Lp);
		}		
	}
	
	public void simulate() {
		freeEnergy = 0;  //free energy is calculated for previous time step
		double potAccum = 0;
		double entAccum = 0;
		double del_phiSquared = 0;
		
		convolveWithRange(phi, phi_bar, R);
		
		double meanLambda = 0;
		
		for (int i = 0; i < Lp*Lp; i++) {
			double dF_dPhi = 0, entropy = 0;
			if(theory == "Exact"){
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* kip.util.MathPlus.atanh(phi[i]) - H;
				Lambda[i] = 1;//(1 - phi[i]*phi[i]);
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else{
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* kip.util.MathPlus.atanh(phi[i])- H;
				Lambda[i] = sqr(1 - phi[i]*phi[i]);				
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}
	
			delPhi[i] = - dt*Lambda[i]*dF_dPhi + sqrt(Lambda[i]*(dt*2*T)/dx)*noise();
			phiVector[i] = delPhi[i];
			meanLambda += Lambda[i];
			
			double potential = -(phi[i]*phi_bar[i])/2.0;
			potAccum += potential;
			entAccum -= T*entropy;

			freeEnergy += potential - T*entropy - H*phi[i];

		}		
		meanLambda /= Lp*Lp;
		double mu = (mean(delPhi)-(DENSITY-mean(phi)))/meanLambda;
		mu /= dt;
		if (magConservation == false)
			mu = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			freeEnergy +=  -mu*phi[i];
			phi[i] += delPhi[i]-Lambda[i]*mu*dt;
			del_phiSquared += phi[i]*phi[i];
		}
		t += dt;
	}
	
	
	public void useNoiselessDynamics() {
		noiselessDynamics = true;
	}	
	
	double noise() {
		return noiselessDynamics ? 0 : random.nextGaussian();
	}
	
	public double mean(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
				sum += a[i];
		return sum/(Lp*Lp); 
	}
		
	double meanSquared(double[] a) {
		double sum = 0;
		for (int i = 0; i < Lp*Lp; i++)
				sum += a[i]*a[i];
		return sum/(Lp*Lp);
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
	
	public boolean circleInt(){
		return circleInteraction;
	}
	
	
	
		
	public double isingFreeEnergyCalc(double [] config){
		convolveWithRange(config, phi_bar, R);
		freeEnergy = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			if(Math.abs(phi[i]) >= 1)
				System.out.println("boundary at point " + i);
			double entropy = -((1.0 + config[i])*log(1.0 + config[i]) +(1.0 - config[i])*log(1.0 - config[i]))/2.0;
			double potential = -(config[i]*phi_bar[i])/2.0;
			freeEnergy += potential - T*entropy - H*config[i];
		}
		freeEnergy /= (Lp*Lp);
		if (Double.isNaN(freeEnergy))
			return Double.POSITIVE_INFINITY;
		return freeEnergy;
	}
	public double isingFreeEnergyCalcA (double [] config){
		convolveWithRange(config, phi_bar, R);
		freeEnergy = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			if(Math.abs(phi[i]) >= 1)
				System.out.println("boundary at point " + i);
			double entropy = -((1.0 + config[i])*log(1.0 + config[i]) +(1.0 - config[i])*log(1.0 - config[i]))/2.0;
			double potential = -(config[i]*phi_bar[i])/2.0;
			//double boundaryTerm = exp(-sqr(1-config[i])/(2.0*sigma*sigma))/sqr(1-config[i]) + exp(-sqr(1+config[i])/(2.0*sigma*sigma))/sqr(1+config[i]);
			//System.out.println("Boundary term = " + boundaryTerm);
			freeEnergy += potential - T*entropy*100 - H*config[i];
		}
		freeEnergy /= (Lp*Lp);
		if (Double.isNaN(freeEnergy))
			return Double.POSITIVE_INFINITY;
		return freeEnergy;
	}

}
