package mantesting;

import solver.Constructive;
import ttp.TTP1Instance;
import ttp.TTPSolution;
import utils.Deb;
import utils.TwoOptHelper;

/**
 * junk testing
 */
public class TestJunk {
  
  public static void mainx(String[] args) {
    int[] tour = {1,3,5,4,6,2};
    int nbCities = tour.length;
    
    // map indices to their associated cities
    int[] mapIC = new int[nbCities];
    for (int i=0; i<nbCities; i++) {
      mapIC[tour[i]-1] = i+1;
    }
    //Deb.echol("idx  : ");Deb.echo(new int[]{1,2,3,4,5,6});
    Deb.echol("tour : ");Deb.echo(tour);
    Deb.echol("wacc: "); Deb.echo(mapIC);
    Deb.echol("tacc: "); Deb.echo(mapIC);
    //Deb.echol(": "); Deb.echo(mapIC);
  }
  
  public static void main(String[] args) {
    
    /* instance information */
    String inst = "my-ttp/sample-data-10.ttp";
    inst = "eil51-ttp/eil51_n50_uncorr_01.ttp";
    inst = "a280-ttp/a280_n279_uncorr_01.ttp";
    TTP1Instance ttp = new TTP1Instance("./TTP1_data/"+inst);
    Deb.echo(ttp);
    
    /* initial solution s0 */
    Constructive construct = new Constructive(ttp);
    TTPSolution s0 = construct.generate("sg");
    //s0.setTour(new int[]{1,3,5,4,6,2});
    ttp.objective(s0);
    
    /* before */
    int[] x = s0.getTour();
    int[] z = s0.getPickingPlan();
    int[] A = ttp.getAvailability();
    Deb.echo();
    //Deb.echol("z   : "); Deb.echo(z,"%4d");
    Deb.echol("x   :"); Deb.echo(x,"%4d");
    Deb.echol("wacc:"); Deb.echo(s0.weightAcc,"%4d");
    Deb.echol("tacc:"); Deb.echo(s0.timeAcc,"%4.0f");

    //Deb.echo("ob = "+s0.ob);
    //Deb.echo("ft = "+s0.ft);
    //Deb.echo("fp = "+s0.fp);
    Deb.echo();
    //if (true) return;

    /* after */
    int i=2, j=16; // 2-opt
    TwoOptHelper.do2opt(x, i, j);
    //int k=3;      // bit-flip
    //z[k] = z[k]!=0 ? 0 : A[k];
    
    // compute objective value
    ttp.objective(s0);
    
    Deb.echo("neighbor solution:");
    //Deb.echol("z   :"); Deb.echo(z,"%4d");
    Deb.echol("x   :"); Deb.echo(x,"%4d");
    Deb.echol("wacc:"); Deb.echo(s0.weightAcc,"%4d");
    Deb.echol("tacc:"); Deb.echo(s0.timeAcc,"%4.0f");

    //Deb.echol("wr:"); Deb.echo(s0.weightRec,"%4d");
    //Deb.echo("ob = "+s0.ob);
    //Deb.echo("ft = "+s0.ft);
    //Deb.echo("fp = "+s0.fp);
    
  }
}
