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
	public int Lp;
	public double dt, t;
	public double lastMu;
	public double[] phi;
	public double F_ave, lastFreeEnergy, dF_dt, freeEnergy;
	public int aveCount;
	double [] phi_bar, del_phi, Lambda;
	double horizontalSlice;
	double verticalSlice;
	ComplexDouble2DFFT fft;	// Object to perform transforms
	double[] fftScratch;
	public static final double KR_SP = 5.13562230184068255630140;
	public static final double T_SP = 0.132279487396100031736846;	
	
	Accumulator accFreeEnergy;
	Accumulator dF_dtAcc;
	Accumulator accMaxPhi;
	Accumulator accMinPhi;
	Accumulator accFEvT;
	
	boolean noiselessDynamics = false;
	boolean circleInteraction = true;
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
		double dT = params.fget("dT");
		DENSITY = params.fget("Magnetization");

		
		accFreeEnergy = new Accumulator(dt);
		accFreeEnergy.setAveraging(true);
		accFEvT = new Accumulator(dT);
		accFEvT.setAveraging(true);
		dF_dtAcc = new Accumulator(dt);
		dF_dtAcc.setAveraging(true);
		accMaxPhi = new Accumulator(dt);
		accMaxPhi.setAveraging(true);		
		accMinPhi = new Accumulator(dt);
		accMinPhi.setAveraging(true);
		
		
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
	
		theory = params.sget("Approx");
				
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		params.set("Lp", Lp);
		
		t = 0;

		phi = new double[Lp*Lp];
		phi_bar = new double[Lp*Lp];
		del_phi = new double[Lp*Lp];
		Lambda = new double [Lp*Lp];
		
		fftScratch = new double[2*Lp*Lp];
		fft = new ComplexDouble2DFFT(Lp, Lp);
		
		if(params.sget("Init Conditions") == "Random Gaussian")
			randomizeField(DENSITY);
		else if ("Init Conditions" == "Constant"){
			for (int i = 0; i < Lp*Lp; i ++)
				phi[i] = DENSITY;
		}else if(params.sget("Init Conditions") == "Read From File")
			readInitialConfiguration();
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
		//double potAccum = 0;
		//double entAccum = 0;
		
		convolveWithRange(phi, phi_bar, R);
		
		double meanLambda = 0;
		
		for (int i = 0; i < Lp*Lp; i++) {
			double dF_dPhi = 0, entropy = 0;
			if(theory == "Exact"){
				dF_dPhi = -phi_bar[i] -T*log(1.0-phi[i])/2.0+T*log(1.0+phi[i])/2.0 - H;
				Lambda[i] = 1;
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Exact Semi-Stable"){
				dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
				Lambda[i] = (1 - phi[i]*phi[i]);
				entropy = -((1.0 + phi[i])*log(1.0 + phi[i]) +(1.0 - phi[i])*log(1.0 - phi[i]))/2.0;
			}else if(theory == "Exact Stable"){
				dF_dPhi = -phi_bar[i]+T*(-log(1.0-phi[i])+log(1.0+phi[i]))/2.0 - H;
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
			meanLambda += Lambda[i];
			
			double potential = -(phi[i]*phi_bar[i])/2.0;
			//potAccum += potential;
			//entAccum -= T*entropy;

			freeEnergy += potential - T*entropy - H*phi[i];

		}
		
		meanLambda /= Lp*Lp;
		double mu = (mean(del_phi)-(DENSITY-mean(phi)))/meanLambda;
		for (int i = 0; i < Lp*Lp; i++) {
			freeEnergy +=  -mu*phi[i];
			phi[i] += del_phi[i]-Lambda[i]*mu;
		}
		freeEnergy /= (Lp*Lp) ;
		accFreeEnergy.accum(t,freeEnergy);
		dF_dtAcc.accum(t,dF_dt);
		dF_dt = (freeEnergy - lastFreeEnergy)/dt;
		lastFreeEnergy = freeEnergy;
		//System.out.println("dF_dt " + freeEnergy + " " + freeEnergy_i + " " + dF_dt);
		t += dt;
		
		accMinPhi.accum(t, kip.util.DoubleArray.min(phi));
		accMaxPhi.accum(t, kip.util.DoubleArray.max(phi));
		//System.out.println(kip.util.DoubleArray.max(phi) + " " + kip.util.DoubleArray.min(phi));
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

	public Accumulator getAccFEvT(){
		return accFEvT;
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
	
}
