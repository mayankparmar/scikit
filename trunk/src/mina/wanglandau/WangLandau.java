package mina.wanglandau;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import java.awt.*;
import java.text.NumberFormat;
import java.awt.Color;


public class WangLandau extends AbstractAnimation {
   PlottingPanel histogramPanel, densityPanel, heatPanel;
   Dataset histogramData, densityData, heatData;
   
   int mcs;
   int L, N;
   double density;	 // percentage of (spin 0) magnetic impurities
   double[] g;       // logarithm of the density of states (energy argument translated by 2N)
   int[] H;          // histogram (reduce f when it is "flat")
   int E;            // energy of current spin configuration (translated by 2N)
   int[] spin;
   double f;         // multiplicative modification factor to g
   int iterations;   // number of reductions to f
   
   
   int sumNeighbors(int i) {
      int u = i - L;
      int d = i + L;
      int l = i - 1;
      int r = i + 1;
      
      if (u < 0)        u += N;
      if (d >= N)       d -= N;
      if (i % L == 0)   l += L;
      if (r % L == 0)   r -= L;
      return spin[u] + spin[d] + spin[l] + spin[r];
   }
   

   void initialize() {
      mcs = 0;
      N = L*L;
      f = Math.exp(1);
      iterations = 0;
      
      spin = new int[N];
      for (int i = 0; i < N; i++) {
         spin[i] = Math.random() < 0.5 ? 1 : -1;
		 if (Math.random() < density)
			spin[i] = 0;
      }
	  
      g = new double[4*N + 1];
      H = new int   [4*N + 1];
      for (int e = 0; e <= 4*N; e++) {
         g[e] = 0;
         H[e] = 0;
      }
      
      E = 0;
      for (int i = 0; i < N; i++) {
         E += - spin[i] * sumNeighbors(i);
      }
      E /= 2;        // we double counted all interacting pairs
      E += 2*N;      // translate energy by 2*N to facilitate array access
   }
   
   
   void flipSpins() {
      for (int steps = 0; steps < N; steps++) {
         int i = (int) (N * Math.random());
         int dE = 2*spin[i]*sumNeighbors(i);
         
         if (Math.random() < Math.exp(g[E] - g[E + dE])) {
            spin[i] = -spin[i];
            E += dE;
         }
         
         g[E] += Math.log(f);
         H[E] += 1;
      }
   }
   
   
   boolean isFlat() {
      int netH = 0;
      double numEnergies = 0;
      
      for (int e = 0; e <= 4*N; e++) {
         if (H[e] > 0) {
            netH += H[e];
            numEnergies++;
         }
      }
      
      for (int e = 0; e <= 4*N; e++)
         if (0 < H[e] && H[e] < 0.8*netH/numEnergies)
            return false;
      
      return true;
   }
   
   
   public void doStep() {
      int mcsMax = mcs + Math.max(100000/N, 1);
      for (; mcs < mcsMax; mcs++)
         flipSpins();
      control.println("mcs = " + mcs);
      control.println("iteration = " + iterations);
      
      if (isFlat()) {
//         NumberFormat nf = NumberFormat.getInstance();
//         nf.setMaximumFractionDigits(3);
         f = Math.sqrt(f);
         iterations++;
         
         for (int e = 0; e <= 4*N; e++)
            H[e] = 0;
      }
      
      densityData.clear();
      histogramData.clear();
      heatData.clear();
      
      for (int e = 0; e <= 4*N; e++) {
         if (g[e] > 0) {
            densityData.append   (e - 2*N, g[e] - g[0]);
            histogramData.append (e - 2*N, H[e]);
         }
      }
      
      for (double T = 0.5; T < 6; T += 0.1)
         heatData.append(T, Thermodynamics.heatCapacity(N, g, 1/T));
      for (double T = 1.9; T < 2.7; T += 0.02)
         heatData.append(T, Thermodynamics.heatCapacity(N, g, 1/T));
      
      densityPanel.repaint();
      histogramPanel.repaint();
      heatPanel.repaint();
   }
   
   
   public void initializeAnimation() {
      L = control.getInt("L");
	  density = control.getDouble("Impurity density");
      initialize();
      
      densityData.clear();
      histogramData.clear();
      heatData.clear();
      densityPanel.repaint();
      histogramPanel.repaint();
      heatPanel.repaint();
      
      control.clearMessages();
   }
   
   
   public void resetAnimation() {
      control.setValue("L", 16);
	  control.setValue("Impurity density", 0.2);
      initializeAnimation();
   }
   
   
   public WangLandau() {
      densityPanel = new PlottingPanel("E", "ln g(E)", "");
      densityPanel.addDrawable(densityData = new Dataset(Color.blue));
      densityPanel.setPreferredMinMaxY(0, 10000);
      densityPanel.setAutoscaleY(true);      
      DrawingFrame frame = new DrawingFrame("Density of States", densityPanel);
      
      histogramPanel = new PlottingPanel("E", "H(E)", "");
      histogramPanel.addDrawable(histogramData = new Dataset());
      histogramPanel.setPreferredMinMaxY(0, 10000);
      histogramPanel.setAutoscaleY(true);      
      frame = new DrawingFrame("Histogram", histogramPanel);
      
      heatPanel = new PlottingPanel("T", "C(T)", "");
      heatPanel.addDrawable(heatData = new Dataset(Color.black, Color.red, true));
      heatPanel.setPreferredMinMax(0.5, 6, 0, 1000);      
      heatPanel.setAutoscaleX(true);
      heatPanel.setAutoscaleY(true);      
      heatData.setMarkerShape(Dataset.NO_MARKER);
      heatData.setSorted(true);
      frame = new DrawingFrame("Heat Capacity", heatPanel);
   }
   
   
   public static void main (String[] args) {
      WangLandau app = new WangLandau();
      Control control = new AnimationControl(app);
      app.setControl(control);
   }
}
