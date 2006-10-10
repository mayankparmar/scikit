package kip.traffic;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class SpinodalApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    int L;
    int[] data;
    double beta, J;
    
	public static void main(String[] args) {
		frame(new Control(new SpinodalApp()), "Spinodal Model");
	}
    
    public SpinodalApp() {
        params.add("Size", 128, true);
        params.add("Temperature", 2.0, false);
        params.add("Interaction", 0.5, false);
    }
    
    public void animate() {
        beta = 1 / params.fget("Temperature");
        J = params.fget("Interaction");    
    }
    
    
    private int sumNeighbors(int i) {
        int N = L*L;
        int y = i/L;
        int up   = (i+L)%N;
        int down = (i-L+N)%N;
        int left = (i-1+L)%L + y*L;
        int rght = (i+1)%L + y*L;
        return data[up] + data[down] + data[left] + data[rght];
    }
    
    private double deltaEnergy(int i, int dn) {
        return dn*(dn + 2*data[i] - J*sumNeighbors(i));
    }
    
    private void monteCarloTrial() {
        int i = (int) (L*L * random());
        int dn = random() < 0.5 ? -1 : 1;
        
        if (data[i] + dn >= 0) {
            double dE = deltaEnergy(i, dn);
            if (random() < min(exp(-beta*dE), 1)) {
                data[i] += dn;
            }
        }
    }
    
    public void run() {
        L = params.iget("Size");

        data = new int[L*L];
        
        grid.setData(L, L, data, 0, 16);
        
        addDisplay(grid);
        
        while (true) {
            for (int i = 0; i < L*L; i++)
                data[i] = 0;
                
            yield();
            
            while (true) {
                for (int i = 0; i < L*L; i++) {
                    monteCarloTrial();
                }
                yield();            
            }
        }
    }
}
