package mina.wanglandau;

public class Thermodynamics {
   
   static double logZ(int N, double[] g, double beta) {
      // m = max {e^(g - beta E)}
      double m = 0;
      for (int E = -2*N; E <= 2*N; E++)
         m = Math.max(m, g[E+2*N] - beta*E);
      
      //     s     = Sum {e^(g - beta E)} * e^(-m)
      // =>  s     = Z * e^(-m)
      // =>  log s = log Z - m
      double s = 0;
      for (int E = -2*N; E <= 2*N; E++)
         s += Math.exp(g[E+2*N] - beta*E - m);
      return Math.log(s) + m;
   }
   
   
   static double heatCapacity(int N, double[] g, double beta) {
      double logZ = logZ(N, g, beta);
      
      double E_avg = 0;
      double E2_avg = 0;
      
      for (int E = -2*N; E <= 2*N; E++) {
         if (g[E+2*N] == 0) continue;
         
         E_avg  += E   * Math.exp(g[E+2*N] - beta*E - logZ);
         E2_avg += E*E * Math.exp(g[E+2*N] - beta*E - logZ);
      }
      
      return (E2_avg - E_avg*E_avg) * beta*beta;
   }
}
