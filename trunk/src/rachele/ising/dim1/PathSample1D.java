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
	import scikit.dataset.Accumulator;
	//import static kip.util.DoubleArray.*;

	public class PathSample1D {
		public int Lp;
		public double dt, t;
		public int t_f;
		public double[][] phi;
		double [] phiBar, rightExp1, rightExp2, rightExp3, rightExpBar, parRightExp1, parRightExp2, parRightExp3;
		double DENSITY;
		public double L, R, T, J, dx, H, du;
		public double u = 0.0;
		boolean sampleNoise;
		Accumulator timeSliceAcc; 
		Accumulator spaceSliceAcc;
		
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
			phiBar = new double [Lp];
			rightExp1 = new double [Lp];
			rightExp2 = new double [Lp];
			rightExp3 = new double [Lp];
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
			
			timeSliceAcc = new Accumulator(1);
			spaceSliceAcc = new Accumulator(dt);
			double density_i = -0.4;
			double density_f = 0.68;
//			double delta_density = (density_f - density_i)/t_f;
			
			
			//read in initial conditions from file
			//must have proper dimensions of phi array
			//readInputPath();
					
			
			//uncomment these lines for  constant slope
			/*for (int i = 0; i < Lp; i++){
				for (int j = 0; j <= t_f; j ++)
					phi [j][i] = j*delta_density+density_i;
			}*/
		
			//uncomment these lines for flat, jump, flat
			for (int i = 0; i < Lp; i ++){
				for (int j = 0; j < t_f/2; j ++ )
					phi[j][i] = density_i;
				for (int j = t_f/2; j <= t_f; j ++)
					phi[j][i] = density_f;
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
		
		public Accumulator getTimeSlice(){
			timeSliceAcc.clear();
			for (int i = 0; i < Lp; i++){
				timeSliceAcc.accum(i, phi[t_f/2][i]);
				//System.out.println(i + " " + phi[i][t_f/2]);
			}
			return timeSliceAcc;
		}
		
		public Accumulator getSpaceSlice(){
			spaceSliceAcc.clear();
			for (int j = 0; j <= t_f; j++)
				spaceSliceAcc.accum(j,phi[j][Lp/2]);
			return spaceSliceAcc;
		}
		
		public double firstPhiDeriv(int time, int i){
			double ret;
			if (time == 0){
				ret =(phi[time+1][i] - phi[time][i])/dt;
				//System.out.println("t=0 " + i);
			}else if (time == t_f){
				ret = (phi[time][i] - phi[time-1][i])/(dt);
				//System.out.println("t=t_f " + i);
			}else{
				ret = (phi[time+1][i] - phi[time-1][i])/(2*dt);
			}
			return ret;
		}
		
		public void calcRightExp(int time, double [] rightExp, double [] parRightExp){
			convolveWithRange(phi[time], phiBar, R);
			for (int i = 0; i < Lp; i++){
				double dPhi_dt = firstPhiDeriv(time, i);
				rightExp[i] = dPhi_dt + phiBar[i] + atanh(phi[time][i])/T - H;	
				parRightExp[i] = phiBar[i] + atanh(phi[time][i])/T - H;	
			}
		}
			
		public void simulate() {
			boolean forwardInTime = true;
			if (forwardInTime){
			calcRightExp(0, rightExp2, parRightExp2);
			calcRightExp(1, rightExp3, parRightExp3);
			for (int j = 1; j < t_f; j++){
				rightExp1=rightExp2;
				parRightExp1 = parRightExp2;
				rightExp2 = rightExp3;
				parRightExp2 = parRightExp3;					
				calcRightExp(j+1, rightExp3, parRightExp3);
				convolveWithRange(rightExp2, rightExpBar, R);
				for (int i = 0; i < Lp; i++){
					double term1 = -(phi[j+1][i]-2*phi[j][i]+phi[j-1][i])/sqr(dt) - (parRightExp3[i] - parRightExp1[i]) / (2*dt);
					double term2 = rightExpBar[i];
					double term3 = rightExp2[i]/(T*(1-sqr(phi[j][i])));
					phi[j][i] += -du*(term1 + term2 + term3 );
					if(sampleNoise)
						phi[j][i] += sqrt(2*du/(T*dx*dt))*random.nextGaussian();
				}
			}	
			u += du;
			}else{
				calcRightExp(t_f, rightExp2, parRightExp2);
				calcRightExp(t_f-1, rightExp1, parRightExp1);
				for (int j = t_f-1; j > 0; j--){
					rightExp3=rightExp2;
					parRightExp3 = parRightExp2;
					rightExp2 = rightExp1;
					parRightExp2 = parRightExp1;					
					calcRightExp(j-1, rightExp1, parRightExp1);
					convolveWithRange(rightExp2, rightExpBar, R);
					for (int i = 0; i < Lp; i++){
						double term1 = -(phi[j+1][i]-2*phi[j][i]+phi[j-1][i])/sqr(dt) - (parRightExp3[i] - parRightExp1[i]) / (2*dt);
						double term2 = rightExpBar[i];
						double term3 = rightExp2[i]/(T*(1-sqr(phi[j][i])));
						phi[j][i] += -du*(term1 + term2 + term3 );
						if(sampleNoise)
							phi[j][i] += sqrt(2*du/(T*dx*dt))*random.nextGaussian();
					}
				}	
				u += du;				
			}
		}
	}
				
			/*	
			double term1 = 
			double [] scratch;
			double [] lastScratch;
			double atanhphi, d2Phi_dt2, df_dt;

			lastScratch = new double [Lp];
			scratch = new double [Lp];
			for (int i = 0; i < Lp; i++)
				scratch[i] = phi[i][t_f];
			convolveWithRange(scratch, lastScratch, R);
			for (int i = 0; i < Lp; i++)
				lastScratch[i] -= atanh(phi[i][t_f])/T; 
				
			for(int j = t_f-1; j > 0; j--){
				for( int i = 0; i < Lp; i++)
					scratch[i] = phi [i][j];
				convolveWithRange(scratch, phi_bar, R);
				for (int i = 0; i < Lp; i++){
					atanhphi = atanh(phi[i][j]);
					dPhi_dt[i] = (phi[i][j+1]-phi[i][j]);
					scratch[i] = dPhi_dt[i] + phi_bar[i] - atanhphi/T;
				}	
				convolveWithRange(scratch, phi_bar, R);
				for (int i = 0; i < Lp; i++){
					double phi2 = sqr(phi[i][j]);
					del_phi[i][j] = phi_bar[i]-scratch[i]/(T*T*(1-phi2));
					scratch[i] -= dPhi_dt[i];
					df_dt = scratch[i]-lastScratch[i];
					lastScratch[i] = scratch[i];
					d2Phi_dt2 = phi[i][j+1]-2*phi[i][j]+ phi[i][j-1];
					del_phi[i][j] -= d2Phi_dt2 + df_dt; 
					del_phi[i][j] *= -du;
					del_phi[i][j] += sqrt(2*du/(T*dx*dt))*random.nextGaussian();
					phi[i][j] += del_phi[i][j];
				}
			}
			//System.out.println(del_phi[Lp/2][t_f/2] + " " + phi[Lp/2][t_f/2]);
			u += du;*/
		//}
	//}


