package kip.clump;

import kip.util.Random;
import scikit.params.Parameters;

abstract public class AbstractClump2D {
	public double L, R, T, dx;
	Random random = new Random();
	
	public static final double DENSITY = 1;
	
	// value of kR which minimizes j1(kR)/kR
	public static final double KR_SP = 5.13562230184068255630140;
	// S(k) ~ 1 / (V(kR_sp)/T+1)
	// => T_SP = - V(kR_sp) = - 2 j1(kR_sp) / kR_sp 
	public static final double T_SP = 0.132279487396100031736846;	
	
	abstract public void readParams(Parameters params);
	abstract public StructureFactor newStructureFactor(double binWidth);
	abstract public void accumulateIntoStructureFactor(StructureFactor sf);
	abstract public void simulate();
	abstract public double[] coarseGrained();
	abstract public int numColumns();
	abstract public double time();
}
