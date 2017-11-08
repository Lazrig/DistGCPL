/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.parties;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import patientlinkage.DataType.DispatchWorkerInfo;

/**
 *
 * @author ibrahim
 */
public class GenCoordinator extends network.Server {

    int port;
    ArrayList<int[]> MyBlocksCnt;
    ArrayList<int[]> EvalBlocksCnt;
    int BlockVar;
    public ObjectOutputStream genOs ;
    public ObjectInputStream genIs;
        
    public GenCoordinator(int port) {
        this.port = port;
        //this.BlockVar = BlkVar;
        //this.MyBlocksCnt = AblocksCnt;
        genOs=null;
        genIs=null;
    }

    public void start() {

        try {
            
            System.out.println("\nWaiting For Party B Coordinator to connect!...");
            listen(port);
            System.out.println("Party B (Evaluator)'s Coordinator Connected!");

            //System.out.printf("\n Starting initial Block sizes Exechange for Blking Var(%d) !.... %n", BlockVar);
            genOs = new ObjectOutputStream(os);
            genOs.flush();
            genIs = new ObjectInputStream(is);
            
           // System.out.printf("\n b4 calling exchng");
            //exChngBlockCnts();
            //genOs.reset();
            //System.out.printf("\n Gen Blk xchng completed..");

        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<int[]> getEvalBlockCount() {
        return EvalBlocksCnt;
    }

    public void stop() {
        try {
            genOs.writeObject("Stop!");
            genOs.flush();
            while (true) {
                String ch = (String) genIs.readObject();
                //System.out.println("* " + ch);
                if (ch.equals("Stop!")) {
                    break;
                }
            }
            System.out.println("\nGen.  is Done, Diconnecting from Eval Coordinator!.....");

            disconnect();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//Send information about the new set of Blocks assignements Info. (WorkerIp, Port, BlkId)
    public synchronized void sendNewDispatchWorkersInfo(ArrayList <DispatchWorkerInfo> info) {
        try {
            
            //genOs.flush();
            genOs.reset();
            genOs.writeObject(info);
            genOs.flush();


            //sleep(100);
            //pool.finalize();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    public synchronized void SignalEndOfDispatches(){
        try{
            //genOs.flush();
            genOs.writeObject("End of Dispatches");
            genOs.flush();
            
            System.out.println("\nGenCoord. Completes  the Blk. Var.!.....");
            } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    public void exChngBlockCnts(int BlkVar, ArrayList<int[]> AblocksCnt) {
        int EvalBlkVar;
        this.MyBlocksCnt = AblocksCnt;
        try {
            
            //genOs.flush();
            //genOs.reset();
            genOs.writeObject(BlkVar);
            genOs.flush();
            
            EvalBlkVar = (int) genIs.readObject();
            
            assert(BlkVar==EvalBlkVar);
            System.out.println("\n Gen. reading BlockCnts for Var: "+ EvalBlkVar);
            genOs.writeObject(MyBlocksCnt);
            genOs.flush();

            
            EvalBlocksCnt = (ArrayList<int[]>) genIs.readObject();
            System.out.println("\n Blk cnt writing and reading done..");
            genOs.writeObject("Gen.Done Xchng!");
            genOs.flush();
            
            while (true) {
                String ch = (String) genIs.readObject();
                System.out.println("* " + ch);
                if (ch.equals("Eval.Done Xchng!")) {
                    break;
                }
            }
            System.out.println("\nGen. Blk xchng is Done!.....");

            //sleep(100);
            //pool.finalize();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
