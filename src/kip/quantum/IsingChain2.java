package kip.quantum;

import kip.util.Random;
import scikit.numerics.fn.C1Function;
import scikit.numerics.opt.Relaxation;
import scikit.util.DoubleArray;
import scikit.util.Pair;

public class IsingChain2 {
	int n = 8;
	int dim = n/2;
	double dt = 0.001;
	
	Random r = new Random(1);
	
	void run() {
		Relaxation opt = new Relaxation(dim, dt);
		opt.setFunction(energyFn);

		double[] a = new double[dim]; // coeff
		for (int i = 0; i < dim; i++)
			a[i] = r.nextDouble()-0.5;
		opt.initialize(a);
		
		while(true) {
			opt.setStepSize(dt);
			opt.step();
			System.out.println(energyFn.eval(a) + " " + a[0] + " " + a[1] + " " + a[2]);
		}

	}
	
	C1Function energyFn = new C1Function() {
		double e;
		double[] grad = new double[dim];
		
		double J = 1;
		double lambda = 0; //0.5;
		
		double[] a;
		double[] spin = new double[n];
		double weight; // a_ij s_i s_j
		
		double e_acc;
		double[] corr_acc = new double[dim];
		double[] e_corr_acc = new double[dim];
		
		
		public Pair<Double,double[]> calculate(final double[] a) {
			this.a = a;
			for (int i = 0; i < n; i++)
				spin[i] = -1;
			weight = calcWeight();
			
			e_acc = 0;
			DoubleArray.zero(corr_acc);
			DoubleArray.zero(e_corr_acc);
			
			int mcs;
			for (mcs = 0; mcs < 10000; mcs++) {
				for (int i = 0; i < n; i++) {
					trySpinFlip(r.nextInt(n));
				}
				accumulate();
			}
			
			e = e_acc/mcs; 
			for (int i = 0; i < dim; i++) {
				double corr = corr_acc[i] / mcs;
				double e_corr = e_corr_acc[i] / mcs;
				grad[i] = 2*(e_corr - e*corr);
			}
			return new Pair<Double,double[]>(e, grad);
		}
		
		double calcWeight() {
			double weight = 0;
			for (int i = 0; i < n; i++) {
				for (int d = 0; d < dim; d++) { 
					int ip = (i+d)%n;
					weight += a[d] * spin[i] * spin[ip];
				}
			}
			return weight;
		}
		
		double weightAfterFlip(int i) {
			double weightp = weight;
			for (int j = i-dim+1; j < i+dim; j++) {
				if (i != j) {
					int d = Math.abs(j - i);
					int jp = (j+n)%n;
					weightp -= 2 * a[d] * spin[i] * spin[jp];
				}
			}
			
//			spin[i] *= -1;
//			assert(Math.abs(weightp - calcWeight()) > 1e-12);
//			spin[i] *= -1;
			
			return weightp;
		}
		
		void trySpinFlip(int i) {
			double weightp = weightAfterFlip(i);
			
			double p1 = Math.exp(2*weight);
			double p2 = Math.exp(2*weightp);
			if (p2 > p1 || r.nextDouble() < p2/p1) {
				spin[i] *= -1;
				weight = weightp;
			}
		}
		
		double classicalEnergy() {
			double energy = 0;
			for (int i = 0; i < n; i++) {
				int ip = (i+1)%n;
				energy += -J*spin[i]*spin[ip];
			}
			return energy;
		}
		
		double classicalCorrelation(int d) {
			double corr = 0;
			for (int i = 0; i < n; i++) {
				int ip = (i+d)%n;
				corr += spin[i]*spin[ip];
			}
			return corr/n;
		}
		
		void accumulate() {
			double e = classicalEnergy();
			for (int i = 0; i < n; i++) {
				double f = Math.exp(weight);
				double fp = Math.exp(weightAfterFlip(i));
				e += lambda * fp / f;
			}
			
			e_acc += e;
			for (int d = 0; d < dim; d++) {
				double corr = classicalCorrelation(d);
				corr_acc[d] += corr;
				e_corr_acc[d] += e*corr;
			}
		}
	};

	
	public static void main(String[] args) {
		new IsingChain2().run();
	}
}
