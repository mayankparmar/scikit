package rachele.ising.dim1;

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
	//import static kip.util.DoubleArray.*;

	public class PathSample1D {
		public int Lp;
		public double dt, t;
		public int t_f;
		public double[][] phi, del_phi;
		double DENSITY;
		double [] phi_bar;
		public double L, R, T, J, dx, H, du;
		public double u = 0.0;
		
		public static final double KR_SP = 4.4934102;
		
		Random random = new Random();
		
		ComplexDoubleFFT fft;
		private double[] fftScratch;
		public double freeEnergyDensity;
		
		public PathSample1D(Parameters params) {
			random.setSeed(params.iget("Random seed", 0));
			
			//R = params.fget("R");
			//L = R*params.fget("L/R");
			//T = params.fget("T");
			//dx = R/params.fget("R/dx");
			//dt = params.fget("dt");
			//DENSITY = params.fget("Density");
			//Lp = Integer.highestOneBit((int)rint((L/dx)));
			//dx = L / Lp;
			//double RoverDx = R/dx;
			//params.set("R/dx", RoverDx);
			
			R = 2000;
			T = .86;
			J = 2.0;
			dt = 0.1;
			H = -0.07;
			L = 600000;
			dx = R/(16.0);
			Lp = Integer.highestOneBit((int)rint((L/dx)));
			dx = L / Lp;
			du = 0.01;
			
			t_f = 50;

			phi = new double[Lp][t_f+1];
			phi_bar = new double[Lp];
			del_phi = new double[Lp][t_f+1];
			
			fftScratch = new double[2*Lp];
			fft = new ComplexDoubleFFT_Mixed(Lp);
			
			double density_i = -0.6;
			double density_f = 0.6;
			double delta_density = (density_f - density_i)/t_f;
			
			
			for (int i = 0; i < Lp; i++){
				for (int j = 0; j <= t_f; j ++)
					phi [i][j] = j*delta_density+density_i;
			}
		}
		
		//public void readParams(Parameters params) {
		//	T = params.fget("T");
		//	J = params.fget("J");
		//	dt = params.fget("dt");
		//	R = params.fget("R");
		//	L = R*params.fget("L/R");
		//	dx = R/params.fget("R/dx");
		//	Lp = Integer.highestOneBit((int)rint((L/dx)));
		//	dx = L / Lp;
		//	params.set("R/dx", R/dx);
		//}
		
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
			double ret[] = new double[Lp*(t_f+1)];
			for (int j = 0; j <= t_f; j++){
				for (int i = 0; i < Lp; i++)
					ret[i+j*Lp] = phi[i][j];
			}
			return ret;
		}
		
		public void simulate() {
			double [] scratch;
			double [] lastScratch;
			double [] dPhi_dt;
			double atanhphi, d2Phi_dt2, df_dt;

			lastScratch = new double [Lp];
			scratch = new double [Lp];
			dPhi_dt = new double [Lp];
			for (int i = 0; i < Lp; i++)
				scratch[i] = phi[i][t_f];
			convolveWithRange(scratch, lastScratch, R);
			for (int i = 0; i < Lp; i++)
				lastScratch[i] -= atanh(phi[i][t_f])/T; 
				
			for(int j = t_f-1; j > 0; j--){
				for( int k = 0; k < Lp; k++)
					scratch[k] = phi [k][j];
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
			System.out.println(del_phi[Lp/2][t_f/2] + " " + phi[Lp/2][t_f/2]);
			u += du;
		}
	}


