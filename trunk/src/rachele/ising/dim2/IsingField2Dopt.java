package rachele.ising.dim2;

//import static java.lang.Math.PI;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
//import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
//import static scikit.numerics.Math2.hypot;
import static scikit.numerics.Math2.j0;
import static scikit.numerics.Math2.j1;
import static scikit.numerics.Math2.jn;
import static scikit.numerics.Math2.sqr;
import kip.util.Random;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.ComplexDouble2DFFT;
//import scikit.numerics.fft.util.FFT2D;
import scikit.numerics.fn.Function2D;
import scikit.util.DoubleArray;

public class IsingField2Dopt {
	public double L, Rx, Ry, T, dx, J, H, dT;
	public int Lp, N;
	public double DENSITY, del_Rx, del_Ry;
	public double dt, t;
	public double[] phi, phiVector;
	public double freeEnergy, rangeParameter;
	double [] phi_bar, delPhi, Lambda, A;
	ComplexDouble2DFFT fft;
//	FFT2D fft;
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	public double lambda;
	
	boolean noiselessDynamics = false;
	boolean circleInteraction = false;
	boolean magConservation = false;
	//String theory;
	
	Random random = new Random();
	
	public IsingField2Dopt(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		J = params.fget("J");
		Rx = params.fget("Rx");
		Ry = params.fget("Ry");
		//L = Rx*params.fget("L/R");
		L = params.fget("L");
		T = params.fget("T");
		H = params.fget("H");
		//dx = Rx/params.fget("R/dx");
		dx = params.fget("dx");
		dt = params.fget("dt");
		rangeParameter = params.fget("range change");
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
		//theory = params.sget("Approx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("dx", dx);
		//double RoverDx = Rx/dx;
		//params.set("R/dx", RoverDx);
		//params.set("Lp", Lp);
		N = Lp*Lp;
		t = 0;

		allocate();
		initializeFieldWithHexSeed();
		//randomizeField(DENSITY);
	}
	
	public void initializeFieldWithHexSeed() {
 		for (int i = 0; i < Lp*Lp; i++) {
			double R = Rx;
			double x = dx*(i%Lp - Lp/2);
			double y = dx*(i/Lp - Lp/2);
			double field = 0;
			double k = KR_SP/R;
			field = 0;
			field += cos(k * (1*x + 0*y));
			field += cos(k * (0.5*x + 0.5*sqrt(3)*y));
			field += cos(k * (-0.5*x + 0.5*sqrt(3)*y));

			double r = sqrt(x*x+y*y);
			double mag = 0.5 / (1+sqr(r/R));
			phi[i] = DENSITY*(1+mag*field);
			System.out.println(phi[i]);
		}
 	//	shiftField();
	}
	
	public void randomizeField(double m) {
		for (int i = 0; i < Lp*Lp; i++)
			phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
			//phi[i] = random.nextGaussian()/(dx);
	}
	
	public void readParams(Parameters params) {
		dt = params.fget("dt");
		H = params.fget("H");
		T = params.fget("T");
		rangeParameter = params.fget("range change");
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
	}

	void convolve(double[] src, double[] dest, Function2D fn) {
		double V;
		for (int i = 0; i < Lp*Lp; i++) {
			fftScratch[2*i] = src[i];
			fftScratch[2*i+1] = 0;
		}
		
		fft.transform(fftScratch);
		for (int y = -Lp/2; y < Lp/2; y++) {
			for (int x = -Lp/2; x < Lp/2; x++) {
				int i = Lp*((y+Lp)%Lp) + (x+Lp)%Lp;
				double k_xR = (2*PI*x/L)*Rx;
				double k_yR =(2*PI*y/L)*Ry;
				V = fn.eval(k_xR, k_yR);
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
		String theory = "Exact";
		
//		convolveWithRange(phi, phi_bar, Rx);
		convolve(phi, phi_bar, new Function2D(){
			public double eval(double k1, double k2) {
				return potential(k1, k2);
			}
		});
		
		double meanLambda = 0;
		
		
		for (int i = 0; i < Lp*Lp; i++) {
			double dF_dPhi = 0, entropy = 0;
			if(theory == "Exact"){
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* scikit.numerics.Math2.atanh(phi[i]) - H;
				Lambda[i] = 1;//(1 - phi[i]*phi[i]);
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else{
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* scikit.numerics.Math2.atanh(phi[i])- H;
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
			delPhi[i] -= Lambda[i]*mu*dt;
			double testPhi = phi[i]+ delPhi[i];
			if(theory == "Exact"){
				if(abs(testPhi) >= 1){
					if(testPhi>=1)
						testPhi=phi[i]+(1-phi[i])/2.0;
					else if(testPhi<=-1)
						testPhi=phi[i]+(-1-phi[i])/2.0;
				}
			}	
			//phi[i] += delPhi[i]-Lambda[i]*mu*dt;
			//phi[i] += delPhi[i];
			phi[i]=testPhi;
			del_phiSquared += phi[i]*phi[i];
		}
		t += dt;
	}
	
	public void adjustRanges(){
		del_Rx=dt*rangeParameter*Rx*Rx*dFdensity_dRx();
		del_Ry=dt*rangeParameter*Ry*Ry*dFdensity_dRy();
		Rx -= del_Rx;
		Ry -= del_Ry;
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
	
	public double dFdensity_dRx() {
		double[] dphibar_dR = phi_bar;
		convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				double dPot_dRx;
				if (circleInteraction == true){
					double kR = sqrt(k1*k1*Rx*Rx + k2*k2*Ry*Ry);
					double dkR_dRx = k1 == 0 ? 0 : (k1*k1*Rx / kR);
					dPot_dRx = dpotential_dkR(kR)*dkR_dRx;
				}else{
					dPot_dRx = dsquarePotential_dR1(k1, k2, Rx, Ry);
				}
				dPot_dRx *= J;
				return dPot_dRx;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp);
	}
	
	public double dFdensity_dRy() {
		double[] dphibar_dR = phi_bar;
		convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				double dPot_dRy;
				if (circleInteraction == true){
					double kR = sqrt(k1*k1*Rx*Rx + k2*k2*Ry*Ry);
					double dkR_dRy = k2 == 0 ? 0 : (k2*k2*Ry / kR);
					dPot_dRy = dpotential_dkR(kR)*dkR_dRy;
				}else{
					dPot_dRy = dsquarePotential_dR1(k2, k1, Ry, Rx);
				}
				dPot_dRy *= J;
				return dPot_dRy;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp);
	}
	
	private void allocate(){
		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		delPhi = new double[Lp*Lp];
		Lambda = new double [Lp*Lp];
		phiVector = new double[Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp,Lp);
		fftScratch = new double[2*Lp*Lp];
	}
	
	private double potential(double kRx, double kRy){
		double V;
		if (circleInteraction == true){
			double kR = sqrt(kRx*kRx + kRy*kRy);
			V = (kR == 0 ? 1 : 2*j1(kR)/kR);
		}else{
			V = (kRx == 0 ? 1 : sin(kRx)/kRx);
			V *= (kRy == 0 ? 1 : sin(kRy)/kRy);
		}
		return V;
	}
	
	private double dsquarePotential_dR1(double k1, double k2, double R1, double R2){
		double potk2 = (k2 == 0) ? 1 : sin(k2*R2)/(k2*R2);
		return (k1 == 0 || k2 == 0) ? 0 : potk2*(cos(k1*R1)/R1 - sin(k1*R1)/(k1*R1*R1));
	}
	
	private double dpotential_dkR(double kR) {
		double kR2 = kR*kR;
		return (kR == 0) ? 0 : j0(kR)/kR - 2*j1(kR)/kR2  - jn(2,kR)/kR;
	}
}
