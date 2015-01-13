package solver;

import ttp.TTP1Instance;
import ttp.TTPSolution;
import utils.Deb;
import utils.SwapHelper;

/**
 * local search algorithms 
 * based on TSP's N1 and KP's Bit-flip
 * 
 * @author kyu
 *
 */
public class JointN1BF extends LocalSearch {
  
  
  public JointN1BF() {
    super();
  }
  
  public JointN1BF(TTP1Instance ttp) {
    super(ttp);
  }
  
  public JointN1BF(TTP1Instance ttp, TTPSolution s0) {
    super(ttp, s0);
  }
  
  @Override
  public TTPSolution solve() {
    
    // calculate initial objective value
    ttp.objective(s0);
    
    // copy initial solution into improved solution
    TTPSolution sol = s0.clone();//, sBest = s0.clone();
    
    // TTP data
    int nbCities = ttp.getNbCities();
    int nbItems = ttp.getNbItems();
    long[][] D = ttp.getDist();
    int[] A = ttp.getAvailability();
    double maxSpeed = ttp.getMaxSpeed();
    double minSpeed = ttp.getMinSpeed();
    long capacity = ttp.getCapacity();
    double C = (maxSpeed - minSpeed) / capacity;
    double R = ttp.getRent();
    
    // solution data
    int[] tour = sol.getTour();
    int[] pickingPlan = sol.getPickingPlan();
    int[] mapCI = new int[nbCities];          // city/index store
    
    // iterations indicators
    boolean improv;
    int nbIter = 0;
    
    // delta parameters
    int deltaP, deltaW;
    double deltaT;
    
    // swapped cities
    int c1, c2, c3, c4;
    
    // changed velocities
    double v1, v2, v3, v2i;
    
    // best solution params
    int iBest=0, kBest=0;
    double GBest = sol.ob;
    
    // neighbor solution data
    int fp = sol.fp;
    double ft = sol.ft, G = sol.ob;
    int wc,       // current weight
        newWA,
        oldWA;    // saved WA at swap index
    int refBF;    // one-bit-flip index
    double[] tacc = new double[nbCities];  // tmp time acc
    
    do {
      
      /* map indices to their associated cities */
      for (int q=0; q<nbCities; q++) { // @todo move this to solution coding?
        mapCI[tour[q]-1] = q;
      }
      
      // improvement checker and iterator
      improv = false;
      nbIter++;
      
      /* ****************************** */
      /* swap two adjacent cities       */
      /* ****************************** */
      for (int i=1;i<nbCities-1; i++) {
        
        oldWA = sol.weightAcc[i];
        newWA = sol.weightAcc[i-1] + sol.weightAcc[i+1] - sol.weightAcc[i];
        
        /* compute time using delta technique */
        // affected cities
        c1 = tour[i-1]-1;
        c2 = tour[i]-1;
        c3 = tour[i+1]-1;
        c4 = tour[(i+2)%nbCities]-1;
    
        // partial velocities
        v1 = maxSpeed - sol.weightAcc[i-1]*C;
        v2 = maxSpeed - sol.weightAcc[i]  *C;
        v3 = maxSpeed - sol.weightAcc[i+1]*C;
        v2i= maxSpeed - newWA*C;
        
        // compute objective
        deltaT = - D[c1][c2]/v1 - D[c2][c3]/v2  - D[c3][c4]/v3
                 + D[c1][c3]/v1 + D[c3][c2]/v2i + D[c2][c4]/v3;
        ft = sol.ft + deltaT;
        
        /* fix time accumulator */
//        for (int q=0; q<i+1; q++) {
//          tacc[q] = sol.timeAcc[q];
//        }
        if (i-2>-1) tacc[i-2] = sol.timeAcc[i-2]; // @todo sufficient ?
        tacc[i-1] = sol.timeAcc[i-1] - D[c1][c2]/v1 + D[c1][c3]/v1;
        tacc[i] = tacc[i-1] + D[c2][c3]/v2i;
        for (int r=i+1; r<nbCities; r++) {
          tacc[r] = sol.timeAcc[r] + deltaT;
        }
        
        /* apply a swap between node i and node i+1 */
        SwapHelper.doSwap(tour, i);
        sol.weightAcc[i] = newWA;
        SwapHelper.doSwap(mapCI, tour[i]-1, tour[i+1]-1);
        
        
        /* ****************************** */
        /* on bit flip                    */
        /* ****************************** */
        for (int k=0; k<nbItems; k++) {
          
          /* cleanup and stop execution if interrupted */
          if (Thread.currentThread().isInterrupted()) {
            return sol;
          }
          
          /* check if new weight doesn't exceed knapsack capacity */
          if (pickingPlan[k]==0 && ttp.weightOf(k)>sol.wend) {
            continue;
          }
          
          /*
           * sub-KP: calculate delta, calculate total profit
           */
          if (pickingPlan[k]==0) {
            deltaP = ttp.profitOf(k);
            deltaW = ttp.weightOf(k);
          }
          else {
            deltaP = -ttp.profitOf(k);
            deltaW = -ttp.weightOf(k);
          }
          fp = sol.fp + deltaP;
          
          
          /*
           * velocity-TSP
           * TSP constrained with knapsack weight
           */
          // index where Bit-Flip happened
          refBF = mapCI[ A[k]-1 ];
          
          // starting time
          ft = refBF==0 ? .0 : tacc[refBF-1];
          
          // recalculate velocities from start
          for (int r=refBF; r<nbCities; r++) {
            wc = sol.weightAcc[r] + deltaW;
            ft += D[tour[r]-1][tour[(r+1)%nbCities]-1] / (maxSpeed-wc*C);
          }
          
          /* compute objective value */
          G = fp - ft*R;
          
          if (G > GBest) {
            iBest = i;
            kBest = k;
            GBest = G;
            improv = true;
            
            if (firstfit) break;
          }
          
        } // END FOR k
        
        // swap back to retrieve initial solution
        SwapHelper.doSwap(tour, i);
        sol.weightAcc[i] = oldWA;
        SwapHelper.doSwap(mapCI, tour[i]-1, tour[i+1]-1);
        
        if (firstfit && improv) break;
      } // END FOR i
      
      if (improv) {
        
        // swap
        SwapHelper.doSwap(tour, iBest);
        
        // bit-flip
        pickingPlan[kBest] = pickingPlan[kBest]!=0 ? 0 : A[kBest];
        
        ttp.objective(sol);
        
        // for more accuracy in unit testing
        sol.ob = GBest;
        if (firstfit) {
          sol.ft = ft;
          sol.fp = fp;
        }
        
        // debug print
        if (this.debug) {
          Deb.echo("Best "+nbIter+":");
          Deb.echo(sol);
          Deb.echo("ob-best: "+sol.ob);
          Deb.echo("wend   : "+sol.wend);
          Deb.echo("---");
        }
      }
      
//      improv=false;
    } while(improv);
    
    return sol;
  }
  
}
