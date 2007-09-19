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
import scikit.numerics.fft.ComplexDouble2DFFT;
import scikit.params.Parameters;
//import java.io.*;

public class IsingField2D {
	public double L, R, T, dx, J, H;
	public int Lp, N;
	public double dt, t;
	public double lastMu;
	public double[] phi, del_phiSq;
	public double F_ave, lastFreeEnergy, dF_dt, freeEnergy;
	public int aveCount;
	double [] phi_bar, del_phi, Lambda;
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

	public double [] lineminPoint;
	public double [] lineminDirection;
	public double [] xi;
	public double [] g;
	public double [] h;
	
	public double f_p;
	public double fret;
	
	Accumulator accFreeEnergy;
	Accumulator dF_dtAcc;
	Accumulator accMaxPhi;
	Accumulator accMinPhi;
	Accumulator accFEvT;
	Accumulator accPotential;
	Accumulator accEntropy;
		
	boolean noiselessDynamics = false;
	boolean circleInteraction = true;
	boolean magConservation = false;
	String theory;
	
	Random random = new Random();
	
	double DENSITY;
	
	public IsingField2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));
		
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		H = params.fget("H");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		//double dT = params.fget("dT");
		DENSITY = params.fget("Magnetization");

		
		accFreeEnergy = new Accumulator(dt);
		accFreeEnergy.setAveraging(true);
		//accFEvT = new Accumulator(dT);
		//accFEvT.setAveraging(true);
		dF_dtAcc = new Accumulator(dt);
		dF_dtAcc.setAveraging(true);
		accMaxPhi = new Accumulator(dt);
		accMaxPhi.setAveraging(true);		
		accMinPhi = new Accumulator(dt);
		accMinPhi.setAveraging(true);
		accPotential = new Accumulator(dt);
		accPotential.setAveraging(true);
		accEntropy = new Accumulator(dt);
		accEntropy.setAveraging(true);
		accFEvT = new Accumulator(dt);
		accFEvT.setAveraging(true);
		
		horizontalSlice = params.fget("Horizontal Slice");
		verticalSlice = params.fget("Vertical Slice");
		
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
				
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		params.set("Lp", Lp);
		N = Lp*Lp;
		

		lineminPoint = new double [N];
		lineminDirection = new double [N];
		xi = new double [N];
		g = new double [N];
		h = new double [N];

		t = 0;

		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		del_phi = new double[Lp*Lp];
		Lambda = new double [Lp*Lp];
		del_phiSq = new double[Lp*Lp];
		
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
		T = params.fget("T");
		dt = params.fget("dt");
		H = params.fget("H");
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		
		params.set("R/dx", R/dx);
		params.set("Lp", Lp);
		params.set("dF_dt", dF_dt);
		params.set("Free Energy", freeEnergy);
		
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
		
		horizontalSlice = params.fget("Horizontal Slice");
		verticalSlice = params.fget("Vertical Slice");
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
				//dF_dPhi = -phi_bar[i] -T*log(1.0-phi[i])/2.0+T*log(1.0+phi[i])/2.0 - H;
				dF_dPhi = -phi_bar[i] +T* kip.util.MathPlus.atanh(phi[i])- H;
				Lambda[i] = 1;
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Exact Semi-Stable"){
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* kip.util.MathPlus.atanh(phi[i]) - H;
				Lambda[i] = (1 - phi[i]*phi[i]);
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Exact Stable"){
				//dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				dF_dPhi = -phi_bar[i]+T* kip.util.MathPlus.atanh(phi[i])- H;
				Lambda[i] = sqr(1 - phi[i]*phi[i]);				
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Phi4"){
				dF_dPhi = -phi_bar[i]+T*(-phi[i]-phi[i]*sqr(phi[i])/2.0) - H;
				Lambda[i] = sqr(1 - phi[i]*phi[i]);
				entropy = (sqr(phi[i]) + sqr(sqr(phi[i]))/4.0)/2.0;
			}else if(theory == "Linear"){
				dF_dPhi = -phi_bar[i] - T*phi[i] -H;
				Lambda[i] = sqr(1 - phi[i]*phi[i]);
				entropy = (sqr(phi[i]))/2.0;
			}
			del_phi[i] = - dt*Lambda[i]*dF_dPhi;// + sqrt(Lambda[i]*(dt*2*T)/dx)*noise();
			del_phiSq[i] = del_phi[i]*del_phi[i];
			meanLambda += Lambda[i];
			
			double potential = -(phi[i]*phi_bar[i])/2.0;
			potAccum += potential;
			entAccum -= T*entropy;

			freeEnergy += potential - T*entropy - H*phi[i];

		}
		
		meanLambda /= Lp*Lp;
		double mu = (mean(del_phi)-(DENSITY-mean(phi)))/meanLambda;
		mu /= dt;
		if (magConservation == false)
			mu = 0;
		for (int i = 0; i < Lp*Lp; i++) {
			freeEnergy +=  -mu*phi[i];
			phi[i] += del_phi[i]-Lambda[i]*mu*dt;
			del_phiSquared += phi[i]*phi[i];
		}
		
		freeEnergy /= (Lp*Lp) ;
		potAccum /= (Lp*Lp);
		entAccum /= (Lp*Lp);
		accFreeEnergy.accum(t,freeEnergy);
		accPotential.accum(t, potAccum);
		accEntropy.accum(t, entAccum);
		dF_dtAcc.accum(t,dF_dt);
		dF_dt = (freeEnergy - lastFreeEnergy)/dt;
		lastFreeEnergy = freeEnergy;
		//System.out.println("dF_dt " + freeEnergy + " " + freeEnergy_i + " " + dF_dt);
		t += dt;
		
		accMinPhi.accum(t, kip.util.DoubleArray.min(phi));
		accMaxPhi.accum(t, kip.util.DoubleArray.max(phi));
		//System.out.println(kip.util.DoubleArray.max(phi) + " " + kip.util.DoubleArray.min(phi));
	}
	
	public void accumFreeEnergy(){
		accFEvT.accum(T, freeEnergy);
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

	public Accumulator getFreeEnergyAcc() {
		return accFreeEnergy;
	}

	public Accumulator getMaxPhiAcc() {
		return accMaxPhi;
	}

	public Accumulator getMinPhiAcc() {
		return accMinPhi;
	}

	public Accumulator getDF_dtAcc(){
		return dF_dtAcc;
	}
	
	public Accumulator getAccPotential(){
		return accPotential;
	}

	public Accumulator getAccEntropy(){
		return accEntropy;
	}
	
	public Accumulator getAccFEvT(){
		return accFEvT;
	}
	
	public void initializeConjGrad(){
    	// Initializations:
    	// evaluate function and derivative at given point
    	// set all vectors (xi, g, h) equal to the direction
    	// of steepest decent at this point
		f_p = freeEnergyCalc(phi);
		xi = steepestAssentCalc(phi);
    	for(int j = 0; j < N; j ++){
    		g[j] = -xi[j];
    		h[j] = g[j];
    		xi[j] = h[j];
    	}		
	}
	
	public void getConjGradMin(){
    	
    	//for(int iteration = 1; iteration < maxIterations; iteration ++){
    		//System.out.println("iteration = " + iteration);
    		//conjIterations = iteration;
			fret = linemin(phi, xi); 
			//check to see if this move is OK
			//checkMove();
        	//if(checked == true){
        		//the following lone accepts the move:
        		for (int i = 0; i < N; i ++){
        			phi [i] = lineminPoint[i];
        			xi[i] = lineminDirection[i];
        			//drawablePoints[iteration*N + i] = point[i];
        		}
    		
        		// Check for doneness
        		if(2.0*Math.abs(fret - f_p) <= tolerance*(Math.abs(fret)+ Math.abs(f_p)+ EPS)){
        			//we are done -> return
        			System.out.println("Conj Grad Min finished at time " + t + " iterations");
        			return;
        		}else{
        			//accept the new value of the function value
        			f_p = fret;
        			//Construct the new direction h
        			double dgg = 0.0; //  numeration of gamma scalar = varies by method
        			double gg = 0.0; // denominator of gamma scalar = g_i dot g_i
        			for(int j = 0; j < N; j ++){
        				gg += g[j]*g[j];
        				//dgg += xi[j]*xi[j];			// This statement for Fletcher-Reeves
        				dgg += (xi[j] + g[j])*xi[j];	//This statement for Polak--Ribiere
        			}
        			if(gg == 0.0){
        				System.out.println("Conj Grad Min finished gg = 0 at time " + t + " iterations");
        				return;
        				//if gradient is exactly zero, then we are already done
        			}	
        			double gamma = dgg/gg;
        			for(int j = 0; j < N; j++){
        				g[j] = -xi[j];
        				h[j] = g[j] +gamma*h[j];
        				xi[j] = h[j];	//This is our new direction
        			}
        		}
        	//	switchD = 1;
    		//}else{
    		//	for (int i = 0; i < N; i++)
    		//		point[i] = minReferencePoint[i];
    		//	switchD = -1;
    		//}
    		
    	//}
        		for(int i = 0; i < N; i ++){
        			del_phiSq[i] = xi[i];
        		}
    t += dt;
    //System.out.println("Maximum iterations exceeded");
	}
	
    public double linemin(double point[], double direction[]){
    	for (int i = 0; i < N; i ++){	
    		lineminPoint[i] = point[i];	
    		lineminDirection[i] = direction[i];
    		//System.out.println( i + " " + lineminPoint[i] + " " + lineminDirection[i]);
    	}
    	double [] initBracket = new double [3]; 
     	// Make up two initial configurations and find an initial bracket
    	//be careful about input:  start at lambda_a = 0 amd lambda_b = positive so that it goes downhill 
    	initBracket = initialBracketMultiDim(0.0, 0.1);
    	//Find the min with golden
    	double minValue = goldenMinMultiDim(initBracket);
    	return minValue;
    }
	
    public double [] initialBracketMultiDim(double ax, double bx){
		double [] output = new double [3];
		
		//make sure bx is positive
		
		bx = Math.abs(bx);
		
		double f_b = f1dim(bx);
		double f_a = f1dim(ax);
		System.out.println("f_b = " + f_b + " f_a = " + f_a);
		double u, f_u;
		
		//Check to see if f(ax) has a higher value than f(bx)
		//If not, make bx smaller and smaller until it does
		
		while (f_b > f_a){
			bx = .5*bx;
			f_b = f1dim(bx);
//			if(bx < tolerance){
//				System.out.print("bx less than tolerence");
//			}
		}
		//System.out.println("bx = " + bx);
		
		//If not, switch the sign of bx and check
		
//		if (f_b > f_a){
//			bx = -bx;
//			f_b = f1dim(bx);
//		}
//		
//		//Check to see if f(ax) has a higher value than f(bx)
//		//If not, switch roles of ax and bx so that we can 
//		//go downhill in the direction from ax to bx
//		if (f_b > f_a){
//			double tempA = f_a;
//			f_a = f_b;
//			f_b = tempA;
//			tempA = ax;
//			ax = bx;
//			bx = tempA;
//		}
		
		//First guess for midpoint
		double cx =	bx + GOLD*(bx-ax);
		double f_c = f1dim(cx);
		
		//repeat the following until we bracket
		int iterations = 0;
		while(f_b > f_c){
			iterations ++;
			u = cx + GOLD*(cx-bx);
			f_u = f1dim(u);
			ax = bx;
			f_a = f_b;
			bx = cx;
			f_b = f_c;
			cx = u;
			f_c = f_u;
		}
		
		output[0] = ax;
		output[1] = bx;
		output[2] = cx;
		System.out.println("init bracket finished after " + iterations + " iterations");
		System.out.println("f_a = " + f_a + " f_b = " + f_b + " f_c = " + f_c);
		return output;
	}
	
	public double freeEnergyCalc(double [] config){
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			double entropy = -((1.0 + config[i])*log(1.0 + config[i]) +(1.0 - config[i])*log(1.0 - config[i]))/2.0;
			double potential = -(config[i]*phi_bar[i])/2.0;
			//System.out.println( i + " " + config[i] + " " + entropy + " " + potential);
			freeEnergy += potential - T*entropy - H*config[i];
		}
		
		return freeEnergy;
	}
	
	public double goldenMinMultiDim(double [] input){
		
		double x0, x1, x2, x3;
		double ax = input[0];
		double bx = input[1];
		double cx = input[2];
		x0 = ax;
		x3 = cx;
		
		//put the new test point in the longer segment
		//if bc is the longer segment, then x2 is the 
		//new test point and goes in between b and c
		
		if(Math.abs(cx - bx) > Math.abs(bx - ax)){
			x1 = bx;
			x2 = bx + GOLDC*(cx - bx);
		}else{
			x2 = bx;
			x1 = bx - GOLDC*(bx - ax);
		}
		
		double f_1 = f1dim(x1);
		double f_2 = f1dim(x2);
		
		int iteration = 0;
		while(Math.abs(x3-x0) > tolerance*(Math.abs(x1) + Math.abs(x2))  && x1 != x2 && x2 != x3){
			iteration ++;
//			System.out.println(x1 + " " + x2 + " " + x3);
//			System.out.println(Math.abs(x3-x0) + " " + tolerance*(Math.abs(x1) + Math.abs(x2)));
			
			if(f_2 < f_1){
				// choose new triplet as x1, x2, x3
				// shift variables x0, x1, x2 (new test point
				// which is chosen to go inside the larger segment
				x0 = x1;
				x1 = x2;
				x2 = GOLDR*x1 + GOLDC*x3; 
				// x2 goes a distance GOLDC*(x3-x1) inside the x3 x1 segment:
				// so x2 = x1 + GOLDC*(x3-x1)
				//       = x1*(1-GOLDC) + GOLDC*x3
				//       = x1*GOLDR + x3*GOLDC
				
				f_1 = f_2;
				f_2 = f1dim(x2);
				
			}else{
				x3 = x2;
				x2 = x1;
				x1 = GOLDR*x2 + GOLDC*x0;
				// x1 = x0 + GOLDC*(x2-x0)
				//    = x0*GOLDR + x2*GOLDC
				
				f_2 = f_1;
				f_1 = f1dim(x1);
			}
		}
		
		System.out.println("Golden Min finished after " + iteration + " iterations");
		
		double xmin;
		double minValue;
		if(f_1 < f_2){
			xmin = x1;
			minValue = f_1;
		}else{
			xmin = x2;
			minValue = f_2;
		}
		for(int i = 0; i < N; i ++){
			lineminPoint[i] = lineminPoint[i] + xmin*lineminDirection[i];
		}
		//System.out.println("golden min = " + minReferencePoint[0] + " " + minReferencePoint[1] + " lambda = " + xmin);
		//minReferenceDirection = delFreeEnergyCalc(minReferencePoint);
		lambda = xmin;
		return minValue;
	}
	
    public double f1dim(double lambda){
    	double newPoint [] = new double [N];
    	for(int i = 0; i < N; i++){
    		newPoint[i] = lineminPoint[i] + lambda*lineminDirection[i];
    	}
    	double ret = freeEnergyCalc(newPoint);
    	return ret;
    }

	public double [] steepestAssentCalc(double [] config){
		double steepestAssentDir [] = new double [N];
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			steepestAssentDir[i] = -phi_bar[i] +T* kip.util.MathPlus.atanh(phi[i])- H;
		}
		return steepestAssentDir;		
	}
	
	public void steepestDecent(){
		// Find direction of steepest decent
		del_phiSq = steepestAssentCalc(phi);
		for (int i = 0; i < N; i ++){
			del_phiSq[i] = -del_phiSq[i];
		}		
		fret = linemin(phi, del_phiSq);
		
		for (int i = 0; i < N; i ++){
			phi[i] = lineminPoint[i];
		}
		t += dt;
		accFreeEnergy.accum(t,fret);

	}
	
}
