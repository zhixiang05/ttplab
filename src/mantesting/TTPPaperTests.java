package mantesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import solver.*;
import ttp.TTP1Instance;
import ttp.TTPSolution;
import utils.Deb;
import utils.Log;
import utils.RandStr;

public class TTPPaperTests {
  
  /**
   * max execution time in seconds
   */
  static final int maxTime = 600;
  
  
  /**
   * initial tour generation
   * l: Lin-Kernighan
   * o: optimal
   * r: random
   * g: greedy
   * s: simple
   */
  static final char tourAlgo = 'l';
  
  
  /**
   * initial picking plan generation
   * z: zeros
   * r: random
   * g: greedy
   */
  static final char ppAlgo = 'g';
  
  
  
  
  static final String[] instFolders = {
    "eil51-ttp",
    "berlin52-ttp",
    "eil76-ttp",
    "kroA100-ttp",
    "pr124-ttp",
    "ch150-ttp",
    "kroA200-ttp",
    "u159-ttp",
    "ts225-ttp",
    "a280-ttp",
    "lin318-ttp",
    "u574-ttp",
    "u724-ttp",
    "dsj1000-ttp",
    "rl1304-ttp",
  };
  
  
  static final String[] KPTypes = {
    "uncorr",
    "uncorr-similar-weights",
    "bounded-strongly-corr",
  };
  
  
  static final int[] knapsackCategories = {
//    2,// X
    6,
    10,
  };
  
  
  static final int[] itemFactor = {
//    1,
    3,
    10,
  };
  
  
  static final int nbRep = 5;
  
  static final String[] exclude = {
    "eil51_n150_uncorr_06.ttp",
    "berlin52_n153_uncorr_06.ttp",
    "eil76_n225_uncorr_06.ttp",
    "kroA100_n297_uncorr_06.ttp",
    "pr124_n369_uncorr_06.ttp",
    "ch150_n447_uncorr_06.ttp",
    "kroA200_n597_uncorr_06.ttp",
    "u159_n474_uncorr_06.ttp",
    "ts225_n672_uncorr_06.ttp",
    "a280_n837_uncorr_06.ttp",
    "lin318_n951_uncorr_06.ttp",
    "u574_n1719_uncorr_06.ttp",
    "u724_n2169_uncorr_06.ttp",
    "dsj1000_n2997_uncorr_06.ttp",
    "rl1304_n3909_uncorr_06.ttp",
    "eil51_n150_uncorr-similar-weights_06.ttp",
    "berlin52_n153_uncorr-similar-weights_06.ttp",
    "eil76_n225_uncorr-similar-weights_06.ttp",
    "kroA100_n297_uncorr-similar-weights_06.ttp",
    "pr124_n369_uncorr-similar-weights_06.ttp",
    "ch150_n447_uncorr-similar-weights_06.ttp",
    "kroA200_n597_uncorr-similar-weights_06.ttp",
    "u159_n474_uncorr-similar-weights_06.ttp",
    "ts225_n672_uncorr-similar-weights_06.ttp",
    "a280_n837_uncorr-similar-weights_06.ttp",
    "lin318_n951_uncorr-similar-weights_06.ttp",
    "u574_n1719_uncorr-similar-weights_06.ttp",
    "u724_n2169_uncorr-similar-weights_06.ttp",
    "dsj1000_n2997_uncorr-similar-weights_06.ttp",
    "rl1304_n3909_uncorr-similar-weights_06.ttp",
    "eil51_n150_bounded-strongly-corr_06.ttp",
    "berlin52_n153_bounded-strongly-corr_06.ttp",
    "eil76_n225_bounded-strongly-corr_06.ttp",
    "kroA100_n297_bounded-strongly-corr_06.ttp",
    "pr124_n369_bounded-strongly-corr_06.ttp",
    "ch150_n447_bounded-strongly-corr_06.ttp",
    "kroA200_n597_bounded-strongly-corr_06.ttp",
    "u159_n474_bounded-strongly-corr_06.ttp",
    "ts225_n672_bounded-strongly-corr_06.ttp",
    "a280_n837_bounded-strongly-corr_06.ttp",
    "lin318_n951_bounded-strongly-corr_06.ttp",
    "u574_n1719_bounded-strongly-corr_06.ttp",
  };
  
  /**
   * test function
   */
  public static void main(String[] args) throws FileNotFoundException {
    
    
    /* algorithm settings */
    final LocalSearch algo = new Joint2optBF();
    algo.firstfit();
    algo.noDebug();
    algo.noLog();
    
    // constructive algorithm code
    final String codeS0 = tourAlgo + "" + ppAlgo;
    
    int nbRepRand = ppAlgo == 'r' ? nbRep : 1;
    
    // save result
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    String filename = "./results/"+algo.getName()+"."+codeS0+"."+df.format(new Date())+".csv";
    final PrintWriter out = new PrintWriter(filename);
    
    /* test for all instances */
    for (int u=0;u<nbRepRand;u++) { // instances
      for (final int ifact : itemFactor) {
        for (final int kcat : knapsackCategories) {
          for (final String kptype : KPTypes) {
            for (final String fold : instFolders) {
              
              String tspi = fold.substring(0, fold.length()-4);
              String dimstr = "";
              for (int i=0;i<tspi.length();i++) {
                char c = tspi.charAt(i);
                if (c>='0' && c<='9') dimstr += c;
              }
              
              int dim = Integer.parseInt(dimstr);
              final String name = tspi+"_n"+
                  (dim-1)*ifact+"_"+
                  kptype+"_"+
                  (kcat<10?"0":"")+kcat+".ttp";
              final String ttpi = fold+"/"+name;
              Deb.echo(name);
              
              if (Arrays.asList(exclude).contains(name)) {
                Deb.echo(">> skipped");
                continue;
              }
              
              //if (true) continue;
              
              // TTP instance
              TTP1Instance ttp = new TTP1Instance("./TTP1_data/"+ttpi);
              
              /* initial solution s0 */
              Constructive construct = new Constructive(ttp);
              TTPSolution s0 = construct.generate(codeS0);
              ttp.objective(s0);
              
              /* algorithm setting */
              algo.setS0(s0);
              algo.setTTP(ttp);
              
              
              /*---------------------------------*
               * execute: interruption sensitive *
               *---------------------------------*/
              ExecutorService executor = Executors.newFixedThreadPool(4);
              Future<?> future = executor.submit(new Runnable() {
                  
                @Override
                public void run() {
                  
                  /* execute */
                  long startTime = System.currentTimeMillis();
                  TTPSolution sx = algo.solve();
                  long stopTime = System.currentTimeMillis();
                  long exTime = stopTime - startTime;
                  double exTimeSec = exTime/1000.0;
                  //String time = String.format("%.4f", exTimeSec);
                  
                  /* console output */
                  long ob = Math.round(sx.ob);
                  
                  Deb.echo(codeS0 + " " + ttpi + ":\n"+
                           "Objective: " + ob + "\n"+
                           "Duration : " + String.format("%.4f", exTimeSec) + " sec\n");
                  
                  /* save in result file */
                  out.println(name + " " + ob + " " + 
                              String.format("%.4f", exTimeSec));
                  
                  /* log results (solution, objective value, and runtime) */
                  String namePrefix = "RES."+ttpi.replace('/', '#')+"."+codeS0+"-"+algo.getName()+"."+RandStr.rand(2);
                  Log log = new Log(namePrefix);
                  log.print(sx+"\n"+
                            "Objective: "+sx.ob+"\n"+
                            "Duration : "+exTimeSec+" sec");
                  log.close();
                  
                }
              });
              
              executor.shutdown();  // reject all further submissions
              
              try {
                // wait 1 seconds to finish
                future.get(maxTime, TimeUnit.SECONDS);  
              } catch (InterruptedException e) {
                // possible error cases
                System.out.println("job was interrupted");
              } catch (ExecutionException e) {
                // possible error cases
                System.out.println("caught exception: " + e.getCause());
              } catch (TimeoutException e) {
                // interrupt the job
                future.cancel(true);
                System.out.println("timeout");
              }
              /* end execution */
            }
          }
        }
      }
    } // END for instances
    
    out.close();
  }
}