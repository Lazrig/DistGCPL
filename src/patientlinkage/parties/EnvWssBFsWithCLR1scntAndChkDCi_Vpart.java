/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.parties;

import patientlinkage.GarbledCircuit.PatientLinkageWssBFGadget;
import patientlinkage.DataType.PatientLinkage4GadgetWsInputs;
import patientlinkage.DataType.PatientLinkageWssBFOutput;
import cv.CVCompEnv;
import flexsc.CompEnv;
import flexsc.CompPool;
import flexsc.Mode;
import flexsc.Party;
import gc.GCEva;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
//import jdk.management.resource.ResourceType;
import patientlinkage.DataType.PatientLinkage2;
import patientlinkage.DataType.PatientLinkage4GadgetInputsAndClr1sChkDci;
import patientlinkage.DataType.PatientLinkage4GadgetWsBF1sCntInputs;
import patientlinkage.GarbledCircuit.PatientLinkageWssBFw1sCntCLRGadget;
import patientlinkage.GarbledCircuit.PatientLinkageWssBFw1sCntGadget;
import static patientlinkage.Util.Main.DEBUG;
import patientlinkage.Util.Util;
import static patientlinkage.Util.Util.copyOfRange2d;
import pm.PMCompEnv;

/**
 *
 * @author cf
 * @param <T>
 */
public class EnvWssBFsWithCLR1scntAndChkDCi_Vpart <T> extends network.Client{
    String addr;
    int port;
    Mode mode;
    boolean[][][] bin_b;
    int[] BFs1sCnt_b;
    int[] BFs1sCnt_a;
    //boolean[][] Ws_b;
    //boolean[]threshold_b;
    boolean[][] chkDCi;
    int len_a;
    int len_b;
    ArrayList<PatientLinkage2> linkage;
    ArrayList<String> PartyA_IDs;
    ArrayList<String> PartyB_IDs;
    int numOfTasks;
    int nHtasks;
    int nVtasks;

    public EnvWssBFsWithCLR1scntAndChkDCi_Vpart(String addr, int port, Mode mode, int numOfTasks,int hTasks,int vTasks, boolean[][][] bin_b,int[] BFs1sCnt_b,boolean[][] chkDCi, int len_a, ArrayList<String> PartyB_IDs) {
        this.addr = addr;
        this.port = port;
        this.mode = mode;
        this.bin_b = bin_b;
        this.BFs1sCnt_b = BFs1sCnt_b;
        //this.Ws_b = Ws_b;
        //this.threshold_b = threshold_b;
        this.chkDCi=chkDCi;
      
        this.len_a = len_a;
        len_b=bin_b.length;
        this.PartyB_IDs = PartyB_IDs;
        this.numOfTasks = numOfTasks;
        this.nHtasks=hTasks;
        this.nVtasks=vTasks;
    }

    public void implement(int Part) {
        int vTasks=this.nVtasks; // vTasks must be equal to total numOfTasks in the config
        CompPool.MaxNumberTask=vTasks;
        int hTasks=this.nHtasks;//1;//numOfTasks;
        if (hTasks>len_a) hTasks=len_a;
        
        if(vTasks>this.bin_b.length) vTasks=this.bin_b.length;
        int[][] Range0 = Util.linspace(0, this.len_a, hTasks);
        int[][] vRange = Util.linspace(0, this.bin_b.length, vTasks); //vTasks
        if(DEBUG)
            System.out.println("vTasks="+vTasks);
        try {
            if(DEBUG)
                System.out.println("\n Trying to connect with the generator on Addr:port!"+addr+":"+port);
            
            connect(addr, port);
            if(DEBUG)
                System.out.println("connected with the generator!");

            CompEnv<T> eva = null;

            if (null != mode) switch (mode) {
                case REAL:
                    eva = (CompEnv<T>) new GCEva(is, os);
                    os.flush();
                    break;
                case VERIFY:
                    eva = (CompEnv<T>) new CVCompEnv(is, os, Party.Bob);
                    break;
                case COUNT:
                    eva = (CompEnv<T>) new PMCompEnv(is, os, Party.Bob);
                    break;
                default:
                    break;
            }
            
            if(DEBUG)
                System.out.println("Eval starting data exchng...");
            
             
             ObjectInputStream is0 = new ObjectInputStream(is); //blocks to read header
             // swapped to make Gen  create ObjS-out before the this
             ObjectOutputStream os0 = new ObjectOutputStream(os);
             os.flush();
             os0.flush();
            //input
            if(DEBUG)
                System.out.println("initializing patient linkage circuit ...");
            Object[] inputs = new Object[vTasks];//[this.numOfTasks];
            boolean[][][] bin_a = Util.generateDummyArray(bin_b, len_a);
            //boolean[][][] BFs1sCnt_a = Util.generateDummyArray(BFs1sCnt_b, len_a);
            
            //boolean[][] Ws_a = new boolean[this.Ws_b.length][this.Ws_b[0].length];
            //boolean[] threshold_a = new boolean[this.threshold_b.length];
            boolean[][] achkDCi;//=new boolean[len_a][bin_b.length];
            
            
             
             //os.flush();
             if(DEBUG)
                System.out.println("Eval starting data exchng...");
             
             
             achkDCi= (boolean[][])is0.readObject();
             
             
             if(DEBUG)
                 System.out.println("1-achkDCi read ....ok");
            
            
            os0.writeObject(chkDCi);
            os0.flush();
            
            if(DEBUG)
                System.out.println("2-chkDCi written ....ok");
            //---------
             //ObjectInputStream is00 = new ObjectInputStream(is);
             BFs1sCnt_a= (int[])is0.readObject();
             
             if(DEBUG)
                System.out.println("3-BFs1sCnt_a read ....ok");
             
            //ObjectOutputStream os00 = new ObjectOutputStream(os);
            //os00.flush();
           // os0.reset();
            os0.writeObject(BFs1sCnt_b);
            os0.flush();
            
            if(DEBUG)
                System.out.println("4-BFs1sCnt_b written ....ok");
            
            
            //os0.flush();
            if(DEBUG)
                System.out.println("preparing inputs...");
            //PatientLinkage4GadgetWsInputs.resetBar();
            //PatientLinkage4GadgetWsInputs.all_progresses = len_a + this.bin_b.length * this.numOfTasks;
            
            
           //===================================================================
           
            Object[] Vresults=new Object[hTasks];
            for (int h = 0; h < hTasks;h++){//this.numOfTasks; h++) {
                for(int v=0;v<vTasks;v++) {// it is not neccessary for vTasks=this.numOfTasks
                   
                
                    PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp0 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(Arrays.copyOfRange(bin_a, Range0[h][0], Range0[h][1]), 
                                                                            copyOfRange2d(achkDCi, Range0[h], vRange[v]) ,eva, "Alice", v);
            
                    PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp1 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(Arrays.copyOfRange(bin_b, vRange[v][0], vRange[v][1]),
                                                                    copyOfRange2d(chkDCi, Range0[h], vRange[v]), eva, "Bob", v);
                                                               
             inputs[v] = new Object[]{tmp0, tmp1};
            }
            




//System.out.println(String.format("[%s]%d%%      \r", PatientLinkage4GadgetWsInputs.progress(100), 100));
            


            //1.26.17 os.flush();

            //compute
            if(DEBUG){
                System.out.println("computing patient linkage circuit ...task #"+h);
                System.out.println("\n Eval. pool MaxnumberTask: "+CompPool.MaxNumberTask);
            }
            // PatientLinkageWssBFGadget.resetBar();
            //PatientLinkageWssBFGadget.all_progresses = bin_a.length * len_b;
            CompPool<T> pool = new CompPool<>(eva, this.addr, this.port+1);
            
            Object[] tVresult = pool.runGadget(new PatientLinkageWssBFw1sCntCLRGadget(), inputs);
            //System.out.println(String.format("[%s]%d%%      \r", PatientLinkageWssBFGadget.progress(100), 100));
            pool.finalize();
            
            System.out.println("\n GC thread pool finalized.. results ready ..free ports from :"+(this.port + 1));
            
            Vresults[h]=Util.VunifyObjArray(tVresult, eva, len_b);
            } // for h
            Object[] result1 = new Object[Vresults.length];
            Object[] result2 = new Object[Vresults.length];
            //Object[] result3 = new Object[result.length];

            for (int i = 0; i < Vresults.length; i++) {
                result1[i] = ((PatientLinkageWssBFOutput) Vresults[i]).getA();
                result2[i] = ((PatientLinkageWssBFOutput) Vresults[i]).getB();
                //result3[i] = ((PatientLinkageWssBFOutput) result[i]).getC();
            }
            
            
            
            
          //===================================================================   
            
            
            
            
            
            
            
            
            /*for(int i = 0; i < this.numOfTasks; i++){
                PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp0 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(Arrays.copyOfRange(bin_a, Range0[i][0], Range0[i][1]),
                       Arrays.copyOfRange(achkDCi, Range0[i][0], Range0[i][1]), eva, "Alice", i);
                PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp1 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(this.bin_b,
                        Arrays.copyOfRange(chkDCi, Range0[i][0], Range0[i][1]), eva, "Bob", i);
                //PatientLinkage4GadgetWsInputs<T> tmp0 = new PatientLinkage4GadgetWsInputs<>(Arrays.copyOfRange(bin_a, Range0[i][0], Range0[i][1]), Ws_a, threshold_a, eva, "Alice", i);         
                //PatientLinkage4GadgetWsInputs<T> tmp1 = new PatientLinkage4GadgetWsInputs<>(this.bin_b, Ws_b, threshold_b, eva, "Bob", i); 
                inputs[i] = new Object[]{tmp0, tmp1};
            }*/
            
/*
            for(int i = 0; i < this.numOfTasks; i++){
                PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp1 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(Arrays.copyOfRange(this.bin_b, Range0[i][0], Range0[i][1]),
                       Arrays.copyOfRange(chkDCi, Range0[i][0], Range0[i][1]), eva, "bob", i);
                PatientLinkage4GadgetInputsAndClr1sChkDci<T> tmp0 = new PatientLinkage4GadgetInputsAndClr1sChkDci<>(bin_a,
                        Arrays.copyOfRange(achkDCi, Range0[i][0], Range0[i][1]), eva, "Alice", i);
                //PatientLinkage4GadgetWsInputs<T> tmp0 = new PatientLinkage4GadgetWsInputs<>(Arrays.copyOfRange(bin_a, Range0[i][0], Range0[i][1]), Ws_a, threshold_a, eva, "Alice", i);         
                //PatientLinkage4GadgetWsInputs<T> tmp1 = new PatientLinkage4GadgetWsInputs<>(this.bin_b, Ws_b, threshold_b, eva, "Bob", i); 
                inputs[i] = new Object[]{tmp0, tmp1};
            }

*/
//System.out.println(String.format("[%s]%d%%      \r", PatientLinkage4GadgetWsInputs.progress(100), 100));
           /* os.flush();
            //end
            
            //compute
            System.out.println("computing patient linkage circuit ...");
           // PatientLinkageWssBFGadget.resetBar();
            //PatientLinkageWssBFGadget.all_progresses = bin_a.length * len_a;
            CompPool<T> pool = new CompPool<>(eva, this.addr, this.port+1);
            Object[] result = pool.runGadget(new PatientLinkageWssBFw1sCntCLRGadget(), inputs);
            //System.out.println(String.format("[%s]%d%%      \r", PatientLinkageWssBFGadget.progress(100), 100));
            pool.finalize();
            Object[] result1 = new Object[result.length];
            Object[] result2 = new Object[result.length];
            //Object[] result3 = new Object[result.length];
            
            for(int i = 0; i < result.length; i++){
                result1[i] = ((PatientLinkageWssBFOutput)result[i]).getA();
                result2[i] = ((PatientLinkageWssBFOutput)result[i]).getB();
                //result3[i] = ((PatientLinkageWssBFOutput)result[i]).getC();
            }
*/

            T[][] d1 = Util.<T>unifyArray(result1, eva, len_a);
            T[][][] d2 = Util.<T>unifyArray1(result2, eva, len_a);
           // T[][][] d3 = Util.<T>unifyArray1(result3, eva, len_a);
           //1.26.17  os.flush();
            //end
            
            //Output
            if(DEBUG)
                System.out.println("GB protocol completed!");
            for (T[] d11 : d1) {
                eva.outputToAlice(d11);
            }
            os.flush();
            for (T[][] d21 : d2) {
                for (T[] d211 : d21) {
                    eva.outputToAlice(d211);
                }
            }
            os.flush();
//            for (T[][] d31 : d3) {
//                for (T[] d311 : d31) {
//                    eva.outputToAlice(d311);
//                }
//            }
//            os.flush(); 
           //end
            //ObjectInputStream ois = new ObjectInputStream(is);
            linkage = (ArrayList<PatientLinkage2>)is0.readObject();
            //ObjectOutputStream oos = new ObjectOutputStream(os);
           
            
            //1.26.17 os0.flush();
            if(1==Part){
                //os0.reset();
                os0.writeObject(this.PartyB_IDs);
                os0.flush();
                this.PartyA_IDs = (ArrayList<String>)is0.readObject();
                
            }
           //oos.reset();
           
           
           // os0.reset();
           os0.writeObject("Eval.Done!");
           os0.flush();
           
          while( true){
             if(DEBUG) 
                System.out.println("\n Waiting for Gen. Term OK......");  
            String ch=(String)is0.readObject();
            if(DEBUG)
                System.out.println("* "+ch);
            if(ch.equals("Gen.Done!"))
               break;
           }
           if(DEBUG)
                System.out.println("\nEval. Done!.....");
            
            //sleep(200);
            //oos.close();
            disconnect();
            sleep(5);
        } catch (Exception ex) {
            System.out.println("\n Exception in connect:"+ex);
            if(DEBUG)
                ex.printStackTrace(System.err); //.out
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    public ArrayList<PatientLinkage2> getLinkage() {
        return linkage;
    }

    public ArrayList<String> getPartyA_IDs() {
        return PartyA_IDs;
    }

    public int[] getPartyA_1sCnt() {
        return BFs1sCnt_a;
    }
    
}
