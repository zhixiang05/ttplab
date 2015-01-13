package solver;

import ttp.TTP1Instance;
import ttp.TTPSolution;
import utils.Deb;
import utils.TwoOptHelper;


/**
 * local search algorithms 
 * based on TSP's 2-opt and KP's Bit-flip
 * 
 * @author kyu
 *
 */
public class Joint2optBF extends LocalSearch {
  
  
  public Joint2optBF() {
    super();
  }
  
  public Joint2optBF(TTP1Instance ttp) {
    super(ttp);
  }
  
  public Joint2optBF(TTP1Instance ttp, TTPSolution s0) {
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
    
    // initial solution data
    int[] tour = sol.getTour();
    int[] pickingPlan = sol.getPickingPlan();
    int[] mapCI = new int[nbCities];      // city/index store
    
    // delta parameters
    int deltaP, deltaW;
    double deltaT = .0;
    
    // improvement indicator
    boolean improv;
    
    // best solution
    int iBest=0, jBest=0, kBest=0;
    double GBest = sol.ob;
    
    // neighbor solution
    int fp = 0;
    double ft = .0, G = .0;
    int nbIter = 0;
    int wc;
    int i=3, j=7, k=5;
    double[] tacc = new double[nbCities];  // tmp time acc
    int[] wacc = new int[nbCities];
    
    do {
      /* map indices to their associated cities */
      for (int q=0; q<nbCities; q++) { // @todo move this to solution coding?
        mapCI[tour[q]-1] = q;
      }
      
      improv = false;
      nbIter++;
      
      // TSP 2opt exchange
      for (i=1; i<nbCities-1; i++) {
        for (j=i+1; j<nbCities; j++) {
          
          // calc tacc n' wacc
          for (int q=0; q<nbCities; q++) { // stop at i-1...
            tacc[q] = sol.timeAcc[q];
            wacc[q] = sol.weightAcc[q];
          }
          
          /* calculate final time with partial delta */
          ft = sol.ft;
          wc = i-2 < 0 ? 0 : sol.weightAcc[i-2]; // fix index...
          double ftacc = i-2 < 0 ? .0 : sol.timeAcc[i-2]; // @todo remove ftacc, use integer suite numbers
          
          for (int q=i-1; q<=j; q++) {
            
            wc += TwoOptHelper.get2optValue(q, sol.weightRec, i, j);
            int c1 = TwoOptHelper.get2optValue(q, tour, i, j)-1;
            int c2 = TwoOptHelper.get2optValue((q+1)%nbCities, tour, i, j)-1;
            
            deltaT = -sol.timeRec[q] + D[c1][c2]/(maxSpeed-wc*C);
            
            // accumulate final time
            ft = ft + deltaT;
            
            // fix time accumulator
            ftacc = ftacc + D[c1][c2]/(maxSpeed-wc*C);
            
            tacc[q] = ftacc; // need to continue 'till the end...
            wacc[q] = wc;
          }
          for (int q=j+1;q<nbCities;q++) {
            double diff = sol.timeAcc[q]-sol.timeAcc[q-1];
            tacc[q] = tacc[q-1] + diff;
          }
          
          
          /* ****************************** */
          /* on bit flip                    */
          /* ****************************** */
          for (k=0; k<nbItems; k++) {
            
            /* cleanup and stop execution */
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
            // BF index on original tour
            int origBF = mapCI[ A[k]-1 ];
            // BF index on changed tour
            int newBF = TwoOptHelper.get2optIndex(origBF, tour, i, j);
            
            sol.weightRec[origBF] += deltaW;
            
            // starting history params
            wc = newBF==0 ?  0 : wacc[newBF-1];
            ft = newBF==0 ? .0 : tacc[newBF-1];
            
            // recalculate velocities from start
            for (int r=newBF; r<nbCities; r++) {
              
              wc += TwoOptHelper.get2optValue(r, sol.weightRec, i, j);
              
              int c1 = TwoOptHelper.get2optValue(r, tour, i, j)-1;
              int c2 = TwoOptHelper.get2optValue((r+1)%nbCities, tour, i, j)-1; // todo: avoid using the % operator...
              
              ft += D[c1][c2] / (maxSpeed-wc*C);
            }
            // remove delta
            sol.weightRec[origBF] -= deltaW;
            
            G = fp - ft*R;
//            Deb.echo("("+i+","+j+","+k+") iBF:"+refBF+" || "+"ft: "+ ft + " | G: " + G + " | fp: "+fp);
            
            // update best
            if (G > GBest) {
              
              iBest = i;
              jBest = j;
              kBest = k;
              GBest = G;
              improv = true;
              
              if (firstfit) break;
            }
            
          } // END FOR k
          if (firstfit && improv) break;
        } // END FOR j
        if (firstfit && improv) break;
      } // END FOR i
      
      /*
       * test improvement
       */
      if (improv) {
        
        // 2opt invert
        TwoOptHelper.do2opt(tour, iBest, jBest);
        
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
      
    } while(improv);
    
    return sol;
  }
  

}
