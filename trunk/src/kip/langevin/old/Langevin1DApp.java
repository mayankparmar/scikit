package kip.langevin.old;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
import static java.lang.Math.*;

public class Langevin1DApp extends AbstractSimulation {
   PlotFrame fieldPlot = new PlotFrame("x", "fields", "Langevin");
   HistogramFrame nuctimes = new HistogramFrame("t", "count", "Nucleation Times");
   LangevinDroplet droplet = new LangevinDroplet();
   Langevin1D sim = new Langevin1D(), oldsim, oldoldsim;
   
   enum State {metastable, equilibrating};
   State state;
   
   double equilibrationTime = 5;
   int stepsPerDisplay;
   
   // range of time for which to collect nucleating droplets
   double lowBound, highBound;

   // average difference between crude nucleation time, and real nucleation time
   public double overshootEstimate;
   
   
   void plotFields() {
      fieldPlot.clearData();
      int di = sim.N < 400 ? 1 : (sim.N / 400);
      for (int i = 0; i < sim.N; i += di) {
         fieldPlot.append(0, i*sim.dx, sim.ψ[i]);
         fieldPlot.append(1, i*sim.dx, sim.φ[i]);
      }
      fieldPlot.setMessage("t = " + (int)sim.t);      
   }
   
   
   public void initialize() {
      overshootEstimate = control.getDouble("Intervention overshoot");
      lowBound = control.getDouble("Droplet low bound");
      highBound = control.getDouble("Droplet high bound");
      
      sim.initialize(control);
      droplet.initialize(control);
      
      state = State.equilibrating;
      fieldPlot.setPreferredMinMax(0, sim.L, -1, 1);      
   }
   
   
   public void doStep() {
      for (int i = 0; i < stepsPerDisplay; i++) {
         sim.h = (state == State.equilibrating) ? -abs(sim.h) : abs(sim.h);
         sim.step();

         switch (state) {
         case metastable:
            if (sim.t - oldsim.t > 4*overshootEstimate) {
               oldoldsim = oldsim;
               oldsim = (Langevin1D)sim.clone();
            }

            if (sim.nucleated()) {
               if (lowBound < sim.t && sim.t < highBound) {
                  droplet.findDroplet(oldoldsim, sim.t-overshootEstimate, sim.dropletLocation());
               }
               nuctimes.append(sim.t);
            }
            
            if (sim.nucleated() || sim.t > highBound) {
               control.setAdjustableValue("Random seed", ++sim.randomSeed);
               sim.initialize(control);
               state = State.equilibrating;
            }
            break;
            
         case equilibrating:
            if (sim.t > equilibrationTime) {
               sim.t = 0;
               sim.h = -sim.h;
               state = State.metastable;
               oldoldsim = oldsim = (Langevin1D)sim.clone();
            }
            break;
         }
      }
      
      plotFields();
      droplet.plotDroplet();
   }
   
   
   public void startRunning() {
      sim.getParameters(control);
      stepsPerDisplay = control.getInt("Steps per display");
   }
   
   
   public void reset() {
      control.setAdjustableValue("Steps per display", 2000);
      control.setValue("Intervention overshoot", 10);
//      control.setValue("Intervention dt", 0.1);
      control.setValue("Droplet low bound", 10000);
      control.setValue("Droplet high bound", 10000);
      control.setValue("Data path", "" /*"/Users/kbarros/dev/nucleation/droplet_profiles4"*/);
      
      control.setValue(" ", "");
      control.setValue("Random seed", 0);
      control.setValue("Crude cutoff", 0);

// Parameters to match Aaron
	  control.setAdjustableValue("\u03bb", 0); // λ
	  control.setAdjustableValue("h", 0.075);
      control.setValue("Length", 100);
      control.setValue("dx", 2);
      control.setAdjustableValue("dt", 0.1);
	  control.setAdjustableValue("R\u00b2", 10); // R2
	  control.setAdjustableValue("\u03b5", "-0.27775"); // ε
      control.setAdjustableValue("\u03b1", 1); // α
      control.setAdjustableValue("\u0393", 0.6); // Γ


//Old Parameters before Aaron matching
/*
	  control.setAdjustableValue("\u03bb", 0); // λ
	  control.setAdjustableValue("h", 0.223);
      control.setValue("Length", 50);
      control.setValue("dx", 0.5);
      control.setAdjustableValue("dt", 0.1);
	  control.setAdjustableValue("R\u00b2", 0.1); // R2
	  control.setAdjustableValue("\u03b5", "-5/9"); // ε
      control.setAdjustableValue("\u03b1", 1); // α
      control.setAdjustableValue("\u0393", 0.005); // Γ
*/
/*
	  control.setAdjustableValue("\u03bb", 0); // λ
	  control.setAdjustableValue("h", 0.223);
      control.setValue("Length", 1000);
      control.setValue("dx", 10);
      control.setAdjustableValue("dt", 0.1);
	  control.setAdjustableValue("R\u00b2", 100); // R2
	  control.setAdjustableValue("\u03b5", "-5/9"); // ε
      control.setAdjustableValue("\u03b1", 1); // α
      control.setAdjustableValue("\u0393", 0.5); // Γ
*/
   }
   
   
   public void copySeed() {
      for (int i = 0; i < sim.N; i++) {
         sim.ψ[i] = droplet.getψ(i);
         sim.φ[i] = droplet.getφ(i);
      }
      sim.t = 0;
      state = State.metastable;
      oldoldsim = oldsim = (Langevin1D)sim.clone();
      sim.ψcutoff = 10; // no more nucleation!
      fieldPlot.repaint();
   }
   
   
   public static void main(String[] args) {
      SimulationControl c = SimulationControl.createApp(new Langevin1DApp());
      c.addButton("copySeed", "Copy Seed");
   }
}
