package kip.dyn1d;


public class Intervention {
	
/*	
	final int ORIGIN = 0;
	final double testDt = 0.5;
	
	protected double[] rotate(double[] a, int x) {
		int L = a.length;
		double[] r = new double[L];
		for (int i = 0; i < a.length; i++) {
			r[i] = a[(i-x+L)%L];
		}
		return r;
	}
	
	protected int displacement(int x, int y) {
		int L = ψ.length;
		int d = y - x;
		if (d < L/2)
			d += L;
		if (d > L/2)
			d -= L;
		return d;
	}
	
	
	// in growth mode if field crosses origin; assume field was in a negative metastable well
	public boolean inGrowthMode() {
		for (int i = 0; i < ψ.length; i++)
			if (ψ[i] >= ORIGIN)
				return true;
		return false;
	}
	
	
	private int findGrowthCenter() {
		assert(inGrowthMode());
		
		int loc = 0; // guaranteed less than i; can be negative
		int cnt = 0; // number of accumulations into loc
		int L = ψ.length;
		
		for (int i = 0; i < L; i++) {
			if (ψ[i] >= ORIGIN) {
				if (cnt == 0)
					loc = i;
				else
					loc += (i - loc/cnt < L/2) ? i : i - L;
				cnt++;
			}
		}
		return (loc/cnt + L) % L;
	}
	
	
	private int findGrowthRadius(int x) {
		// possibilities for improvement:
		// a) dynamically find width
		// b) feed in information about known solution
		return ψ.length / 16;
	}
	
	
	private double magnetization() {
		double acc = 0;
		for (int i = 0; i < ψ.length; i++)
			acc += ψ[i];
		return acc / ψ.length;
	}
	
	
	private double regionMagnetization(int x, int testRadius) {
		double acc = 0;
		int L = ψ.length;
		for (int i = x-testRadius; i <= x+testRadius; i++)
			acc += ψ[(i+L)%L];
		return acc / (2*testRadius+1);
	}
	
	
	// negative number means growth; nucleation occurred earlier
	// positive number means loss; nucleation occurred later
	public double testNucleationAtTime(double t, int x, int testRadius) {
		Dynamics1D sim = simulationAtTime(t);
		int L = ψ.length;
		
		double magReference = sim.regionMagnetization(x, testRadius);
		if (magReference < sim.magnetization()) {
			System.out.println(t + " unenhanced");
			return +1;  // region is not even enhanced!
		}
		
		final int TRIALS = 100;
		
		double[] avg = new double[L];
		double acc = 0;
		
		for (int i = 0; i < TRIALS; i++) {
			Dynamics1D c = sim.clone();
			c.random.setSeed((long)(Math.random()*(1<<48)));
			while (c.time() < sim.time()+testDt)
				c.step();
			acc += c.regionMagnetization(x, testRadius);
			
			for (int j = 0; j < L; j++)
				avg[j] += c.ψ[j] / TRIALS;
		}
		
		Job.plot(0, rotate(sim.ψ, L/2-x));
		Job.plot(1, rotate(avg, L/2-x));
		Job.plot(2, langerDroplet(L/2));
		System.out.println("t " + t + " delta: " + (magReference - acc/TRIALS));
		
		return magReference - acc/TRIALS;
	}
	
	
	public double intervention() {
		if (!inGrowthMode())
			throw new IllegalArgumentException();
		
		double lo = Math.max(time()-memoryTime/2, 0);
		double hi = time();
		 
		int x = findGrowthCenter();
		int testRadius = findGrowthRadius(x);
		
		System.out.println("growth center " + x);
		
		while (hi - lo > testDt / 10) {
			double t = (lo + hi) / 2;
			double delta = testNucleationAtTime(t, x, testRadius);
			Job.yield();
			
			if (delta < 0)
				hi = t;
			else
				lo = t;
		}
		return (lo + hi) / 2;
	}
*/

}