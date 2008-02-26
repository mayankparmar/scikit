package rachele.ising.dim2;

import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static scikit.numerics.Math2.hypot;
import static scikit.numerics.Math2.j0;
import static scikit.numerics.Math2.j1;
import static scikit.numerics.Math2.jn;
import static scikit.numerics.Math2.sqr;
import kip.util.Random;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.ComplexDouble2DFFT;
import scikit.numerics.fft.util.FFT2D;
import scikit.numerics.fn.Function2D;
import scikit.util.DoubleArray;

public class IsingField2Dopt {
	public double L, Rx, Ry, T, dx, J, H, dT;
	public int Lp, N;
	public double DENSITY, freeEnergy;
	public double dt, t;
	public double[] phi, phiVector;
	public double rangeParameter, noiseParameter;
	public double potAccum = 0;
	public double entAccum = 0;
	double [] phi_bar, delPhi, Lambda, A;
	ComplexDouble2DFFT fft;
	FFT2D fft2;
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	public double lambda;
	
	boolean circleInteraction = false;
	boolean magConservation = false;
	public boolean recordTvsFE = false;
	public boolean recordHvsFE = false;
	double lastFreeEnergy = 0.0;
	String theory;
	double slopeTolerance = 0.000000001;
	
	Random random = new Random();
	
	public IsingField2Dopt(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		J=-1;
		Rx = params.fget("Rx");
		Ry = params.fget("Ry");
		L = params.fget("L");
		T = params.fget("T");
		H = params.fget("H");
		dx = params.fget("dx");
		dt = 1.0;
		rangeParameter = params.fget("range change");
		noiseParameter = params.fget("Noise");
		DENSITY = params.fget("Magnetization");
		if(params.sget("Interaction") == "Circle") circleInteraction = true;
		if(params.sget("Dynamics?") == "Langevin Conserve M") magConservation = true;
		else if(params.sget("Dynamics?") == "Langevin No M Conservation") magConservation = false;
		theory = params.sget("Theory");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("dx", dx);
		N = Lp*Lp;
		t = 0;
		allocate();
		//initializeFieldWithHexSeed();
		randomizeField(DENSITY);
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
		H = params.fget("H");
		T = params.fget("T");
		rangeParameter = params.fget("range change");
		noiseParameter = params.fget("Noise");
		if(params.sget("Interaction") == "Circle") circleInteraction = true;
		else circleInteraction = false;
		theory = params.sget("Theory");
		if(params.sget("Dynamics?") == "Langevin Conserve M")magConservation = true;
		else if(params.sget("Dynamics?") == "Langevin No M Conservation")magConservation = false;
	}

	public void simulate() {
		freeEnergy = 0;  //free energy is calculated for previous time step
		potAccum = 0;
		entAccum = 0;
		double del_phiSquared = 0;
		fft2.convolve(phi, phi_bar, new Function2D(){
			public double eval(double k1, double k2) {
				return potential(k1, k2);
			}
		});
		
		double meanLambda = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			double dF_dPhi = 0;
			dF_dPhi = -J*phi_bar[i]+T* scikit.numerics.Math2.atanh(phi[i]) - H;
			if(theory == "Slow Near Edge"){			
				Lambda[i] = sqr(1 - phi[i]*phi[i]);	
			}else{
				Lambda[i] = 1;				
			}

				delPhi[i] = - dt*Lambda[i]*dF_dPhi + sqrt(Lambda[i]*(dt*2*T)/dx)*noise();
				phiVector[i] = delPhi[i];
				meanLambda += Lambda[i];
				double entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;	
				double potential = -J*(phi[i]*phi_bar[i])/2.0;
				potAccum += potential;
				entAccum -= T*entropy;
				freeEnergy += potential - T*entropy - H*phi[i];
			}
		meanLambda /= Lp*Lp;
		double mu = (mean(delPhi)-(DENSITY-mean(phi)))/meanLambda;
		mu /= dt;
		if (magConservation == false) mu = 0;
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

		dt = mean(Lambda);
		double slope = (freeEnergy - lastFreeEnergy)/dt;
		if (abs(slope) < slopeTolerance) recordHvsFE = true;//recordTvsFE = true;
		t += dt;	
		lastFreeEnergy = freeEnergy;
		
	}
	
	public void adjustRanges(){
		Rx -= dt*rangeParameter*Rx*Rx*dFdensity_dRx();
		Ry -= dt*rangeParameter*Ry*Ry*dFdensity_dRy();
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
	
	double noise() {
		return noiseParameter*random.nextGaussian();
	}
	
	public double dFdensity_dRx() {
		double[] dphibar_dR = phi_bar;
		fft2.convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				double dPot_dRx;
				if (circleInteraction == true){
					double kR = hypot(k1*Rx, k2*Ry);
					double dkR_dRx = k1 == 0 ? 0 : (k1*k1*Rx / kR);
					dPot_dRx = dpotential_dkR(kR)*dkR_dRx;
				}else{
					dPot_dRx = dsquarePotential_dR1(k1, k2, Rx, Ry);
				}
				return dPot_dRx;
			}
		});
		return DoubleArray.dot(phi, dphibar_dR) / (2*Lp*Lp);
	}
	
	public double dFdensity_dRy() {
		double[] dphibar_dR = phi_bar;
		fft2.convolve(phi, phi_bar, new Function2D() {
			public double eval(double k1, double k2) {
				double dPot_dRy;
				if (circleInteraction == true){
					double kR = hypot(k1*Rx, k2*Ry);
					double dkR_dRy = k2 == 0 ? 0 : (k2*k2*Ry / kR);
					dPot_dRy = dpotential_dkR(kR)*dkR_dRy;
				}else{
					dPot_dRy = dsquarePotential_dR1(k2, k1, Ry, Rx);
				}
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
		fft2 = new FFT2D(Lp, Lp);
		fft2.setLengths(L, L);
	}
	
	private double potential(double kx, double ky){
		double V;
		double kRx = kx *Rx;
		double kRy = ky *Ry;
		if (circleInteraction == true){
			double kR = hypot(kRx, kRy);
			V = (kR == 0 ? 1 : 2*j1(kR)/kR);
		}else{
			V = (kRx == 0 ? 1 : sin(kRx)/kRx);
			V *= (kRy == 0 ? 1 : sin(kRy)/kRy);
		}
		return V;
	}
	
	private double dsquarePotential_dR1(double k1, double k2, double R1, double R2){
		double potk2 = (k2 == 0) ? 1 : sin(k2*R2)/(k2*R2);
		return (k1 == 0) ? 0 : potk2*(cos(k1*R1)/R1 - sin(k1*R1)/(k1*R1*R1));
	}
	
	private double dpotential_dkR(double kR) {
		double kR2 = kR*kR;
		return (kR == 0) ? 0 : j0(kR)/kR - 2*j1(kR)/kR2  - jn(2,kR)/kR;
	}
}