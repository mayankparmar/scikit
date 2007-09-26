package kip.clump.dim3;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import kip.util.Random;

public class AbstractClump3D {
	public double L, R, T, dx;
	Random random = new Random();

	public static final double DENSITY = 1;
	public static final double KR_SP = 5.76345919689454979140645;
	public static final double T_SP = 0.08617089416190739793014991;

	
	public double potential(double kR) {
		return (kR == 0) ? 1 : (3/(kR*kR))*(sin(kR)/kR - cos(kR));
	}
	
	public double dpotential_dkR(double kR) {
		double kR2 = kR*kR;
		double kR3 = kR2*kR;
		double kR4 = kR2*kR2;
		return (kR == 0) ? 0 : (9*cos(kR)/kR3 + 3*(kR2-3)*sin(kR)/kR4);
	}
}
