package kip.quantum;

import kip.util.Complex;

public class IsingChain {
	int n = 3;
	int mask = (1 << n) - 1;
	Complex[] ground;
	
	public IsingChain() {
		ground = new Complex[1<<n];
		for (int i = 0; i < 1<<n; i++)
			ground[i] = new Complex(Math.random()-0.5, Math.random()-0.5);
	}
	
//	Complex trialHamiltonian(int i) {
//		
//	}
//	
//	double hamiltonianDistance() {
//		
//	}
	
	int rotateOne(int s) {
		s = s & mask;
		return ((s << 1) & mask) | (s >> (n-1));
	}
	
	int classicalEnergy(int s) {
		int cnt = Integer.bitCount(s ^ rotateOne(s));
		return 2*cnt - n;
	}
	
	int hamiltonian(int i, int j) {
		if (i == j)
			return classicalEnergy(i);
		else if (Integer.bitCount(i ^ j) == 1) 
			return 1;
		else
			return 0;
	}
	
	void printHamiltonian() {
		for (int i = 0; i < (1<<n); i++) {
			for (int j = 0; j < (1<<n); j++) {
				System.out.print(hamiltonian(i, j) + "    ");
			}
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		IsingChain chain = new IsingChain();
		chain.printHamiltonian();
	}
}
