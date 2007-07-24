package rachele.ising.dim1;

	import kip.util.Random;
	import scikit.numerics.fft.ComplexDoubleFFT;
	import scikit.numerics.fft.ComplexDoubleFFT_Mixed;
	import scikit.params.Parameters;
	import static java.lang.Math.PI;
	import static java.lang.Math.log;
	import static java.lang.Math.rint;
	import static java.lang.Math.sin;
	import static java.lang.Math.sqrt;
import static kip.util.DoubleArray.*;

	public class PathSample1D {
		public int Lp;
		public double dt, t;
		public int t_f;
		public double[][] phi;
		double DENSITY;
		double [] phi_bar, del_phi;
		public double L, R, T, J, dx, H;

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
			
			t_f = 100;

			phi = new double[Lp][t_f+1];
			phi_bar = new double[Lp];
			del_phi = new double[Lp];
			
			fftScratch = new double[2*Lp];
			fft = new ComplexDoubleFFT_Mixed(Lp);
			
			double density_i = -0.8;
			double density_f = 0.675;
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
			double [] phiAtT;
			phiAtT = new double [Lp];
			
			for(int j = 1; j < t_f; j++){
				for( int k = 0; k < Lp; k++)
					phiAtT[k] = phi [k][j];
				convolveWithRange(phiAtT,phi_bar, R);//convolve once for every time step
				for (int i = 0; i < Lp; i++){
						double t8 = -phi_bar[i]/((1-phi[i][j])*(1-phi[i][j])*T);
						atan();
				del_phi[]
				newphi[i][j] = ;
				}
			}
			//convolveWithRange(phi, phi_bar, R);

			//for (int i = 0; i < Lp; i++) {
			//	del_phi[i] = - dt*(-phi_bar[i]-T*log(1.0-phi[i])+T*log(1.0+phi[i])) + sqrt(dt*2*T/dx)*random.nextGaussian();
			//}
			////double mu = mean(del_phi)-(DENSITY-mean(phi));
			//for (int i = 0; i < Lp; i++) {
			//	phi[i] += del_phi[i];// - mu;
				
			//}
			//t += dt;
		}
	}


