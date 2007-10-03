package kip.clump.radial;

import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.acos;
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;
import scikit.params.Parameters;

public class ClumpRadial {
	double DENSITY = 1;
	double dx, L, R, T, t, dt;
	int Lp;
	double[] phi, phibar, del_phi;
	double rms_dF_dphi, freeEnergyDensity;
	
	public ClumpRadial(Parameters params) {
		dx = params.fget("dx");
		L = params.fget("L");
		R = params.fget("R");
		T = params.fget("T");
		dt = params.fget("dt");
		
		Lp = (int)(L/dx);
		dx = L/Lp;
		params.set("dx", dx);
		
		phi = new double[Lp];
		phibar = new double[Lp];
		del_phi = new double[Lp];
		
		t = 0;
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
	}
	
	public void simulate() {
		convolveWithRange(phi, phibar, R);
		
		for (int ap = 0; ap < Lp; ap++) {
			del_phi[ap] = - dt*(phibar[ap]+T*log(phi[ap]));
		}
		double mu = rmean(del_phi)-(DENSITY-rmean(phi));
		for (int ap = 0; ap < Lp; ap++) {
			// clip del_phi to ensure phi(t+dt) > phi(t)/2
			del_phi[ap] = max(del_phi[ap]-mu, -phi[ap]/2.);
		}
		
		rms_dF_dphi = 0;
		freeEnergyDensity = 0;
		for (int ap = 0; ap < Lp; ap++) {
			rms_dF_dphi += (2*PI*ap) * sqr(del_phi[ap] / dt);
			freeEnergyDensity += (2*PI*ap) * (0.5*phi[ap]*phibar[ap]+T*phi[ap]*log(phi[ap]));
			phi[ap] += del_phi[ap];
		}
		rms_dF_dphi = sqrt(rms_dF_dphi/(PI*Lp*Lp));
		freeEnergyDensity = freeEnergyDensity/(PI*Lp*Lp) - 0.5;
		t += dt;
	}
	
	public double rmean(double[] src) {
		double acc = 0;
		for (int ap = 0; ap < Lp; ap++)
			 acc += 2*PI*ap*src[ap];
		return acc / (PI*Lp*Lp);
	}

	private void convolveWithRange(double[] src, double[] dst, double R) {
		int Rp = (int)(R/dx);
		for (int ap = 0; ap < Lp; ap++) {
			dst[ap] = 0;
			for (int bp = max(0, ap-Rp); bp < ap+Rp; bp++) {
				double a = ap*dx;
				double b = bp*dx;
				double cos_theta = max((a*a+b*b-R*R)/(2*a*b), -1);
				double theta = acos(cos_theta);
				dst[ap] += (2*R*theta*dx)*src[min(bp, Lp-1)];
			}
			dst[ap] /= PI*R*R;
		}
	}
}
