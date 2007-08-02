package rachele.ising.dim1;

import java.io.*;
//import java.io.FileReader;
//import java.util.*;

import kip.util.Random;
import static kip.util.MathPlus.*;
import scikit.numerics.fft.ComplexDoubleFFT;
import scikit.numerics.fft.ComplexDoubleFFT_Mixed;
import scikit.params.Parameters;
import static java.lang.Math.PI;
//import static java.lang.Math.log;
import static java.lang.Math.rint;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import scikit.dataset.PointSet;
//import static kip.util.DoubleArray.*;

public class PathSample1D {
	public int Lp;
	public double dt, t;
	public int t_f;
	public double[][] phi, delPhi;
	double [] phiBar, rightExp, rightExpBar, parRightExp1, parRightExp2, parRightExp3;
	double DENSITY;
	public double L, R, T, J, dx, H, du;
	public double adjust1, adjust2;
	public double u = 0.0;
	boolean sampleNoise;

	public static final double KR_SP = 4.4934102;

	Random random = new Random();

	ComplexDoubleFFT fft;
	private double[] fftScratch;
	public double freeEnergyDensity;

	public PathSample1D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = R/params.fget("R/dx");
		dt = params.fget("dt");
		du = params.fget("du");
		DENSITY = params.fget("Density");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		double RoverDx = R/dx;
		params.set("R/dx", RoverDx);
		t_f = params.iget("Time Interval");
		phi = new double [t_f+1][Lp];
		delPhi = new double [t_f+1][Lp];
		phiBar = new double [Lp];
		rightExp = new double [Lp];
		parRightExp1 = new double [Lp];
		parRightExp2 = new double [Lp];
		parRightExp3 = new double [Lp];
		rightExpBar = new double [Lp];
		if (params.sget("Sampling Noise").equals("On"))
			sampleNoise = true;
		else
			sampleNoise = false;

		fftScratch = new double[2*Lp];
		fft = new ComplexDoubleFFT_Mixed(Lp);

		double density_i = params.fget("init denisty");
		double density_f = params.fget("fin density");
		double delta_density = (density_f - density_i)/t_f;


		//read in initial conditions from file
		//must have proper dimensions of phi array
		//readInputPath();

		if(params.sget("Init Step").equals("Off")){
		//uncomment these lines for  constant slope
		for (int i = 0; i < Lp; i++){
			for (int j = 0; j <= t_f; j ++)
				phi [j][i] = j*delta_density+density_i;
		}
		}else{
		//uncomment these lines for flat, jump, flat
		for (int i = 0; i < Lp; i ++){
		for (int j = 0; j < t_f/2; j ++ )
		phi[j][i] = density_i;
		for (int j = t_f/2; j <= t_f; j ++)
		phi[j][i] = density_f;
		}
		}


	}

	public void readInputPath(){
		try{
			File myFile = new File("inputPath.txt");
			FileInputStream fis = new FileInputStream(myFile);
			DataInputStream dis = new DataInputStream(fis);
			//FileReader fileReader = new FileReader(myFile);
			//BufferedReader reader = new BufferedReader (fileReader);
			//String line;
			//String [] splitLine;
			int timeIndex, spaceIndex;
			double phiValue;

			//while((line = reader.readLine()) != null){
			//	System.out.println(line);
			//}
			for(int i = 0; i < 10 ; i++){
				//if((line = reader. readLine()) != null){
				//splitLine = line.split(" ");
				timeIndex = dis.readInt();
				dis.readChar();       // throws out the tab
				spaceIndex =dis.readInt();
				dis.readChar();       // throws out the tab
				phiValue = dis.readDouble();

				System.out.println(timeIndex + " " + spaceIndex + " " + phiValue);
				//}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void readParams(Parameters params) {
		T = params.fget("T");
		J = params.fget("J");
		R = params.fget("R");
		L = R*params.fget("L/R");
		H = params.fget("H");
		dx = R/params.fget("R/dx");
		Lp = Integer.highestOneBit((int)rint((L/dx)));
		dx = L / Lp;
		params.set("R/dx", R/dx);
		du = params.fget("du");
		if (params.sget("Sampling Noise").equals("On"))
			sampleNoise = true;
		else
			sampleNoise = false;
		adjust1 = params.fget("adjust term1");
		adjust2 = params.fget("adjust term2");
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
		double ret[] = new double[Lp*(t_f+1)];
		for (int j = 0; j <= t_f; j++){
			for (int i = 0; i < Lp; i++)
				ret[i+j*Lp] = phi[j][i];
		}
		return ret;
	}

	public PointSet getSpaceSlice(){
		double slice[] = new double[t_f+1];
		for (int j = 0; j < t_f+1; j++) {
			slice[j] = phi[j][Lp/2];
		}
		return new PointSet(0, dt, slice);
	}

	public PointSet getTimeSlice(){
		double slice[] = new double[Lp];
		for (int i = 0; i < Lp; i++) {
			slice[i] = phi[t_f/2][i];
		}
		return new PointSet(0, dx, slice);
	}

	public double firstPhiDeriv(int time, int i){
		return (phi[time+1][i] - phi[time-1][i])/(2*dt);
	}

	public void calcRightExp(int time, double [] rightExp){
		convolveWithRange(phi[time], phiBar, R);
		for (int i = 0; i < Lp; i++){
			double dPhi_dt = firstPhiDeriv(time, i);
			rightExp[i] = dPhi_dt + phiBar[i];// + T*atanh(phi[time][i]) - H;	
		}
	}

	public void calcParRightExp(int time, double [] parRightExp){
		convolveWithRange(phi[time], phiBar, R);
		for (int i = 0; i < Lp; i++){
			parRightExp[i] = phiBar[i];// + T*atanh(phi[time][i]) - H;	
		}
	}

	public void simulate() {
		calcParRightExp(0, parRightExp2);
		calcParRightExp(1, parRightExp3);
		for (int j = 1; j < t_f; j++){
			double[] parTempArray = parRightExp1;
			parRightExp1 = parRightExp2;
			parRightExp2 = parRightExp3;
			parRightExp3 = parTempArray;

			calcParRightExp(j+1, parRightExp3);
			calcRightExp(j, rightExp);
			convolveWithRange(rightExp, rightExpBar, R);

			for (int i = 0; i < Lp; i++){
				double d2phi_dt2 = (phi[j+1][i]-2*phi[j][i]+phi[j-1][i])/sqr(dt);
				double dparRight_dt = (parRightExp3[i] - parRightExp1[i]) / (2*dt);
				double term1 = adjust1*(-d2phi_dt2 - dparRight_dt);
				double term2 = adjust2*rightExpBar[i];
				double term3 = 0;//rightExp[i]*T/(1-sqr(phi[j][i]));
				delPhi[j][i] = -du*(term1 + term2 + term3 );
				if(sampleNoise)
					delPhi[j][i] += sqrt(2*du*T/(dx*dt))*random.nextGaussian();
			}
		}	
		for(int j = 1; j < t_f; j++){
			for(int i = 0; i < Lp; i++){
				phi[j][i] += delPhi[j][i];
			}
		}
		u += du;
	}
}
