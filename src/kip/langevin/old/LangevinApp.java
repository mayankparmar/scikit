package kip.langevin.old;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
import org.opensourcephysics.numerics.*;

public class LangevinApp extends AbstractSimulation {
   Scalar2DFrame field = new Scalar2DFrame("Langevin");
   double[] psi, newpsi;
   int L, N;
   double dx, R2, h, epsilon, t, dt;
   double M = 1;
   double alpha, kernel;
   
   public void initialize() {
      t = 0;
      dx = control.getInt("dx");
	  L = control.getInt("L");
      kernel = 0;
      
      field.setZRange(false, -1, 1);
	  field.resizeGrid(L, L);
      N = L*L;
	  psi = new double[N];
      newpsi = new double[N];
	  for (int i = 0; i < N; i++) {
		 psi[i] = 0;
	  }
   }
   
   // box-muller algorithm for finding random number with gaussian
   // distribution.  variations exist.
   double gaussian() {
      double x1, x2, w, y1, y2;
 
      do {
         x1 = 2.0 * Math.random() - 1.0;
         x2 = 2.0 * Math.random() - 1.0;
         w = x1 * x1 + x2 * x2;
      } while (w >= 1.0);

      w = Math.sqrt((-2.0 * Math.log(w)) / w);
      y1 = x1 * w;
      y2 = x2 * w;
      return y1;
   }
   
   double ψ(int x, int y) {
      return psi[y*L+x];
   }

   //                                           ⌠t'   -α(t-t')
   // ∂ψ/∂t = -M(-R²∇²ψ + 2εψ + 4ψ³ - h) + η + ⎮   e       ψ(t') dt'
   //                                           ⌡t=0
   public void doStep() {
      for (int x = 0; x < L; x++) {
         int xp = (x+1) % L;
         int xm = (x-1+L) % L;
         for (int y = 0; y < L; y++) {
            int yp = (y+1) % L;
            int ym = (y-1+1) % L;
            double ψ3 = ψ(x,y) * ψ(x,y) * ψ(x,y);
            double laplace = (ψ(xp,y)+ψ(xm,y)+ψ(x,yp)+ψ(x,ym)-4*ψ(x,y))/(dx*dx);
            double dpsi_dt = -M * (-R2*laplace + 2*epsilon*ψ(x,y) + 4*ψ3 - h);
            double dt_eta = Math.sqrt(dt) * gaussian();
            kernel = Math.exp(-alpha*dt) * (kernel + dt*ψ(x,y));
            newpsi[y*L+x] = ψ(x, y) + dt*dpsi_dt + dt_eta + dt*kernel;
         }
      }
      for (int i = 0; i < N; i++)
         psi[i] = newpsi[i];
      t += dt;
      field.setAll(psi);
//      field.setMessage("t = " + t);
   }
   
   public void startRunning() {
	  R2 = control.getDouble("R\u00b2");
	  h = control.getDouble("h");
	  epsilon = control.getDouble("\u03b5");
      alpha = control.getDouble("alpha");
	  dt = control.getDouble("dt");
   }
   
   public void reset() {
      control.setValue("L", 32);
      control.setValue("dx", 1);
	  control.setAdjustableValue("R\u00b2", 8);
	  control.setAdjustableValue("\u03b5", -0.6);
	  control.setAdjustableValue("h", 0.2);
      control.setAdjustableValue("alpha", 0.1);
      control.setAdjustableValue("dt", 0.002);
      enableStepsPerDisplay(true);
      setStepsPerDisplay(10);
   }
   
   public static void main(String[] args) {
     SimulationControl.createApp(new LangevinApp());
   }
}
