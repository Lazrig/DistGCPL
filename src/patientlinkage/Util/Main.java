/*
             * To change this license header, choose License Headers in Project Properties.
             * To change this template file, choose Tools | Templates
             * and open the template in the editor.
 */
package patientlinkage.Util;

import flexsc.CompPool;
import flexsc.Mode;
import gc.GCSignal;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
//import patientlinkage.DataType.BFHelper;
import patientlinkage.DataType.BlkHelper;
import patientlinkage.DataType.DispatchWorkerInfo;
import patientlinkage.DataType.Helper;
import patientlinkage.DataType.PatientLinkage;
import patientlinkage.DataType.PatientLinkage2;
import patientlinkage.DataType.PatientLinkageResults;
import patientlinkage.DataType.TaskConfig;
import patientlinkage.GarbledCircuit.PatientLinkageGadget;
import static patientlinkage.GarbledCircuit.PatientLinkageGadget.getIntArrayFromStrs;
import static patientlinkage.Util.Util.fromInt;
import patientlinkage.parties.Env;
import patientlinkage.parties.EnvWss;
//import patientlinkage.parties.EnvWssBFs;
//import patientlinkage.parties.EnvWssBFsWith1scnt;
//import patientlinkage.parties.EnvWssBFsWith1scntAndChkDCi;
//import patientlinkage.parties.EnvWssBFsWithCLR1scntAndChkDCi;
//import patientlinkage.parties.EnvWssBFsWithCLR1scntAndChkDCi_Vpart;
import patientlinkage.parties.EnvWssF;
import patientlinkage.parties.EvalCoordinator;
import patientlinkage.parties.Gen;
import patientlinkage.parties.GenCoordinator;
import patientlinkage.parties.GenWss;
//import patientlinkage.parties.GenWssBFs;
//import patientlinkage.parties.GenWssBFsWith1scnt;
//import patientlinkage.parties.GenWssBFsWith1scntAndChkDCi;
//import patientlinkage.parties.GenWssBFsWithCLR1scntAndChkDCi;
//import patientlinkage.parties.GenWssBFsWithCLR1scntAndChkDCi_Vpart;
import patientlinkage.parties.GenWssF;

import java.util.*;
import java.util.concurrent.*;
import javax.management.NotificationListener;
import javax.management.Notification;
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.event.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.job.JobInformation;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.management.JPPFManagementInfo;

import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.client.monitoring.jobs.JobMonitor;
import org.jppf.client.monitoring.jobs.JobMonitoringListener;
import patientlinkage.DataType.BatchDataHelper;
import patientlinkage.DataType.Batches;
import patientlinkage.DataType.BlkDataHelper;
import patientlinkage.DataType.BlkSegDataHelper;
import patientlinkage.DataType.BlocksHelper;
import static patientlinkage.Util.Util.readBlockBFsDataForBlockingVar;
import static patientlinkage.Util.Util.readBlocksAndIDsOfAllBlockingVars;

/**
 *
 * @author cf
 */
public class Main {

    /**
     * starting linkage algorithm
     *
     * param args the passing parameters
     */
    
    public static boolean verbose=false;
    public static boolean DEBUG=false;
    public static int port0 ;
    
    public static void startLinkage(String[] args) {
        String file_config = null;
        String file_data = null;
        Properties configs;
        String party = "nobody";
        String addr = null;
        int port = -1;
        int GenCoordPort = 7999;
        boolean Elimination = true; // Remove matched Records by privious step
        double El_threshold=0.93; //0.9 ; //.86; // Elimination threshold
        //Location of attributes
        int IdLoc=1;  // first column is ID
        int[] BlksLoc={2,3,4}; 
        int[] BFDataLoc={5,6,7,8};
        String[] BlkNames={"Ln","Yob","MD"};
        int maxBlkSize = 100;   // For Block segmentation
        int batchSize = 100;  // # of Data Blocks to read with each iteration (to save memory)
        int maxRecsInBatch=1000;  // maximum number of recds in a batch
        int[] BlocksOrderIdx={0,1,2}; 
        int threshold = 1;
        int threads = 1;
        int hTasks = 1;
        int vTasks = 1;
        int numOfParts = 4;
        int records = 0;
        int nWorkers=50;
        boolean filter = false;
        int data_len = 32;// 16;
        String results_save_path = null;
        boolean useBFs = false;
        boolean useCombBF = false;// BFfilter: property, True:  Use combinations of the BFs , False: use  BFs one by one and filter results by removing
        // those ones matched by the previous BF
        boolean PassNumOfOnes = true; //Send BF's number of ones with each record BFs instead of securely computing it.
        boolean useXMLconfigf = false;
        ArrayList<int[]> prop_array = new ArrayList<>();
        ArrayList<Integer> ws = new ArrayList<>();
        ArrayList<int[]> BFprop_array = new ArrayList<>();
        ArrayList<Integer> BFws = new ArrayList<>();
        ArrayList<PatientLinkage> res = null;
        ArrayList<PatientLinkage> tres = null;
        ArrayList<PatientLinkage2> tres2 = null;
        ArrayList<PatientLinkage2> TotBFPartsRes = null;
        ArrayList<PatientLinkage2>[] AllBfsTotBFPartsRes = null;
        ArrayList<PatientLinkage>[] AllBFsRes = null; //(ArrayList<PatientLinkage>[])new ArrayList[4];
        ArrayList<PatientLinkageResults> AllBlksResults = null;
        ArrayList<PatientLinkageResults> taskBlkResults = null;
        boolean[] portUsed;
        int maxNumOfPorts=500;
        boolean[][][] data_bin;

        String[] tmp = null;
        ArrayList<String> PartyA_IDs = null;
        ArrayList<String> PartyB_IDs = null;

        if (args.length < 1) {
            usagemain();
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) != '-') {
                usagemain();
                return;
            }
            try {
                switch (args[i].replaceFirst("-", "")) {
                    case "config":
                        file_config = args[++i];
                        break;
                    case "data":
                        file_data = args[++i];
                        break;
                    case "help":
                        usagemain();
                        break;
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("please input the configure file or data file!");
            } catch (IllegalArgumentException e) {
                System.out.println(args[i] + " is illegal input");
            }
        }

        if (useXMLconfigf) {
            configs = getConfigsFromXML(file_config);
            party = configs.getProperty("party");
            addr = configs.getProperty("address");
            port = Integer.parseInt(configs.getProperty("port"));
            threshold = Integer.parseInt(configs.getProperty("threshold"));
            threads = Integer.parseInt(configs.getProperty("threads"));
            hTasks = Integer.parseInt(configs.getProperty("hTasks"));
            vTasks = Integer.parseInt(configs.getProperty("vTasks"));
            numOfParts = Integer.parseInt(configs.getProperty("numOfParts"));
            records = Integer.parseInt(configs.getProperty("records"));
            filter = Integer.parseInt(configs.getProperty("filter")) == 1; //false;

            results_save_path = configs.getProperty("results_save_path");
            useBFs = Integer.parseInt(configs.getProperty("method")) == 2;
            useCombBF = Integer.parseInt(configs.getProperty("BFfilter")) == 0; //1 means use single BFs and do filter out matched recs

            if (configs.containsKey("comBF")) {
                String BFs = configs.getProperty("comBF");
                tmp = BFs.trim().split("->");
                BFprop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                // BFws.add(Integer.parseInt(tmp[1].trim()));

            }
            if (configs.containsKey("BFweights")) {
                String strBFsw = configs.getProperty("BFweights");
                int[] tmpint;
                tmpint = getIntArrayFromStrs(strBFsw.trim());
                for (int el : tmpint) {
                    BFws.add(el);
                }

            }
            for (int ki = 1; configs.containsKey("com." + ki); ki++) {
                String combs = configs.getProperty("com." + ki);
                tmp = combs.trim().split("->");
                prop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                ws.add(Integer.parseInt(tmp[1].trim()));

            }

        }

        if (!useXMLconfigf) {
            try (FileReader fid_config = new FileReader(file_config); BufferedReader br_config = new BufferedReader(fid_config)) {
                String line;
                while ((line = br_config.readLine()) != null) {
                    String[] strs1 = line.split("\\|");
                    if (strs1.length < 1) {
                        continue;
                    }

                    String str = strs1[0].trim();

                    if (str.equals("") || !str.contains(":")) {
                        continue;
                    }

                    String[] strs2 = str.split(":");

                    switch (strs2[0].trim()) {
                        case "party":
                            party = strs2[1].trim();
                            break;
                        case "address":
                            addr = strs2[1].trim();
                            break;
                            
                        case "port":
                            port = Integer.parseInt(strs2[1].trim());
                            break;
                        case "GenCoordPort":
                            GenCoordPort = Integer.parseInt(strs2[1].trim());
                            break;  
                        case "nWorkers":
                              nWorkers = Integer.parseInt(strs2[1].trim());
                            break;
                        case "DEBUG":
                            DEBUG = Integer.parseInt(strs2[1].trim()) == 1;
                            // Remove matched Records by privious step
                            break;    
                            
                        case "useElimination":
                            Elimination = Integer.parseInt(strs2[1].trim()) == 1;
                            // Remove matched Records by privious step
                            break;
                        case "El_threshold":
                            El_threshold = Double.parseDouble(strs2[1].trim());
                            // Remove matched Records by privious step
                            break; 
               
                            
                        case "threshold":
                            threshold = Integer.parseInt(strs2[1].trim());
                            //threshold = 96;//89~=70%, 96~=75%
                            break;
                        case "threads":
                            threads = Integer.parseInt(strs2[1].trim());
                            break;
                        case "vTasks":
                            vTasks = Integer.parseInt(strs2[1].trim());
                            break;
                        case "hTasks":
                            hTasks = Integer.parseInt(strs2[1].trim());
                            break;
                        case "numOfParts":
                            numOfParts = Integer.parseInt(strs2[1].trim());
                            break;

                        case "filter":
                            filter = Integer.parseInt(strs2[1].trim()) == 1;
                            break;
                        case "records":
                            records = Integer.parseInt(strs2[1].trim());
                            break;
                        case "method":

                            useBFs = Integer.parseInt(strs2[1].trim()) == 2;
                            break;
                        case "BFfilter":

                            useCombBF = Integer.parseInt(strs2[1].trim()) == 0; // 0: means no BF filtering
                            break;
                        case "maxBlkSize":

                            maxBlkSize = Integer.parseInt(strs2[1].trim()) ; 
                            break;   
                        case "maxNoRecsInBatch":

                            maxRecsInBatch = Integer.parseInt(strs2[1].trim()) ; 
                            break;      
                            
                        case "batchSize":

                            batchSize = Integer.parseInt(strs2[1].trim()) ; 
                            break;  
                        case "maxNumOfPorts":    
                            maxNumOfPorts  = Integer.parseInt(strs2[1].trim()) ; 
                            break;
                            
                        case "results_save_path":
                            results_save_path = strs2[1].trim();
                            break;
                        case "locations": // id-Blk1,Blk2,..- BF1,BF2,...
                                           //id-Blocks-BFData,  0 in place of not defined
                            
                            tmp = strs2[1].trim().split("-");
                            if(tmp.length !=3){
                                System.out.println("Location property is not correct, please check the configure file! "
                                        + "\n the correct Locations must be in the form :id-Blocks-BFData, i.e : id-Blk1,Blk2,..- BF1,BF2,... ");
                            throw new AssertionError();
                            } else{
                              IdLoc  = Integer.parseInt(tmp[0].trim());
                              if("0".equals(tmp[1].trim())){
                                BlksLoc=null;  
                              }else{
                              String[] strArray = tmp[1].split(",");
                              
                              BlksLoc = new int[strArray.length];
                              for(int i = 0; i < strArray.length; i++) {
                                    BlksLoc[i] = Integer.parseInt(strArray[i]);
                                }
                              }
                              
                              String[] strArray = tmp[2].split(",");
                               //strArray = tmp[2].split(",");
                               BFDataLoc = new int[strArray.length];
                              for(int i = 0; i < strArray.length; i++) {
                                    BFDataLoc[i] = Integer.parseInt(strArray[i]);
                                }
                            }
                            break;   
                         case "BlkNames":
                            tmp = strs2[1].trim().split(",");
                            BlkNames = new String[tmp.length];
                            System.arraycopy(tmp, 0, BlkNames, 0, tmp.length);
                            
                            break;        
                            
                            
                        case "com":
                            tmp = strs2[1].trim().split("->");
                            prop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                            ws.add(Integer.parseInt(tmp[1].trim()));
                            break;

                        case "comBF":
                            tmp = strs2[1].trim().split("->");
                            BFprop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                            break;

                        case "BFweights":

                            int[] tmpint;
                            tmpint = getIntArrayFromStrs(strs2[1].trim());
                            for (int el : tmpint) {
                                BFws.add(el);
                            }
                            break;
                        default:
                            System.out.println("no property" + strs2[0].trim() + ", please check the configure file!");
                            throw new AssertionError();
                    }

                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
            }

        } //!useXMLconfigf

        int[][] array_int1;
        boolean[][] ws_bin;
        boolean[][] chkPartDCi;
        boolean[][] chkDCi;
        
        //----
        
            //int[][][] PartReductionPerBF = new int[TotnParts][2][numOfUsedBFs];
            long[] BlockVarTotTimes ={0,0,0}; //new double[3];
            int[] BlockVarTotMatches = {0,0,0};//new int[3];
            int[] BlockVarFP = {0,0,0};//new int[3];
            int[] BlockVarUniqMatches = {0,0,0};
            int OverAllFP=0;
        
        
       ArrayList<String> MatchedIDs=new ArrayList<>();
        
         ArrayList<PatientLinkageResults>[] AllBlockVarsRes = (ArrayList<PatientLinkageResults>[]) new ArrayList[3];
         
        
        //----
        
        
       // int BlkVar = 0; //0:ln,1:yob,2:md
       // Helper help1;

        // String ABblocksCntFile="./BFData/AB_ln_Blk_GroupCnt.csv"; // AB_ln_Blk_GroupCnt.csv   AB_md_Blk_GroupCnt.csv    AB_yob_Blk_GroupCnt.csv
        //String ABblocksCntFile = "./BFData/AB_ln_Blk_GroupCnt_1k.csv"; // AB_ln_Blk_GroupCnt.csv   AB_md_Blk_GroupCnt.csv    AB_yob_Blk_GroupCnt.csv
        //BlkIndx which bloking to use: 0:ln,1:yob,2:md
        String MyBlocksCntFile;
        /*String[] AblocksCntFile= {
                   "BFData/A_ln_Blk_GroupCnt.csv","BFData/A_yob_Blk_GroupCnt.csv","BFData/A_md_Blk_GroupCnt.csv"
                          };
        String[] BblocksCntFile= {
                   "BFData/B_ln_Blk_GroupCnt.csv","BFData/B_yob_Blk_GroupCnt.csv","BFData/B_md_Blk_GroupCnt.csv"
                          };
        */
        
        String[] AblocksCntFile= {
                   "BFData/A_ln_Blk_GroupCnt_2k.csv","BFData/A_yob_Blk_GroupCnt_2k.csv","BFData/A_md_Blk_GroupCnt_2k.csv"
                          };
        String[] BblocksCntFile= {
                   "BFData/B_ln_Blk_GroupCnt_2k.csv","BFData/B_yob_Blk_GroupCnt_2k.csv","BFData/B_md_Blk_GroupCnt_2k.csv"
                          };
        
        
        //int BlkIndex = 0;
        ArrayList<int[]> ABblocksCnt = new ArrayList();
        ArrayList<int[]> AblocksCnt = new ArrayList();
        ArrayList<int[]> BblocksCnt = new ArrayList();
        int[] Ablk, Bblk;
        int BlkId;
        int ARecsCntInBlk;
        int BRecsCntInBlk;

       /* if (party.equals("generator")) {
            MyBlocksCntFile = "BFData/A_ln_Blk_GroupCnt_1k.csv";
            AblocksCnt = Util.readPartyBlocksCounts(MyBlocksCntFile);

        } else {
            MyBlocksCntFile = "BFData/B_ln_Blk_GroupCnt_1k.csv";
            BblocksCnt = Util.readPartyBlocksCounts(MyBlocksCntFile);

        }
*/
        //ABblocksCnt=Util.readABblocksCounts(ABblocksCntFile);
        //Remove Blocks with 0 A or B rec counts
        /* for(int rc=ABblocksCnt.size()-1;rc>=0;rc--)
         {
            int[] block0=ABblocksCnt.get(rc);
            if(block0[1]<1 || block0[2]<1)
                ABblocksCnt.remove(rc);
         }
        
        int[] ABblk=ABblocksCnt.get(4);//4 ->  id:179	Arecs:40	Brecs:43
        int BlkId=ABblk[0];
        int ARecsCntInBlk=ABblk[1];
        int BRecsCntInBlk=ABblk[2];
         */
        BlkDataHelper blkhelp;
        BatchDataHelper batchHelper;
        BlkSegDataHelper segBlkhelp;
         BlocksHelper h_AllBlocks;
          int numOfUsedBFs = 0;//BFDataLoc.length;  
          int numOfBlkVars=3; //BlksLoc.length;
          if(numOfBlkVars<=0) numOfBlkVars=1;
       // boolean[][][] Blkdata_bin;
        if (useBFs) {

            System.out.println("start Reading Blocks Ids ...");
            
            
            
            array_int1 = new int[BFprop_array.size()][];
            for (int i = 0; i < array_int1.length; i++) {
                array_int1[i] = BFprop_array.get(i);
            }

            //help1 = Util.readBFsWithProps(file_data, array_int1);
           // help1 = Util.readBFsAndBlocksWithProps(file_data, array_int1);
           h_AllBlocks= readBlocksAndIDsOfAllBlockingVars(file_data,IdLoc,BlksLoc,BFDataLoc  );
            //data_bin = help1.data_bin;

            //if(PassNumOfOnes)
            //      numOf1s= help1.numOfOnesInBFs;
            int[] usedBFws = new int[array_int1[0].length];
            
           // int numOfUsedBFs = 0;  //I need to know how many BFs with properties !=0 (used)
            


      // Which is also the 2nd dim of data_bin
            for (int i = 0; i < array_int1[0].length; i++) {
                if (array_int1[0][i] > 0) {
                    usedBFws[numOfUsedBFs] = BFws.get(i);
                    numOfUsedBFs++;
                }
            }

            ws_bin = new boolean[numOfUsedBFs][];
            for (int i = 0; i < ws_bin.length; i++) {
                ws_bin[i] = fromInt(usedBFws[i], data_len);
            }

            boolean[] threshold_bin = fromInt(threshold, data_len);
            CompPool.MaxNumberTask = threads;
            System.out.println(" encoding data ...done");
            long process_t = 0, t0, t1, tot_t = 0;
            //ArrayList<String> B_IDs;
            //ArrayList<String> A_IDs;
            AllBFsRes = (ArrayList<PatientLinkage>[]) new ArrayList[numOfUsedBFs];
            AllBfsTotBFPartsRes = (ArrayList<PatientLinkage2>[]) new ArrayList[numOfUsedBFs];

            boolean useChkDci = true;
            boolean useBfsInSeq = true;

            process_t = System.currentTimeMillis();

            
            int totBlocks;
            int nBatches;
            //int maxRecsInBatch=1000; // to create batches based on number of recs allowed not # of Blocks
           // int maxNumOfBatchesToLoad=5; // set the number of batches to keep concurrenty in memory for the evaluator
           // ArrayList<int[]> currentBatchesBlkCounts= new ArrayList();
            
            ArrayList<int[]> blksRangesInBatches =new ArrayList(); // to count the Blocks range for each Batch based on max#of Recs
            int AsegmentsCnt = 0;
            int BsegmentsCnt = 0;
            int ARecsCntInBlkSeg;
            int BRecsCntInBlkSeg;
            String ABSegId;
            //System.out.println(" \n System conf Address = " + addr);
            // 
            //TaskConfig tConf = new TaskConfig(party, addr, port, threshold, threads, hTasks, vTasks, numOfParts, PassNumOfOnes, useChkDci, useBfsInSeq);
            // we need to set party foreach task, and addr for eval task

            ArrayList<DispatchWorkerInfo> DispatchInfo = new ArrayList();
            
            maxNumOfPorts=nWorkers*(threads+2);//7000;
             port0 = port; //save initial port
            portUsed=new boolean[maxNumOfPorts];
            switch (party) {
                case "generator":
                    // Let start by assuming each job  has One Task, And we send them all at Once
                    // We can make Jobs Patches (execute a group of jobs "Batch" at once, wait for their completion then execute another batch and so on

                    //TODO: check AblocksCnt size
                    // Connect With Eval Coordinator
                    GenCoordinator genCoord = new GenCoordinator(GenCoordPort);

                    genCoord.start();

                    //--JMXDriverConnectionWrapper jmxDriver = null;
                    //--String listenerId = "";
                    //int nbJobs = 10;//AblocksCnt.size();
                    int nbTasks = 1; //number of tasks per job
                    
                    
                    
                   
                    try (final JPPFClient jppfClient = new JPPFClient()) {

                        // make sure the client has enough connections
                        //ensureSufficientConnections(jppfClient, nbJobs);
                        //--jmxDriver = jppfClient.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
                        // subscribe to task notifications from all nodes
                        //listenerId = jmxDriver.registerForwardingNotificationListener( NodeSelector.ALL_NODES, JPPFNodeForwardingMBean.MBEAN_NAME, new TaskListener(), null, null);
                        // --DriverJobManagementMBean jobMBean = jmxDriver.getJobManager();
                        //--listenerId = jmxDriver.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, new TaskListener(genCoord.genOs), null, null);
                        //--System.out.println("\n Task listener registered: "+listenerId);
                        // synchronization helper that tells us when all jobs have completed
                        for (int BlkVar = 0; BlkVar < numOfBlkVars; BlkVar++) {

                            port = port0; //reset the ports counter to the initial port with every blocking var.
                            for(int up=0;up<maxNumOfPorts;up++)
                                portUsed[up]=false;
                            // this function will use help1.Matched[] to count only none matched recs if Elimination=true
                            //otherwise  help1.Matched[] will stay all false if Elimination= false
                            
                            // try take this out of the loop for the first Var, then for other vars do this after Elim below
                             AblocksCnt = h_AllBlocks.getEachBlockCount(BlocksOrderIdx[BlkVar],Elimination);  //Util.getEachBlockCount(BlocksOrderIdx[BlkVar],help1);
//                          
                            
                               // MyBlocksCntFile = AblocksCntFile[BlocksOrderIdx[BlkVar]];//"BFData/A_ln_Blk_GroupCnt_1k.csv";
//                            AblocksCnt = Util.readPartyBlocksCounts(MyBlocksCntFile);
//
//                            if (Elimination && BlkVar != 0 && AllBlksResults != null) {
//                                System.out.println("Ablock size from file B4 Elim: " +AblocksCnt.size() );
//                                
//                                AblocksCnt = Util.UpdateBlocksRecCountsOfThisParty(BlocksOrderIdx[BlkVar], help1, AblocksCnt);
//                                System.out.println("Ablock size After Elim: " +AblocksCnt.size() );
//                                System.out.println("Ablock [1] " +AblocksCnt.get(1).toString() );
//                            }

                            genCoord.exChngBlockCnts(BlocksOrderIdx[BlkVar], AblocksCnt);  // BlkVar to assert both are on the same blocking var.

                            BlockVarTotTimes[BlkVar] = System.currentTimeMillis();
                            AllBlksResults = null;

                            if (MatchedIDs != null) {
                                MatchedIDs.clear();
                                // MatchedIDs_score.clear();
                            }

                            BblocksCnt = genCoord.getEvalBlockCount();
                            if (ABblocksCnt!=null)
                                 ABblocksCnt.clear();
                            if (AllBlksResults == null) {
                                AllBlksResults = new ArrayList<>();
                            }

                            int JobCount = 0;
                            for (int blk = 0; blk < AblocksCnt.size(); blk++) {
                                Ablk = AblocksCnt.get(blk);
                                ARecsCntInBlk = Ablk[1];
                                BlkId = Ablk[0];
                                BRecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, BblocksCnt);
                                if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {
                                    int[] t = {BlkId, ARecsCntInBlk, BRecsCntInBlk};
                                    ABblocksCnt.add(t);
                                    AsegmentsCnt=1;
                                    BsegmentsCnt=1;
                                        if(ARecsCntInBlk >maxBlkSize){
                                           AsegmentsCnt=  (int) (ARecsCntInBlk/maxBlkSize);
                                           if((ARecsCntInBlk%maxBlkSize) !=0 ) AsegmentsCnt++;
                                        }
                                        if(BRecsCntInBlk >maxBlkSize){
                                            BsegmentsCnt=  (int) (BRecsCntInBlk/maxBlkSize);
                                           if((BRecsCntInBlk%maxBlkSize) !=0 ) BsegmentsCnt++;  
                                        }
                                        
                                                 
                                    JobCount+=(AsegmentsCnt*BsegmentsCnt);
                                }
                            }

                            
                            totBlocks=ABblocksCnt.size();
                            blksRangesInBatches=Util.getBlkRngesOfAllBatches(ABblocksCnt,maxRecsInBatch);
                            nBatches= blksRangesInBatches.size();  //(int)(totBlocks/batchSize);
                                                                 //if((totBlocks%batchSize) !=0 ) nBatches++;
                             System.out.println("\n\n\n #Batches: "+ nBatches );//+" Each has "+ batchSize+"Blks");
                             System.out.println("tot. #Jobs : "+JobCount);
                            ArrayList<int[]> batchABblocksCnt = new ArrayList();
                            //ArrayList<int[]> runningBatches = new ArrayList();
                            // testing small  dataset
                            //JobCount = 5;
                            final CountDownLatch countDown = new CountDownLatch(JobCount);
                            JMXDriverConnectionWrapper jmx = jppfClient.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
                            JMXHandler handler = null;
                            try {
                                // create a handler and register it as a JMX notification listener
                                handler = new JMXHandler(jmx, genCoord.genOs);
                                handler.register();
                                if(DEBUG)
                                   System.out.println("\nJMX handler for jobs monitoring registered" );
                                
                                int fromIdx=0,toIndx=0; 
                                int TotJobsSentSoFar=0;
                                for(int batch=0;batch<nBatches;batch++){
                                    fromIdx= blksRangesInBatches.get(batch)[1]; //batch*batchSize;
                                    toIndx= blksRangesInBatches.get(batch)[2];
                                    batchABblocksCnt=Util.getABBlocks4Range(ABblocksCnt,fromIdx,toIndx-fromIdx+1);//batchSize);
                                    batchHelper=new BatchDataHelper(BlocksOrderIdx[BlkVar],Elimination);
                                    Util.readBatchBFsDataForBlocksRange(file_data, batchHelper,batchABblocksCnt,h_AllBlocks,IdLoc,BlksLoc,BFDataLoc);
                                    int batchJobs=0;//batchABblocksCnt.size();
                                    //int batchJobs2Wait4=batchJobs-nWorkers;
                                    //if (batchJobs2Wait4<0) batchJobs2Wait4=0;
                                    System.out.println(" Proc. Batch#: "+ batch +" |"+(toIndx-fromIdx+1)+"|" );
                                    //final CountDownLatch batchCountDown = new CountDownLatch(batchJobs2Wait4);
                                    
                                    
                                    for (int blk = 0; blk < batchABblocksCnt.size(); blk++) { //AblocksCnt.size()  //5; blk++) { //
                                    //just for test
                                        if (!DispatchInfo.isEmpty()) {
                                            DispatchInfo.clear();
                                        }
                                    //for (int blk=0;blk<5;blk++){    
//                            Ablk = AblocksCnt.get(blk);// 4 ->  id:179	Arecs:40	Brecs:43
//                            BlkId = Ablk[0];
//                            ARecsCntInBlk = Ablk[1];
//
//                            BRecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, BblocksCnt);
                                    Ablk = batchABblocksCnt.get(blk);
                                    BlkId = Ablk[0];
                                    ARecsCntInBlk = Ablk[1];
                                    BRecsCntInBlk = Ablk[2];
                                    if (verbose)
                                        System.out.println(" ................. Processing Var "+BlocksOrderIdx[BlkVar] +" Block (" + BlkId + ")[" + blk + "] ARecs=" + ARecsCntInBlk + ", BRecs=" + BRecsCntInBlk);
                                    else
                                             System.out.print("Blk:"+BlocksOrderIdx[BlkVar]+"."+batch+"."+BlkId+"("+ARecsCntInBlk+"-"+BRecsCntInBlk+")"+", ");
                                    // skip if any of the data sets do not have data belong to this blk Block
                                    // I think I need to do the same technique in PatientLinkageGC-Blocked with Elim 
                                    if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {
                                        //1- intialize the blkDataHelper
                                        blkhelp=new  BlkDataHelper(batchHelper,  BlkId,  Elimination); //BlkDataHelper(BlkId,BlocksOrderIdx[BlkVar],Elimination);
                                        //2- send it to function to read block data from file/batchhelper
                                       // readBlockBFsDataForBlockingVar(file_data, blkhelp, h_AllBlocks);
                                       //readBlockBFsDataFromBatchHelper(batchHelper, blkhelp, h_AllBlocks);
                                        //blkhelp = new BlkHelper(help1, BlkId, BlocksOrderIdx[BlkVar],Elimination);// BlkIndex);
                                        //Blkdata_bin = blkhelp.Blkdata_bin;
                                        if (verbose)
                                            System.out.println("data size: " + blkhelp.Blkdata_bin.length);
                                        //PartyA_IDs = help1.IDs;
                                        //PartyA_IDs = blkhelp.IDs;
                                        if (verbose)
                                            System.out.println("data size: " + blkhelp.Blkdata_bin.length + " A IDs size: " + blkhelp.IDs.size());
                                        
                                        
                                        AsegmentsCnt=1;
                                        BsegmentsCnt=1;
                                        if(ARecsCntInBlk >maxBlkSize){
                                           AsegmentsCnt=  (int) (ARecsCntInBlk/maxBlkSize);
                                           if((ARecsCntInBlk%maxBlkSize) !=0 ) AsegmentsCnt++;
                                        }
                                        if(BRecsCntInBlk >maxBlkSize){
                                            BsegmentsCnt=  (int) (BRecsCntInBlk/maxBlkSize);
                                           if((BRecsCntInBlk%maxBlkSize) !=0 ) BsegmentsCnt++;  
                                        }
                                        for(int aSeg=0;aSeg<AsegmentsCnt;aSeg++){
                                            
                                             segBlkhelp=new  BlkSegDataHelper(blkhelp,  aSeg,  maxBlkSize); 
                                             
                                             for(int bSeg=0;bSeg<BsegmentsCnt;bSeg++){
                                                 
                                                 boolean lastSeg=(bSeg==(BsegmentsCnt-1));
                                                 ABSegId=""+BlkId+"-"+aSeg+"x"+bSeg;
                                                 //ARecsCntInBlkSeg = segBlkhelp.blkSize;
                                                 if(BsegmentsCnt==1)
                                                   BRecsCntInBlkSeg=BRecsCntInBlk;
                                                 else{
                                                     if(lastSeg)
                                                       BRecsCntInBlkSeg=BRecsCntInBlk-bSeg*maxBlkSize; 
                                                     else
                                                        BRecsCntInBlkSeg=maxBlkSize; 
                                                         
                                                    }
                                                     
                                                   
                                              // another way to find b seg # of recs
                                              //int bIdx1=bSeg*maxBlkSize;
                                              //int bIdx2=bIdx1+maxBlkSize;
                                               // BRecsCntInBlkSeg=  (bIdx2<BRecsCntInBlk)? maxBlkSize: BRecsCntInBlk%maxBlkSize;
                                        //Create New Job with a single task in it
                                        // Adding new job 
                                        // Adding Task
                                        TaskConfig tConf = new TaskConfig(party, addr, port, threshold, threads, hTasks, vTasks, numOfParts, PassNumOfOnes, useChkDci, useBfsInSeq);
                                        tConf.party = "Generator";
                                        //String wrkrIp = "localhost";  // this should be gotten from Driver
                                        //tConf.port = port;
                                        //int taskPort = tConf.port;

                                        //tConf.port += (blk % 10);
                                        //tConf.port=port;
                                        int taskPort = tConf.port;

                                        //just for testing
//                                if (DispatchInfo.isEmpty()) {
//                                    System.out.println("\n new dispatch: \n \t size=" + DispatchInfo.size() + " \t BlkId=" + BlkId + "\n          workerIP=" + wrkrIp + "\n         port=" + taskPort);
//                                    DispatchInfo.add(new DispatchWorkerInfo(BlkId, wrkrIp, taskPort));
//                                }
//                                genCoord.sendNewDispatchWorkersInfo(DispatchInfo);
                                        //send this to the notification handler: genCoord.genOs
                                        JPPFJob job = new JPPFJob();
                                        // set the job name
                                        job.setName("J-" + batch+"-"+ ABSegId + "-" + taskPort);
                                        job.setBlocking(false);
                                        tConf.TaskId="J-" + batch+"-"+ ABSegId + "-" + taskPort;
                                        for (int i = 1; i <= nbTasks; i++) {
                                            // create a new task
                                            // add a task using an instance method as entry point

                                            GenTask gTask = new GenTask(segBlkhelp, tConf, BRecsCntInBlkSeg);
                                            job.add(gTask).setId("T-" + BlkId);
                                            //Task<?> BlkTask = job.add("run", gTask);

                                        }
                                        portUsed[port-port0]=true;
                                        // results will be processed asynchronously within
                                        // the job listener's jobEnded() notifications
                                        job.addJobListener(new MyJobListener(portUsed,AllBlksResults, countDown));//,batchCountDown));//--, jobMBean) );

                                        // submit the job
                                        jppfClient.submitJob(job);
                                        
                                        batchJobs++;
                                        
                                        if (verbose)
                                            System.out.println("\n Block " + job.getName() + " submitted on port:" + port);

                                        //port += 10;
                                        // 1.26.17 port += (threads*numOfParts+3);//*numOfParts+2+2;
                                        port += (1+threads+1); // 1 for Gen init. con + #threads + part_jump
                                        if (port >= (port0 + maxNumOfPorts)) {
                                            port = port0;
                                            
                                        }
                                        
                                        while(portUsed[port-port0]){
                                            // 1.26.17  port += (threads*numOfParts+3);
                                           //port += (threads-2)*numOfParts+2+2; 
                                           port += (1+threads+1);
                                           if (port >= (port0 + maxNumOfPorts)) 
                                                port = port0;
                                        }
                                        if(DEBUG)
                                            System.out.println("  next Port:"+port);
                                        // GenTask gTask = new GenTask(blkhelp, tConf, BRecsCntInBlk);
                                        // Execute concurently
                                        //get Results
                                        // taskBlkResults=gTask.run();
                                        // get each task worker IP address
                                        // send them to EvalCoord
                                        // this should be called 1st when distributed, then get the wrkrs info, then send info to eval.
                                        //....taskBlkResults = gTask.run();
                                        }// for bSeg
                                    } // for aSeg
                                    } // if ((ARecsCntInBlk >0 && BRecsCntInBlk>0)
                                        
                                } //for blk

                                // wait until all jobs are complete
                                // i.e. until the count down reaches 0
                               //  }// for batch should be here
                              // batchCountDown.await();
                              TotJobsSentSoFar+=batchJobs;
                              int moreJobsToComplete = (int) (countDown.getCount());
                              int completedJobs = JobCount - moreJobsToComplete;
                              int maxQ=nWorkers;
                              int inQ=TotJobsSentSoFar-completedJobs-nWorkers; //in queue jobs
                              
                              while(inQ>=maxQ)
                              {
                                  sleep(100);
                                  moreJobsToComplete = (int) (countDown.getCount());
                                  completedJobs = JobCount - moreJobsToComplete;
                                  inQ=TotJobsSentSoFar-completedJobs-nWorkers;
                              }
                              
//                              while(completedJobs< (TotJobsSentSoFar-nWorkers))
//                              {
//                                  sleep(100);
//                                  moreJobsToComplete = (int) (countDown.getCount());
//                                  completedJobs = JobCount - moreJobsToComplete;
//                              }
//                               
                               
                            }// for batch
                                // ....????? I need to read next batch while waiting for jobs of previous batch to complete
                                countDown.await();
                            } finally {
                                // unregister the notification listener when not needed anymore
                                if (handler != null) {
                                    handler.unregister();
                                }
                            }

                            System.out.println("\n\n =.=.=.=.=.=.=..=.=.=..=.=.=.=..=.=.=.=.=..=.=.=.=.=.=.=.=.=..=.=.=.=..=\n");
                            System.out.println("\n Blocking Var. " + AblocksCntFile[BlocksOrderIdx[BlkVar]] + " Completed..");

                            AllBlockVarsRes[BlkVar] = AllBlksResults;

                            // we need to incorpoate El_threshold
                            if (Elimination && BlkVar != 2 && AllBlksResults != null) // not last blocking variable
                            {
                                for (int matchIds = 0; matchIds < AllBlksResults.size(); matchIds++) {
                                    if (AllBlksResults.get(matchIds).score >= El_threshold) {
                                        MatchedIDs.add(AllBlksResults.get(matchIds).AID); //at other party BID
                                    }                            //MatchedIDs_score.add(AllBlksResults.get(matchIds).score);
                                }
                                // 1-filterout matched Records by setting Matched field to True
                                h_AllBlocks.FilterOutMatchedRecsByIDs(MatchedIDs); //using Party_IDs

                            }
                            BlockVarTotTimes[BlkVar] = System.currentTimeMillis() - BlockVarTotTimes[BlkVar];
                            BlockVarTotMatches[BlkVar] = AllBlockVarsRes[BlkVar].size();
                            BlockVarFP[BlkVar] = Util.CountFP(AllBlockVarsRes[BlkVar]);
                            BlockVarUniqMatches[BlkVar] = Util.CountUniqueMatches(AllBlockVarsRes[BlkVar]);
                        
                            genCoord.SignalEndOfDispatches();
                        } // FOR BlkVar

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Try client

                    

                    genCoord.stop();
                    break;

                case "evaluator":

                    // Connect With Eval Coordinator
                    //String genCoIpAdd = "localhost"; // we are going to use gen addr from config file as genCoord addr
                    DispatchWorkerInfo rcvdWrkInfo;
                    //String rcvdWrkInfo2;
                    EvalCoordinator EvalCoord = new EvalCoordinator(addr, GenCoordPort);

                    EvalCoord.start();

                    //======================-----======-----====----====---===
                    //MyBlocksCntFile = "BFData/B_ln_Blk_GroupCnt_1k.csv";
                    //BblocksCnt = Util.readPartyBlocksCounts(MyBlocksCntFile);
                    //AblocksCnt = EvalCoord.getGenBlockCount();
                    // JMXDriverConnectionWrapper ejmxDriver = null;
                    //String elistenerId = "";
                    //int nbJobs = BblocksCnt.size();
                    int enbTasks = 1; //number of tasks per job
                    try (final JPPFClient ejppfClient = new JPPFClient()) {
                        // make sure the client has enough connections
                        //ensureSufficientConnections(jppfClient, nbJobs);

                        //ejmxDriver = ejppfClient.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
                        // subscribe to task notifications from all nodes
                        //listenerId = jmxDriver.registerForwardingNotificationListener( NodeSelector.ALL_NODES, JPPFNodeForwardingMBean.MBEAN_NAME, new TaskListener(), null, null);
                        //elistenerId = ejmxDriver.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, new TaskListener(EvalCoord.EvalOs), null, null);
                        //DriverJobManagementMBean jobMBean = ejmxDriver.getJobManager();
                        // we can make eval get the block var from the gen first, then loop while no more blkvars from gen
                        for (int BlkVar = 0; BlkVar < 3; BlkVar++) {
                            for(int up=0;up<maxNumOfPorts;up++)
                                portUsed[up]=false;
                            //...MyBlocksCntFile = BblocksCntFile[BlocksOrderIdx[BlkVar]];//"BFData/B_ln_Blk_GroupCnt_1k.csv";
                           // BblocksCnt = Util.readPartyBlocksCounts(MyBlocksCntFile);
                            //BblocksCnt = Util.getEachBlockCount(BlocksOrderIdx[BlkVar],help1);
                            BblocksCnt=h_AllBlocks.getEachBlockCount(BlocksOrderIdx[BlkVar],Elimination);
//                            if (Elimination && BlkVar != 0 && AllBlksResults != null) {
//                                
//                                System.out.println("Ablock size from file B4 Elim: " +BblocksCnt.size() );
//                                
//                                BblocksCnt = Util.UpdateBlocksRecCountsOfThisParty(BlocksOrderIdx[BlkVar], help1, BblocksCnt);
//                                System.out.println("Ablock size After Elim: " +BblocksCnt.size() );
//                                System.out.println("Ablock [1] " +BblocksCnt.get(1).toString() );
//                                
//                                
//
//                            }

                            EvalCoord.exChngBlockCnts(BlocksOrderIdx[BlkVar], BblocksCnt);  // BlkVar to assert both are on the same blocking var.
                            System.out.printf("\n Main: Eval Blk xchng completed..");
                            BlockVarTotTimes[BlkVar] = System.currentTimeMillis();
                             AllBlksResults = null;
                            AblocksCnt = EvalCoord.getGenBlockCount();
                            
                            if (ABblocksCnt!=null)
                                 ABblocksCnt.clear();
                            
                            if (AllBlksResults == null) {
                                AllBlksResults = new ArrayList<>();
                            }

                            if (MatchedIDs != null) {
                                MatchedIDs.clear();
                                // MatchedIDs_score.clear();
                            }

                           /* int JobCount = 0;
                            for (int blk = 0; blk < BblocksCnt.size(); blk++) {
                                Bblk = BblocksCnt.get(blk);
                                BRecsCntInBlk = Bblk[1];
                                BlkId = Bblk[0];
                                ARecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, AblocksCnt);
                                if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {
                                    int[] t = {BlkId, ARecsCntInBlk, BRecsCntInBlk};
                                    ABblocksCnt.add(t);
                                    JobCount++;
                                }
                            }
                            */
                            int JobCount = 0;
                            for (int blk = 0; blk < AblocksCnt.size(); blk++) {
                                Ablk = AblocksCnt.get(blk);
                                ARecsCntInBlk = Ablk[1];
                                BlkId = Ablk[0];
                                BRecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, BblocksCnt);
                                if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {
                                    int[] t = {BlkId, ARecsCntInBlk, BRecsCntInBlk};
                                    ABblocksCnt.add(t);
                                    AsegmentsCnt=1;
                                    BsegmentsCnt=1;
                                        if(ARecsCntInBlk >maxBlkSize){
                                           AsegmentsCnt=  (int) (ARecsCntInBlk/maxBlkSize);
                                           if((ARecsCntInBlk%maxBlkSize) !=0 ) AsegmentsCnt++;
                                        }
                                        if(BRecsCntInBlk >maxBlkSize){
                                            BsegmentsCnt=  (int) (BRecsCntInBlk/maxBlkSize);
                                           if((BRecsCntInBlk%maxBlkSize) !=0 ) BsegmentsCnt++;  
                                        }
                                        
                                                 
                                    JobCount+=(AsegmentsCnt*BsegmentsCnt);
                                   // JobCount++;
                                }
                            }
                            
                            
                            totBlocks=ABblocksCnt.size();
                            
                            blksRangesInBatches=Util.getBlkRngesOfAllBatches(ABblocksCnt,maxRecsInBatch);
                            nBatches= blksRangesInBatches.size(); 
                                        //nBatches=(int)(totBlocks/batchSize);
                                         //if((totBlocks%batchSize) !=0 ) nBatches++;
                            //ArrayList<int[]> batchABblocksCnt = new ArrayList();
                            
                            // Testing small dataset
                            //JobCount = 5;
                            // to know if all jobs I recieved from Gen and submitted to workers are done
                            final CountDownLatch countDown = new CountDownLatch(JobCount);
                            JMXDriverConnectionWrapper ejmx = ejppfClient.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
                            eJMXHandler ehandler = null;
                            try {
                                // create a handler and register it as a JMX notification listener
                                ehandler = new eJMXHandler(ejmx);
                                ehandler.register();
                                
                                
                                int batchId=0;
                                //int batch=0;
                                Batches currentBatches=new Batches(maxBlkSize,maxRecsInBatch, ABblocksCnt, file_data, BlocksOrderIdx[BlkVar], 
                                                                   Elimination,h_AllBlocks,IdLoc,BlksLoc,BFDataLoc );
                                currentBatches.loadBatch( blksRangesInBatches.get(batchId));
                                
                                
                                //this while loop to track jobs notifications (read from stream)  sent by gen.
                                //System.out.println("\n Notif:="+EvalCoord.recieveNewNotifWorkersInfo2());
                               
                                   
                                EvalCoord.recieveNewNotifWorkersInfo2();
                                while (true) {
                                    // get next work info from pool
                                    String rcvdWrkInfo2;
                                    rcvdWrkInfo2 = EvalCoord.getNextBlockWrkrInfo2();
                                    if (verbose)
                                        System.out.println("\n rcvdWrkInfo := " + rcvdWrkInfo2);
                                    //System.out.println("\n Test if not null: " + rcvdWrkInfo != null);
                                    while (rcvdWrkInfo2 != null) {
                                        
                                        TaskConfig tConf = new TaskConfig(party, addr, port, threshold, threads, hTasks, vTasks, numOfParts, PassNumOfOnes, useChkDci, useBfsInSeq);
                                        String[] rcvdWrk = rcvdWrkInfo2.split("-");
                                        //BlkId = rcvdWrkInfo.DispatchedBlockID;
                                        //tConf.port = rcvdWrkInfo.DispatchAssigned2Port;
                                        //tConf.addr = rcvdWrkInfo.WorkerIP;
                                        batchId=Integer.parseInt(rcvdWrk[1]);
                                        if(!currentBatches.isBatchLoaded(batchId))
                                        {
                                           currentBatches.loadBatch( blksRangesInBatches.get(batchId));
                                           // currentBatches.loadBatch( batchId);  
                                        }
                                       
                                        batchHelper=currentBatches.getBatchHelper(batchId);
                                        
                                        //batchABblocksCnt=currentBatches.getBatchABblkCnt(batchId);
                                        
                                        BlkId = Integer.parseInt(rcvdWrk[2]);
                                        ABSegId=rcvdWrk[3] ;//aSeg+"x"+bSeg;
                                        String[] ABSeg = ABSegId.split("x");
                                        int aSeg=Integer.parseInt(ABSeg[0]);
                                        int bSeg=Integer.parseInt(ABSeg[1]);
                                        
                                        tConf.port = Integer.parseInt(rcvdWrk[4]);
                                        tConf.addr = rcvdWrk[5];
                                        if (verbose)
                                            System.out.println("\n new dispatch rec'd: BlkId=" + BlkId + "\n          workerIP=" + tConf.addr + "\n         port=" + tConf.port);
                                       
                                        //Ablk = batchABblocksCnt.get(BlkId);
                                       
                                        //ARecsCntInBlk = Ablk[1];
                                        //BRecsCntInBlk = Ablk[2];
                                        BRecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, BblocksCnt);
                                        ARecsCntInBlk = Util.getOtherPartyBlkCnt4BlkID(BlkId, AblocksCnt);
                                        
                                        if(verbose)
                                            System.out.println("................. Processing Var "+BlocksOrderIdx[BlkVar] +" Block (" + BlkId + "), ARecs=" + ARecsCntInBlk + ", BRecs=" + BRecsCntInBlk);
                                         else
                                             System.out.print("Blk:"+BlocksOrderIdx[BlkVar]+"."+batchId+"."+BlkId+"["+ABSegId +"]"+", ");
                                        // skip if both of the data sets do not have data belong to this blk Block
                                        // I think I need to do the same technique in PatientLinkageGC-Blocked with Elim 
                                        if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {
                                            //1- intialize the blkDataHelper
                                            //....blkhelp=new BlkDataHelper(h_AllBlocks, BlkId,BlocksOrderIdx[BlkVar],Elimination);
                                            
                                            

                                             blkhelp=new  BlkDataHelper(batchHelper,  BlkId,  Elimination);
                                             
                                             
                                             
                                             
                                             AsegmentsCnt=1;
                                            BsegmentsCnt=1;
                                            if(ARecsCntInBlk >maxBlkSize){
                                                 AsegmentsCnt=  (int) (ARecsCntInBlk/maxBlkSize);
                                                if((ARecsCntInBlk%maxBlkSize) !=0 ) AsegmentsCnt++;
                                                }
                                                if(BRecsCntInBlk >maxBlkSize){
                                                        BsegmentsCnt=  (int) (BRecsCntInBlk/maxBlkSize);
                                                    if((BRecsCntInBlk%maxBlkSize) !=0 ) BsegmentsCnt++;  
                                                }
                                        //for(int aSeg=0;aSeg<AsegmentsCnt;aSeg++){
                                            
                                          //   for(int bSeg=0;bSeg<BsegmentsCnt;bSeg++){
                                                 
                                              segBlkhelp=new  BlkSegDataHelper(blkhelp,  bSeg,  maxBlkSize);   
                                              
                                             // BRecsCntInBlkSeg = segBlkhelp.blkSize;
                                              
                                              boolean lastSeg=(aSeg==(AsegmentsCnt-1));
                                                 //ABSegId=""+BlkId+"-"+aSeg+"x"+bSeg;
                                                 //ARecsCntInBlkSeg = segBlkhelp.blkSize;
                                                 if(AsegmentsCnt==1)  //this if is redundant, the next if is enugh
                                                   ARecsCntInBlkSeg=ARecsCntInBlk;
                                                 else{
                                                     if(lastSeg)
                                                      ARecsCntInBlkSeg=ARecsCntInBlk-aSeg*maxBlkSize; 
                                                     else
                                                        ARecsCntInBlkSeg=maxBlkSize; 
                                                         
                                                    }
                                             
                                             
                                             
                                             
                                             
                                             
                                            //2- send it to function to read block data from file
                                            //readBlockBFsDataForBlockingVar(file_data, blkhelp, h_AllBlocks);
                                            //..blkhelp = new BlkHelper(help1, BlkId, BlocksOrderIdx[BlkVar],Elimination);// BlkIndex);
                                            //Blkdata_bin = blkhelp.Blkdata_bin;

                                            //PartyB_IDs = blkhelp.IDs;
                                            //System.out.println("\n New Data Size after Elimination:"+blkhelp.Blkdata_bin.length);
                                            //Create New Job with a single task in it
                                            // Adding new job 
                                            // Adding Task
                                            tConf.party = "evaluator";
                                            //tConf.addr=getTaskAddress(); // Eval. need to set the address for this task according to A's cordinator
                                            //EvalTask eTask = new EvalTask(blkhelp, tConf, ARecsCntInBlk);

                                            // Execute 
                                            //get Results
                                            JPPFJob job = new JPPFJob();
                                            // set the job name
                                            //job.setName("J-" + BlkId);
                                            job.setName("J-" + batchId+"-"+BlkId+"-"+ ABSegId + "-" + tConf.port);
                                            tConf.TaskId="J-" + batchId+"-"+BlkId+"-"+ ABSegId + "-" + tConf.port;
                                            job.setBlocking(false);
                                            for (int i = 1; i <= enbTasks; i++) {
                                                // create a new task
                                                // add a task using an instance method as entry point
                                                EvalTask eTask = new EvalTask(segBlkhelp, tConf, ARecsCntInBlkSeg);//ARecsCntInBlk);

                                                job.add(eTask).setId("T-" + BlkId);
                                                //Task<?> BlkTask = job.add("run", gTask);

                                            }
                                             
                                            //taskBlkResults = eTask.run();
                                            //job.addJobListener(new MyJobListener(AllBlksResults,countDown,jobMBean) );
                                            job.addJobListener(new MyJobListener(portUsed,AllBlksResults, countDown));//,currentBatchesBlkCounts));

                                            // submit the job
                                            ejppfClient.submitJob(job);
                                            
                                            // I need to figureout how to use this to wait for used port to be freed here in the eval
                                            // now it is useless, just to conform with the task listener
                                            portUsed[tConf.port-port0]=true;
                                            
                                            if (verbose)
                                                System.out.println("\n Block " + job.getName() + " submitted on port:" + tConf.port);
                                             
                                            
                                            
                                            
                                        } // If ARecsCntInBlk
                                        //System.out.println("\n Testing tasks pool empty = "+EvalCoord.getNextBlockWrkrInfo()==null);
                                       currentBatches.getBatch(batchId).batchBlkJobCountDn(BlkId);
                                        // since removeBlock checks for blkJob count, we can directly call it here
                                        currentBatches.getBatch(batchId).removeBlock(BlkId);
                                        
                                        currentBatches.batchCountDn(batchId);
                                       
                                            if(currentBatches.isBatchCompleted(batchId)){
                                                currentBatches.removeBatch(batchId);
                                                System.out.println("\n Batch #"+batchId+ " Completed, Batches still in Memory :"+currentBatches.loadedBatches.size());    
                                            }
                                           int batchesInMem= currentBatches.loadedBatches.size();
                                           
                                        if(DEBUG)
                                            System.out.println("\n # Of Batches in Memory:"+currentBatches.loadedBatches.size());    
                                        //rcvdWrkInfo = EvalCoord.getNextBlockWrkrInfo();
                                        rcvdWrkInfo2 = EvalCoord.getNextBlockWrkrInfo2();
                                    } // while rcvdWrkInfo2 != null
                                    if(verbose)
                                        System.out.println("\n Eval: Dispatch completed......");
                                    if (EvalCoord.recieveNewNotifWorkersInfo2().equals("done")) {
                                        break;
                                    }
                                    
                                    
                                    
                            
                                    
                                    
                                } // while !done
                                System.out.println("\n val: No more Blocks... wait till all submitted Blocks are done");
                                
                               
                              
                                
                               // }// for batch
                                
                                // wait until all jobs completed
                                countDown.await();
                            } finally {
                                // unregister the notification listener when not needed anymore
                                if (ehandler != null) {
                                    ehandler.unregister();
                                }
                            }

                            AllBlockVarsRes[BlkVar] = AllBlksResults;

                            // we need to incorpoate El_threshold
                            if (Elimination && BlkVar != 2 && AllBlksResults != null) // not last blocking variable
                            {
                                for (int matchIds = 0; matchIds < AllBlksResults.size(); matchIds++) {
                                    if (AllBlksResults.get(matchIds).score >= El_threshold) {
                                        MatchedIDs.add(AllBlksResults.get(matchIds).BID); //at other party BID
                                    }                            //MatchedIDs_score.add(AllBlksResults.get(matchIds).score);
                                }
                                // 1-filterout matched Records by setting Matched field to True
                                h_AllBlocks.FilterOutMatchedRecsByIDs(MatchedIDs); //using Party_IDs
                               

                            }
                            BlockVarTotTimes[BlkVar] = System.currentTimeMillis() - BlockVarTotTimes[BlkVar];
                            BlockVarTotMatches[BlkVar] = AllBlockVarsRes[BlkVar].size();
                            BlockVarFP[BlkVar] = Util.CountFP(AllBlockVarsRes[BlkVar]);
                            BlockVarUniqMatches[BlkVar] = Util.CountUniqueMatches(AllBlockVarsRes[BlkVar]);

                            
                            System.out.println("\n\n =.=.=.=.=.=.=..=.=.=..=.=.=.=..=.=.=.=.=..=.=.=.=.=.=.=.=.=..=.=.=.=..=\n");
                            System.out.println("\n Eval: Blocking Var. " + BblocksCntFile[BlocksOrderIdx[BlkVar]] + " Completed..");

                        } // FOR BlkVar

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    EvalCoord.stop();

                    break;
                default:
                    throw new AssertionError();

            }  // switch party

 
            
            int TotUniqueMatches=Util.CountTotUniqueMatches(AllBlockVarsRes) ;
             OverAllFP=Util.CountTotUniqueFP(AllBlockVarsRes);
        
            String str = "";
            process_t = System.currentTimeMillis() - process_t;
            
            
            
            int AllMatches=0;
            long AllTimes=0;
            if (useChkDci) {
                if(Elimination){
                    System.out.println(new Date()+" :\n Matched by 4Bfs using Blocking With Elimination  Threshold :="+El_threshold+"\n DC Threshold :="+(double)threshold/128.0);
                    str = (new Date())+" :\n Matched by 4Bfs using Blocking With Elimination  Threshold :="+El_threshold+"\n DC Threshold :="+(double)threshold/128.0;
                }else{
                    System.out.println(new Date()+" :\n Matched by 4Bfs using Blocking \n DC Threshold :="+(double)threshold/128.0);
                    str = (new Date())+" :\n Matched by 4Bfs using Blocking \n DC Threshold :="+(double)threshold/128.0;
                }
          for(int BlkVar=0;BlkVar<3;BlkVar++){
                    
                
                System.out.println("\n"+BlkVar+"- Using Blocking Variable"+AblocksCntFile[BlocksOrderIdx[BlkVar]]);
                str+="\n"+BlkVar+"- Using Blocking Variable"+AblocksCntFile[BlocksOrderIdx[BlkVar]];
                str += "\n----------------------------------\n";
                str += "ID A  <-->  ID B  : DC score\n";
                str += "----------------------------------\n";
                for (int i = 0; i < AllBlockVarsRes[BlkVar].size(); i++) {
                    
                    
                     System.out.println(AllBlockVarsRes[BlkVar].get(i).AID+" <-> "+AllBlockVarsRes[BlkVar].get(i).BID+" : "+AllBlockVarsRes[BlkVar].get(i).score);
                     str += String.format("%6s \t %6s :\t%3.3f\n", AllBlockVarsRes[BlkVar].get(i).AID,AllBlockVarsRes[BlkVar].get(i).BID,AllBlockVarsRes[BlkVar].get(i).score);
                        
                }
                str += "----------------------------------\n";
                str += "Matches found by this stage= "+AllBlockVarsRes[BlkVar].size()+"\n\n\n";
                str += "\nFP for this BlockVar= "+BlockVarFP[BlkVar];
                str += "\nUnique Matches = "+BlockVarUniqMatches[BlkVar];
                str += "\n Time taken By This BlockVar= "+BlockVarTotTimes[BlkVar]/1e3+ " Secs";
                AllTimes+=BlockVarTotTimes[BlkVar];
                AllMatches+=AllBlockVarsRes[BlkVar].size();
                
             }
                str += "\nTotal Matches found = "+AllMatches;
                str += "\nTotal Time = "+process_t/1e3 +" seconds";
                str += "\nTotal by all BlkVars Time = "+AllTimes/1e3 +" seconds";
                str += "\nTotal Unique Matches Found= "+TotUniqueMatches;
                str += "\nOver All Unique FPs="+OverAllFP;
                System.out.println("\n--------------------------------------------\n "+"Matches found = "+AllMatches);
                System.out.println("Total Time = "+process_t/1e3 +" seconds");
                if (results_save_path != null) {
                    try (FileWriter writer = new FileWriter(results_save_path,true)) {
                        writer.write(str);
                        writer.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            
            
            } //chkDCi
            else if (!useCombBF) {

                if (res != null) {
                    str += (new Date()) + " :\n ----------------------------------\n";
//                    for (int m = 0; m < help1.rules.length; m++) {
//                        str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
//                    } 
                   str += String.format("\nThe threshold is %d\n", threshold);
                    str += "----------------------------------\n";
                    str += "linkage " + "\t\t\tscore\n";
                    str += "ID A(index)  ID B(index)\n\n";
                    for (int n = 0; n < res.size(); n++) {
                        int[] link0 = res.get(n).getLinkage();
                        str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], res.get(n).getScore());
                    }
                    str += String.format("\nThe number of matches records: %d\n", res.size());
                    str += "-----------------------------------\n";
                }
                System.out.println(str);

                if (results_save_path != null) {
                    try (FileWriter writer = new FileWriter(results_save_path, true)) {
                        writer.write(str);
                        writer.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else { //useCombBFs
                for (int i = 0; i < AllBFsRes.length; i++) {

                    str = (new Date()) + " :\n Matched by BF# " + i + "\n";
                    if (AllBFsRes[i] != null) {
                        str += "----------------------------------\n";
//                        for (int m = 0; m < help1.rules.length; m++) {
//                            str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
//                        }
                        str += String.format("\nThe threshold is %d\n", threshold);
                        str += "----------------------------------\n";
                        str += "linkage " + "\t\t\tscore\n";
                        str += "ID A(index)  ID B(index)\n\n";
                        for (int n = 0; n < AllBFsRes[i].size(); n++) {
                            int[] link0 = AllBFsRes[i].get(n).getLinkage();
                            str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], AllBFsRes[i].get(n).getScore());
                        }
                        str += String.format("\nThe number of matches records: %d\n", AllBFsRes[i].size());
                        str += "-----------------------------------\n";
                    }
                    System.out.println(str);

                    if (results_save_path != null) {
                        try (FileWriter writer = new FileWriter(results_save_path, true)) {
                            writer.write(str);
                            writer.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }//for
            } //else useChkDci
        } // if useBFs
        else {
             Helper help1;
             
            array_int1 = new int[prop_array.size()][];
            for (int i = 0; i < array_int1.length; i++) {
                array_int1[i] = prop_array.get(i);
            }

            help1 = Util.readAndEncodeWithProps(file_data, array_int1);
            data_bin = help1.data_bin;

            ws_bin = new boolean[ws.size()][];
            for (int i = 0; i < ws_bin.length; i++) {
                ws_bin[i] = fromInt(ws.get(i), data_len);
            }

            boolean[] threshold_bin = fromInt(threshold, data_len);
            CompPool.MaxNumberTask = threads;

            switch (party) {
                case "generator":
                    PartyA_IDs = help1.IDs;
                    if (filter) {
                        System.out.println("start filtering linkage ...");
                        long t0 = System.currentTimeMillis();
                        boolean[][][] f_data_bin = Util.readAndEncode(file_data, array_int1, 3);
                        Gen<GCSignal> gen = new Gen<>(port, Mode.REAL, threads, f_data_bin, records);
                        gen.implement();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of filtering is " + t1 / 1e3 + " seconds!");

                        t0 = System.currentTimeMillis();
                        f_data_bin = Util.extractArray(data_bin, gen.getRes(), party);
                        GenWssF gen_f = new GenWssF(port, Mode.REAL, threads, f_data_bin, ws_bin, threshold_bin, gen.getRes(), PartyA_IDs);
                        System.out.println("start patientlinkage algorithm ...");
                        gen_f.implement();
                        t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        res = gen_f.getLinkage();
                        PartyB_IDs = gen_f.getPartyB_IDs();

                    } else {
                        System.out.println("start patientlinkage algorithm ...");
                        long t0 = System.currentTimeMillis();
                        GenWss<GCSignal> gen = new GenWss<>(port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyA_IDs);
                        gen.implement();
                        res = gen.getLinkage();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        PartyB_IDs = gen.getPartyB_IDs();
                    }
                    break;
                case "evaluator":
                    PartyB_IDs = help1.IDs;
                    if (filter) {
                        System.out.println("start filtering linkage ...");
                        long t0 = System.currentTimeMillis();
                        boolean[][][] f_data_bin = Util.readAndEncode(file_data, array_int1, 3);
                        Env<GCSignal> eva = new Env<>(addr, port, Mode.REAL, threads, f_data_bin, records);
                        eva.implement();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of filtering is " + t1 / 1e3 + " seconds!");

                        t0 = System.currentTimeMillis();
                        f_data_bin = Util.extractArray(data_bin, eva.getRes(), party);
                        EnvWssF eva_f = new EnvWssF(addr, port, Mode.REAL, threads, f_data_bin, ws_bin, threshold_bin, eva.getRes(), PartyB_IDs);
                        System.out.println("start patientlinkage algorithm ...");
                        eva_f.implement();
                        t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        res = eva_f.getLinkage();
                        PartyA_IDs = eva_f.getPartyA_IDs();
                    } else {
                        System.out.println("start patientlinkage algorithm ...");
                        long t0 = System.currentTimeMillis();
                        EnvWss<GCSignal> eva = new EnvWss<>(addr, port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyB_IDs);
                        eva.implement();
                        res = eva.getLinkage();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        PartyA_IDs = eva.getPartyA_IDs();
                    }
                    break;
                default:
                    throw new AssertionError();
            }

            String str = "";

            if (res != null) {
                str += (new Date()) + " :\n ----------------------------------\n";
                for (int m = 0; m < help1.rules.length; m++) {
                    str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                }
                str += String.format("\nThe threshold is %d\n", threshold);
                str += "----------------------------------\n";
                str += "linkage " + "\t\t\tscore\n";
                str += "ID A(index)  ID B(index)\n\n";
                for (int n = 0; n < res.size(); n++) {
                    int[] link0 = res.get(n).getLinkage();
                    str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], res.get(n).getScore());
                }
                str += String.format("\nThe number of matches records: %d\n", res.size());
                str += "-----------------------------------\n";
            }
            System.out.println(str);

            if (results_save_path != null) {
                try (FileWriter writer = new FileWriter(results_save_path)) {
                    writer.write(str);
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } //!useBFs
    }

    public static void usagemain() {
        String help_str
                = ""
                + String.format("     -config     <path>      : input configure file path\n")
                + String.format("     -data       <path>      : input data file path\n")
                + String.format("     -help                   : show help");
        System.out.println(help_str);
    }

    public static void simulation() {
        String[] args0 = {"-config", "./configs/config_gen_1K.txt", "-data", "./data/Source14k_a_1K.csv"};
        String[] args1 = {"-config", "./configs/config_eva_1K.txt", "-data", "./data/Source14k_b_1K.csv"};

        Thread t_gen = new Thread(() -> {
            startLinkage(args0);
        });
        Thread t_eva = new Thread(() -> {
            startLinkage(args1);
        });

        long t0 = System.currentTimeMillis();
        try {
            t_gen.start();
            t_eva.start();
            t_gen.join();
            t_eva.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        long t1 = System.currentTimeMillis() - t0;

        System.out.println("The total running time is " + t1 / 1e3 + " seconds!");
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        if ("sim".equals(args[0])) {
            simulation();
        } else {
            startLinkage(args);
        }
        long t1 = System.currentTimeMillis() - t0;
        System.out.println("The total running time is " + t1 / 1e3 + " seconds!");
    }

    public static boolean[][] UpdateChkDCi(boolean[][] chkDCi, ArrayList<PatientLinkage> tres) {
        int ind, Otherindx;
        for (int n = 0; n < tres.size(); n++) {
            ind = tres.get(n).getI();
            Otherindx = tres.get(n).getJ();
            chkDCi[ind][Otherindx] = false;
        }
        return chkDCi;
    }
    // add party to change ind to J instead of I

    public static boolean[][] UpdateChkPartDCi(boolean[][] chkDCi, ArrayList<PatientLinkage2> totPartRes,
            int[][] numOf1sInParts, int Part, int DCt, String role) { //Part starts from 1
        int ind = 0, Otherindx = 0, myInd = 0;
        int nOfParts = numOf1sInParts[0].length;
        int t1s, sh1s, tEstMaxSh1s;
        double DCtr = (double) DCt / 128.0;

        switch (role) {
            case "generator":
                for (int n = 0; n < totPartRes.size(); n++) {

                    ind = totPartRes.get(n).getI();
                    myInd = totPartRes.get(n).getI();
                    Otherindx = totPartRes.get(n).getJ();
                    sh1s = totPartRes.get(n).getShared1s();
                    t1s = totPartRes.get(n).getTotal1s();
                    tEstMaxSh1s = 0;
                    for (int i = Part; i < nOfParts; i++) {
                        tEstMaxSh1s += numOf1sInParts[myInd][i];
                    }
                    int remSh1s = (int) (DCtr * t1s) - 2 * tEstMaxSh1s;
                    if (sh1s * 2 < remSh1s) {
                        chkDCi[ind][Otherindx] = false;
                    }
                }
                break;
            case "evaluator":
                for (int n = 0; n < totPartRes.size(); n++) {
                    ind = totPartRes.get(n).getI();
                    myInd = totPartRes.get(n).getJ();
                    Otherindx = totPartRes.get(n).getJ();
                    sh1s = totPartRes.get(n).getShared1s();
                    t1s = totPartRes.get(n).getTotal1s();
                    tEstMaxSh1s = 0;
                    for (int i = Part; i < nOfParts; i++) {
                        tEstMaxSh1s += numOf1sInParts[myInd][i];
                    }
                    int remSh1s = (int) (DCtr * t1s) - 2 * tEstMaxSh1s;
                    if (sh1s * 2 < remSh1s) {
                        chkDCi[ind][Otherindx] = false;
                    }
                }
                break;
        }//case

        return chkDCi;
    }

    // I assumed tres have all linkages, where nonchecked items has score 0
    public static ArrayList<PatientLinkage2> UpdateBFPartsRes(ArrayList<PatientLinkage2> TotBFPartsRes, ArrayList<PatientLinkage2> tres) {
        int ind, Otherindx;
        PatientLinkage2 l;
        if (tres.isEmpty()) {
            return TotBFPartsRes;
        }
        if (TotBFPartsRes == null || TotBFPartsRes.isEmpty()) {
            //TotBFPartsRes=new ArrayList<>();
            TotBFPartsRes = tres;
        } else {
            for (int n = 0; n < tres.size(); n++) {

                l = TotBFPartsRes.get(n);
                assert (tres.get(n).getI() == l.getI() && tres.get(n).getJ() == l.getJ());
                l.setShared1s(l.getShared1s() + tres.get(n).getShared1s());
                l.setScore((double) l.getShared1s() * 2.0 / l.getTotal1s());
                //l.setScore(l.getScore()+tres.get(n).getScore());

                //check if score=shared1s/tot1s
                TotBFPartsRes.set(n, l);
            }
        }

        return TotBFPartsRes;
    }

    public static Properties getConfigsFromXML(String configFile) {
        Properties defaultConf = new Properties();
        // sets default properties

        defaultConf.setProperty("party", "nobody");
        defaultConf.setProperty("address", "");
        defaultConf.setProperty("threshold", "1");
        defaultConf.setProperty("threads", "1");
        defaultConf.setProperty("records", "0");

        defaultConf.setProperty("filter", "0"); //false
        defaultConf.setProperty("port", "-1");
        defaultConf.setProperty("com", "");
        defaultConf.setProperty("results_save_path", "none.txt");

        Properties configs = new Properties(defaultConf);

        // loads properties from file
        try (InputStream inputStream = new FileInputStream(configFile)) {
            configs.loadFromXML(inputStream);

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (configs);
    }

    public static class TaskListener implements NotificationListener {

        ObjectOutputStream genOs;

        public TaskListener(ObjectOutputStream Os) {
            genOs = Os;
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            // the task notification is wrapped into a JPPFNodeForwardingNotification, let's unwrap it
            //System.out.printf("\n Data object sent in notification : "+handback);
            JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
            TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
            // if user-defined notification
            if (actualNotif.isUserNotification()) {
                Object o = actualNotif.getUserData();
                TaskInformation info = actualNotif.getTaskInformation();
                // do something with the user object ...
                if (verbose)
                   System.out.printf("\n Data in notification sent: " + o);
                //System.out.printf("\n TaskInfo: Taskid= %s , JobId=%s , JobName=%s , Pos=%s",info.getId(),info.getId(),info.getJobId(),info.getJobName(),info.getJobPosition());
                //System.out.printf("\n Node Info: "+actualNotif.getNodeInfo()

                try {

                    genOs.writeObject(o);
                    genOs.flush();
                    if (verbose)
                      System.out.printf("\n Data in notification written to Os");
                    // Files.write(Paths.get("IPs.txt"), ("Node sent this :"+  lhost+"\n").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    //exception handling
                }

            }
        }

    }

    // results will be processed asynchronously within
    // the job listener's jobEnded() notifications
    public static class MyJobListener implements JobListener {

        ArrayList<PatientLinkageResults> JobAllBlksResults;
        final CountDownLatch JobsCountDown;
        //ArrayList<int[]> currentBatchesBlkCounts;
        boolean[] portUsed;
       // final CountDownLatch batchJobsCountDown;
        //--DriverJobManagementMBean  MyjMB;

        public MyJobListener(boolean[] portUsd,ArrayList<PatientLinkageResults> AllBlksResults, CountDownLatch countDown){//,ArrayList<int[]> cBatchCnts//,CountDownLatch batchCountDown) {//,DriverJobManagementMBean jMB ) {
            JobAllBlksResults = AllBlksResults;
            JobsCountDown = countDown;
            portUsed=portUsd;
           // currentBatchesBlkCounts=cBatchCnts;
           // batchJobsCountDown = batchCountDown;
            //-- MyjMB = jMB;
        }

        @Override
        public synchronized void jobEnded(final JobEvent event) {
            // ... process the job results ...
            //job.setName("J-" + batch+"-"+ ABSegId + "-" + taskPort); 
            // ABSegId=""+BlkId+"-"+aSeg+"x"+bSeg;
            List<Task<?>> results = (event.getJob()).getAllResults();
            String jName=(event.getJob()).getName();
            String[] tmp=jName.split("-");
            int jPort=Integer.parseInt(tmp[4]); // task port is the 5th part of jName (j-bbb-kkk-assxbss-port)
            for (Task<?> task : results) {
                if (task.getThrowable() != null) { // if the task execution raised an exception
                    System.out.printf("%s raised an exception : %s%n", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
                } else { // otherwise display the task result
                    //taskBlkResults=(ArrayList<PatientLinkageResults>)task.getResult();
                    if (verbose)
                        System.out.printf("\n JOB Ended: Results are ");
                    for (PatientLinkageResults tr : (ArrayList<PatientLinkageResults>) task.getResult()) {
                        if (verbose)
                            System.out.printf("\n " + tr.AID + "--" + tr.BID +"   -> "+tr.score);
                        JobAllBlksResults.add(tr);
                    }
                    //System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
                }
            }
            JobsCountDown.countDown();
            /*int batchId=Integer.parseInt(tmp[1]); //batch Id is the 2nd part of jName (j-bbb-kkk-assxbss-port)
            int blkId= Integer.parseInt(tmp[2]);
            int l=-1,cnt=0;
            for(int i=0;i<currentBatchesBlkCounts.size();i++)
            {
                int[] batch=currentBatchesBlkCounts.get(i);
                if(batch[0]==batchId && batch[1]==blkId)
                {
                    l=i;
                    cnt=batch[2];
                }
            }   
               // if(l!=-1)
               cnt-=1;
             currentBatchesBlkCounts.remove(l);
             int[] e={batchId,blkId,cnt};
             currentBatchesBlkCounts.add(e);*/
            portUsed[jPort-port0]=false;
            //batchJobsCountDown.countDown();
        }

        @Override
        public void jobStarted(final JobEvent event) {
        }

        @Override
        public void jobDispatched(final JobEvent event) {
            // System.out.println("\n Job Dispatched");
            /*   String jobid= event.getJob().getUuid();
                try {
                   
                   NodeJobInformation[] nodeJobInfo= MyjMB.getNodeInformation(jobid );
                   if(nodeJobInfo.length>0){
                   JPPFManagementInfo nodeInfo = nodeJobInfo[0].getNodeInfo();
                   String nodeAddr = nodeInfo.getIpAddress();
                   System.out.println("\n Job sent to node on Ip:"+nodeAddr);
                   }
                           
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
             */
        }

        @Override
        public void jobReturned(final JobEvent event) {
        }

    }

}
