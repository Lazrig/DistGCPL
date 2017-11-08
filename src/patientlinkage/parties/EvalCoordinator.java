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
import static patientlinkage.Util.Main.DEBUG;

/**
 *
 * @author ibrahim
 */
public class EvalCoordinator extends network.Client {

    String GenCoordAddr;
    int GenCoordPort;
    ArrayList<int[]> MyBlocksCnt;
    ArrayList<int[]> GenBlocksCnt;

    int BlockVar;
    public ObjectOutputStream EvalOs;
    public ObjectInputStream EvalIs;

    ArrayList<DispatchWorkerInfo> recievedGenWorkOrdersInfo;
    ArrayList<String> recievedGenWorkOrdersInfo2;

    public EvalCoordinator(String addr, int port) {
        this.GenCoordAddr = addr;
        this.GenCoordPort = port;
        //this.BlockVar = BlkVar;
        //this.MyBlocksCnt = BblocksCnt;
         EvalOs=null;
         EvalIs=null;
        recievedGenWorkOrdersInfo=new ArrayList();
        recievedGenWorkOrdersInfo2=new ArrayList();
    }

    public void start() {

        try {
            System.out.printf("\nTrying to connect to Party A Coordinator on IP %s and port %d ... %n", GenCoordAddr, GenCoordPort);
            connect(GenCoordAddr, GenCoordPort);
            System.out.println("connected with  Party A Coordinator!");

            //System.out.printf("\n Starting initial Block sizes Exechange for Blking Var(%d) !.... %n", BlockVar);
            EvalOs = new ObjectOutputStream(os);
            EvalOs.flush();
            EvalIs = new ObjectInputStream(is);
            
            //System.out.printf("\n b4 calling exchng");
            //exChngBlockCnts();
            //EvalOs.reset();
            //System.out.printf("\n Eval Blk xchng completed..");

        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        try {
            EvalOs.writeObject("Stop!");
            EvalOs.flush();
            while (true) {
                String ch = (String) EvalIs.readObject();
                //System.out.println("* " + ch);
                if (ch.equals("Stop!")) {
                    break;
                }
            }
            System.out.println("\nEval.  is Done, Diconnecting from Gen. Coordinator!.....");

            
            disconnect();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String recieveNewDispatchWorkersInfo() {
        ArrayList<DispatchWorkerInfo> dispWrkrInfo = new ArrayList();
        Object rObj;
        try {

            rObj = EvalIs.readObject();
            
            if (rObj instanceof ArrayList) {

                dispWrkrInfo = (ArrayList<DispatchWorkerInfo>) rObj;
                recievedGenWorkOrdersInfo.addAll(dispWrkrInfo);
                 if(DEBUG){
                    System.out.println("\n new dispatch read, its size = "+dispWrkrInfo.size());
                    System.out.println("\n first element blkId= "+dispWrkrInfo.get(0).DispatchedBlockID);
                 }
                    return ("More To Come");
            } else if (rObj instanceof String) {
                if (((String) rObj).equals("End of Dispatches")) {
                    return ("done");
                }
            }

            //sleep(100);
            //pool.finalize();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ("done");
    }

    
    //=====================
    
    
    public String recieveNewNotifWorkersInfo() {
        DispatchWorkerInfo dispWrkrInfo ;
        Object rObj;
        try {

            rObj = EvalIs.readObject();
            
            if (rObj instanceof DispatchWorkerInfo) {

                dispWrkrInfo = (DispatchWorkerInfo) rObj;
                recievedGenWorkOrdersInfo.add(dispWrkrInfo);
                if(DEBUG){
                    System.out.println("\n new dispatch read, its IP = "+dispWrkrInfo.WorkerIP);
                    System.out.println("\n  \t\t blkId= "+dispWrkrInfo.DispatchedBlockID);
                }
                return ("More To Come");
            } else if (rObj instanceof String) {
                if (((String) rObj).equals("End of Dispatches")) {
                    return ("done");
                }
            }

            //sleep(100);
            //pool.finalize();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ("none");
    }

    
    //=====================
    
    
    public ArrayList<int[]> getGenBlockCount() {
        return GenBlocksCnt;
    }

    //get next Block to be processed from the recieved dispatched bool, and remeove it from the bool
    public DispatchWorkerInfo getNextBlockWrkrInfo() {
        DispatchWorkerInfo info=null;
        if (!recievedGenWorkOrdersInfo.isEmpty()) {
            info=recievedGenWorkOrdersInfo.get(0);
            recievedGenWorkOrdersInfo.remove(info);
            return (info);
        }
        return (info);
    }

    
    
     public String recieveNewNotifWorkersInfo2() {
        String dispWrkrInfo ;
        Object rObj;
        try {

            rObj = EvalIs.readObject();
            
            if (rObj instanceof String) {
                if (((String) rObj).equals("End of Dispatches")) {
                    return ("done");
                } 
                else
                {

                dispWrkrInfo = (String) rObj;
                recievedGenWorkOrdersInfo2.add(dispWrkrInfo);
                if(DEBUG){
                    System.out.println("\n new dispatch read: "+dispWrkrInfo);
                    System.out.println("\n recievedGenWorkOrdersInfo2 size= "+recievedGenWorkOrdersInfo2.size());
                }
                return ("More To Come");
                
            }
            }

            //sleep(100);
            //pool.finalize();
        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ("none");
    }

    
    
    
    
     //get next Block to be processed from the recieved dispatched bool, and remeove it from the bool
    public String getNextBlockWrkrInfo2() {
        String info=null;
        if (!recievedGenWorkOrdersInfo2.isEmpty()) {
            info=recievedGenWorkOrdersInfo2.get(0);
            recievedGenWorkOrdersInfo2.remove(info);
            return (info);
        }
        return (info);
    }

    
    
    public void exChngBlockCnts(int BlkVar, ArrayList<int[]> BblocksCnt) {
        int GenBlkVar;
        this.MyBlocksCnt = BblocksCnt;
        try {
            GenBlkVar = (int) EvalIs.readObject();
             assert(BlkVar==GenBlkVar);
             System.out.println("\n Eval. reading BlockCnts for Var: "+ GenBlkVar);
             
            //EvalOs.reset(); 
            EvalOs.writeObject(BlkVar);
            EvalOs.flush();
            
            GenBlocksCnt = (ArrayList<int[]>) EvalIs.readObject();

            System.out.println("\nSize of GenBlockCnt read in Eval."+GenBlocksCnt.size());
            System.out.println("GenBlocksCnt[1]="+GenBlocksCnt.get(1).toString());
            
            EvalOs.writeObject(MyBlocksCnt);
            EvalOs.flush();

            System.out.println("\nBlk cnt reading and writing  done..");
            while (true) {
                String ch = (String) EvalIs.readObject();
                //System.out.println("* "+ch);
                if (ch.equals("Gen.Done Xchng!")) {
                    break;
                }
            }
            EvalOs.writeObject("Eval.Done Xchng!");
            EvalOs.flush();
            System.out.println("\nEval. Blk xchng is Done!.....");
            //pool.finalize();
            // sleep(200);

        } catch (Exception ex) {
            Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
