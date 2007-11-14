package rachele.ising.dim2;

/* This is a slight modification of kip.clump.dim2.FieldClump2D.java  */

import static java.lang.Math.*;
import static kip.util.MathPlus.*;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import kip.util.Random;
import scikit.dataset.Accumulator;
import scikit.dataset.PointSet;
import scikit.jobs.params.Parameters;
import scikit.numerics.fft.ComplexDouble2DFFT;

public class IsingField2D {
	public double L, R, T, dx, J, H, dT;
	public int Lp, N;
	public double DENSITY;
	public double dt, t;
	public double lastMu;
	public double[] phi, phiVector;
	public double freeEnergy;
	public int aveCount;
	double [] phi_bar, delPhi, Lambda, A;
	public double horizontalSlice;
	public double verticalSlice;
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	
	
	//conjugate gradient parameters
	public int conjIterations;
	static double GOLD = 1.618034;
	static double GOLDR = 0.61803399;
	static double GOLDC = 1-GOLDR;
	static double maxIterations = 1000;
	static double tolerance = 1e-16;
	static double EPS = 1e-16;
	public boolean checked = false;
	public double lambda;
	static double delta = 1e-10;
	public double switchD = 1;
	static double ZEPS = 0.0000000001;  //small number that protects against
	//trying to achieve fractional accuracy for a minimum that happens to be
	// exactly zero
	static double sigma = .2;

	public double [] lineminDirection;
	public double [] xi;
	public double [] g;
	public double [] h;
	
	public double f_p;
	public double fret;
	
	Accumulator accClumpFreeEnergy;
	Accumulator accStripeFreeEnergy;
	Accumulator accEitherFreeEnergy;
	public Accumulator accFreeEnergy;
			
	boolean noiselessDynamics = false;
	boolean circleInteraction = false;
	boolean rectangleInteraction = false;
	boolean magConservation = false;
	int slowPower = 0;
	String theory;
	
	Random random = new Random();
	

	
	public IsingField2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		H = params.fget("H");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		dT = params.fget("dT");
		//double dT = params.fget("dT");
		DENSITY = params.fget("Magnetization");

		
		accStripeFreeEnergy = new Accumulator(0.0001);
		accStripeFreeEnergy.setAveraging(true);
		accClumpFreeEnergy = new Accumulator(0.0001);
		accClumpFreeEnergy.setAveraging(true);
		accEitherFreeEnergy = new Accumulator(0.0001);
		accEitherFreeEnergy.setAveraging(true);
		accFreeEnergy = new Accumulator(dt);
		accFreeEnergy.setAveraging(true);
		
		horizontalSlice = params.fget("Horizontal Slice");
		verticalSlice = params.fget("Vertical Slice");
		
		if(params.sget("Interaction") == "Circle")
			circleInteraction = true;
		else if(params.sget("Interaction") == "Rectangle")
			rectangleInteraction = true;

		if(params.sget("Noise") == "Off"){
			noiselessDynamics = true;
		}else{
			noiselessDynamics = false;
		}

		if(params.sget("Slow CG?") == "Yes, lots") slowPower = 4;
		else if(params.sget("Slow CG?") == "Yes, some") slowPower = 2;
		else if(params.sget("Slow CG?") == "No") slowPower = 0;
		
		if(params.sget("Dynamics?") == "Langevin Conserve M") magConservation = true;
		else if(params.sget("Dynamics?") == "Langevin No M Conservation") magConservation = false;
		
		
		theory = params.sget("Approx");
				
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		params.set("Lp", Lp);
		N = Lp*Lp;
		

		lineminDirection = new double [N];
		xi = new double [N];
		g = new double [N];
		h = new double [N];

		t = 0;

		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		delPhi = new double[Lp*Lp];
		Lambda = new double [Lp*Lp];
		phiVector = new double[Lp*Lp];
		A = new double [Lp*Lp];
		
		fftScratch = new double[2*Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp, Lp);
		
		String init = params.sget("Init Conditions");
		if(init == "Random Gaussian"){
			randomizeField(DENSITY);
			System.out.println("Random Gaussian");
		}else if (init == "Constant"){
			System.out.println("Constant");
			for (int i = 0; i < Lp*Lp; i ++)
				phi[i] = DENSITY;
		}else if(init == "Read From File"){
			readInitialConfiguration();
			System.out.println("Read From File");
		}else if(init == "Artificial Stripe 3"){
			System.out.println("Artificial Stripe 3");
			int stripeLoc = (int)(Lp/3.0);
			randomizeField(DENSITY);
			double m = DENSITY + (1.0-DENSITY)*.0001;
			for (int i = 0; i < Lp*Lp; i ++){
				int x = i%Lp;
				if (x == stripeLoc)
					phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
				if (x == stripeLoc*2)
					phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
				if (x == stripeLoc*3)
					phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
				if (phi[i] > 1.0){
					System.out.println(i + " " + phi[i]);
				}
			}
			
		}else
			System.out.println("no init conditions");
		for (int i = 0; i < Lp*Lp; i ++){
			A[i] = Math.log(phi[i]);
		}
		
	}
	
	public void readInitialConfiguration(){
		try{
			File myFile = new File("../../../research/javaData/configs/inputConfig");
			DataInputStream dis = new DataInputStream(new FileInputStream(myFile));
			int spaceIndex;
			double phiValue;
			try{
				while(true){
					spaceIndex =dis.readInt();
					dis.readChar();       // throws out the tab
					phiValue = dis.readDouble();
					dis.readChar();
					phi[spaceIndex] = phiValue;
				}
			} catch (EOFException e) {
			}

		} catch (FileNotFoundException e) {
			System.err.println("FileStreamsTest: " + e);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void randomizeField(double m) {
		for (int i = 0; i < Lp*Lp; i++)
			phi[i] = m + random.nextGaussian()*sqrt((1-m*m)/(dx*dx));
	}
	
	public void readParams(Parameters params) {
		if (params.sget("Plot FEvT") == "Off") T = params.fget("T");
		dt = params.fget("dt");
		H = params.fget("H");
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		dT = params.fget("dT");
		
		params.set("R/dx", R/dx);
		params.set("Lp", Lp);
		params.set("Free Energy", freeEnergy);
		
		if(params.sget("Interaction") == "Circle"){
			circleInteraction = true;
		}else{
			circleInteraction = false;
		}
		if(params.sget("Interaction") == "Rectangle")
			rectangleInteraction = true;
		else
			rectangleInteraction = false;

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
		
		horizontalSlice = params.fget("Horizontal Slice");
		verticalSlice = params.fget("Vertical Slice");
		
		if(params.sget("Slow CG?") == "Yes, lots") slowPower = 4;
		else if(params.sget("Slow CG?") == "Yes, some") slowPower = 2;
		else if(params.sget("Slow CG?") == "No") slowPower = 0;
	}
	
	public void initializeFieldWithSeed() {
		for (int i = 0; i < Lp*Lp; i++) {
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
		double Rx, Ry;
		if(rectangleInteraction == true){
			Rx = R;
			Ry = R*1.2;
		}else{
			Rx = Ry = R;
		}
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
					double k_xR = (2*PI*x/L)*Rx;
					double k_yR =(2*PI*y/L)*Ry;
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
			if(theory == "Exact" || theory == "Avoid Boundaries"){
				//dF_dPhi = -phi_bar[i] -T*log(1.0-phi[i])/2.0+T*log(1.0+phi[i])/2.0 - H;
				dF_dPhi = -phi_bar[i] +T* kip.util.MathPlus.atanh(phi[i])- H;
				Lambda[i] = 1;
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Exact Semi-Stable"){
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* kip.util.MathPlus.atanh(phi[i]) - H;
				Lambda[i] = (1 - phi[i]*phi[i]);
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Phi4"){
				dF_dPhi = -phi_bar[i]-T*(+2.0*phi[i]+pow(phi[i],3))/2.0 - H;
				//Lambda[i] = sqr(1 - phi[i]*phi[i]);
				Lambda[i] = 1;
				entropy = (sqr(phi[i]) + sqr(sqr(phi[i]))/4.0)/2.0;
			}else if(theory == "Linear"){
				dF_dPhi = -phi_bar[i] - T*phi[i] -H;
				Lambda[i] = sqr(1 - phi[i]*phi[i]);
				entropy = (sqr(phi[i]))/2.0;
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
		
		//freeEnergy = isingFreeEnergyCalc(phi);
		
		meanLambda /= Lp*Lp;
		double mu = (mean(delPhi)-(DENSITY-mean(phi)))/meanLambda;
		mu /= dt;
		if (magConservation == false)
			mu = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			freeEnergy +=  -mu*phi[i];
			if (theory == "Avoid Boundaries"){
				double tempPhi = phi[i] + delPhi[i]-Lambda[i]*mu*dt;
				if(Math.abs(tempPhi) > .99){
					//System.out.println("high");
					double halfMove = phi[i] + (Math.signum(phi[i]) - phi[i])/2.0;
					phi[i] = Math.signum(phi[i])*Math.min(Math.abs(tempPhi), Math.abs(halfMove));
				}else{
					phi[i] = tempPhi;
				}
			}else{
				phi[i] += delPhi[i]-Lambda[i]*mu*dt;
			}
			del_phiSquared += phi[i]*phi[i];
		}
		
		freeEnergy /= (Lp*Lp) ;
		//remove the following line
		//freeEnergy = isingFreeEnergyCalc(phi);
		potAccum /= (Lp*Lp);
		entAccum /= (Lp*Lp);
		accFreeEnergy.accum(t, freeEnergy);
		t += dt;
	}
	
	public void changeT(){
		T += dT;
		System.out.println("T = " + T + " " + dT);
	}
	
	public void accumClumpFreeEnergy(){
		accClumpFreeEnergy.accum(T, freeEnergy);
	}
	
	public void accumStripeFreeEnergy(){
		accStripeFreeEnergy.accum(T, freeEnergy);
	}

	public void accumEitherFreeEnergy(){
		accEitherFreeEnergy.accum(T, freeEnergy);
	}
	
	public void useNoiselessDynamics() {
		noiselessDynamics = true;
	}	
	
	public double phiVariance() {
		double var = 0;
		for (int i = 0; i < Lp*Lp; i++)
			var += sqr(phi[i]-DENSITY);
		return var / (Lp*Lp);
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
	
	public PointSet getHslice(){
		int y = (int) (horizontalSlice * Lp);
		double slice[] = new double[Lp];
		for (int x = 0; x < Lp; x++) {
			slice[x] = phi[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}

	public PointSet getVslice(){
		int x = (int) (verticalSlice * Lp);
		double slice[] = new double[Lp];
		for (int y = 0; y < Lp; y++) {
			slice[y] = phi[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}

	public PointSet get_delHslice(){
		int y = (int) (horizontalSlice * Lp);
		double slice[] = new double[Lp];
		for (int x = 0; x < Lp; x++) {
			slice[x] = delPhi[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}	

	public PointSet get_delVslice(){
		int x = (int) (verticalSlice * Lp);
		double slice[] = new double[Lp];
		for (int y = 0; y < Lp; y++) {
			slice[y] = delPhi[Lp*y + x];
		}
		return new PointSet(0, dx, slice);
	}	
	
	public Accumulator getFreeEnergyAcc() {
		return accFreeEnergy;
	}
	
	public Accumulator getStripeFreeEnergyAcc() {
		return accStripeFreeEnergy;
	}
	
	public Accumulator getClumpFreeEnergyAcc() {
		return accClumpFreeEnergy;
	}
	
	public Accumulator getEitherFreeEnergyAcc() {
		return accEitherFreeEnergy;
	}

	
	public void initializeConjGrad(){
    	// Initializations:
    	// evaluate function and derivative at given point
    	// set all vectors (xi, g, h) equal to the direction
    	// of steepest Descent at this point
		f_p = isingFreeEnergyCalc(phi);
		xi = steepestAscentCalc(phi);
    	for(int j = 0; j < N; j ++){
    		g[j] = -xi[j];
    		h[j] = g[j];
    		xi[j] = h[j];
    	}		
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
	public double [] steepestAscentCalc(double [] config){
		double steepestAscentDir [] = new double [N];
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			steepestAscentDir[i] = (-phi_bar[i] +T* kip.util.MathPlus.atanh(config[i])- H);
			steepestAscentDir[i] *= (pow(1 - phi[i]*phi[i], slowPower));
		}
		return steepestAscentDir;		
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

	public double [] steepestAscentCalcA(double [] config){
		double steepestAscentDir [] = new double [N];
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			double boundaryTerm =  exp(-sqr(1-config[i])/(2.0*sigma*sigma))*(1/sqr(sigma) + 2.0 / (sqr(1-config[i])))/(1-config[i]);
			//boundaryTerm -=        exp(-sqr(1+config[i])/(2.0*sigma*sigma))*(1/sqr(sigma) + 2.0 / (sqr(1+config[i])))/(1+config[i]);
			//System.out.println("Boundary term = " + boundaryTerm);
			steepestAscentDir[i] = (-phi_bar[i] +T*100* kip.util.MathPlus.atanh(config[i])- H + boundaryTerm);
			//steepestAscentDir[i] = (-phi_bar[i] +T* configA[i]- H + boundaryTerm);///(1-sqr(Math.tanh(configA[i])));//*(sqr(1 - phi[i]*phi[i]));
		}
		return steepestAscentDir;		
	}
}
