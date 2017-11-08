/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.Util;

import flexsc.Mode;
import gc.GCSignal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import patientlinkage.DataType.BFHelper;
import patientlinkage.DataType.BlkHelper;
import patientlinkage.DataType.PatientLinkage2;
import patientlinkage.DataType.PatientLinkageResults;
import patientlinkage.DataType.TaskConfig;
import static patientlinkage.Util.Main.UpdateBFPartsRes;
import static patientlinkage.Util.Main.UpdateChkPartDCi;
import patientlinkage.parties.EnvWssBFsWithCLR1scntAndChkDCi_Vpart;



import org.jppf.node.protocol.AbstractTask;
import patientlinkage.DataType.BlkDataHelper;
import patientlinkage.DataType.BlkSegDataHelper;
import static patientlinkage.Util.Main.DEBUG;
  

/**
 *
 * @author ibrahim
 */ //------------------------------------------------------------------------------------------------
class EvalTask extends AbstractTask<ArrayList<PatientLinkageResults>> implements Serializable{

    public BlkSegDataHelper blkhelp;

    public TaskConfig conf;
    public int ARecsCntInBlk;

    public EvalTask(BlkSegDataHelper blkhelp, TaskConfig conf, int ARecsCntInBlk) {
        this.conf = conf;
        this.blkhelp = blkhelp;
        this.ARecsCntInBlk = ARecsCntInBlk;

    }

    //public ArrayList<PatientLinkageResults> 
    @Override public void run() {
        boolean[][] chkPartDCi;
        boolean[][] chkDCi;

        //ArrayList<PatientLinkage> res = null;
        //ArrayList<PatientLinkage> tres = null;
        ArrayList<PatientLinkage2> tres2 = null;
        ArrayList<PatientLinkage2> TotBFPartsRes = new ArrayList<>();// null;
        ArrayList<PatientLinkage2>[] AllBfsTotBFPartsRes = null;
        ArrayList<PatientLinkageResults> taskBlkResults = new ArrayList<>();
        ArrayList<String> PartyA_IDs = new ArrayList<>();
        ArrayList<String> PartyB_IDs = null;

        int port = conf.port;
        String addr = conf.addr;
        String party = "evaluator"; // conf.party;

        int threads = conf.threads, hTasks = conf.hTasks, vTasks = conf.vTasks;
        int threshold = conf.threshold;
        int numOfUsedBFs = blkhelp.Blkdata_bin[0].length; // need to verify
        int BRecsCntInBlk = blkhelp.Blkdata_bin.length;
        // this what we need to return
        AllBfsTotBFPartsRes = (ArrayList<PatientLinkage2>[]) new ArrayList[numOfUsedBFs];
        chkPartDCi = new boolean[ARecsCntInBlk][BRecsCntInBlk];
        chkDCi = new boolean[ARecsCntInBlk][BRecsCntInBlk];

        
        if(DEBUG)
            System.out.print("\nTask: "+conf.TaskId+" Gen Addr :"+addr);
            //System.out.println("\n inside Eval Task: BlkId="+blkhelp.BlockID+" Gen Addr:port="+addr+":"+port);
        else
            System.out.print("\nTask: "+conf.TaskId);
            //System.out.print("\n Blk:"+blkhelp.BlockID);
        //PartyB_IDs = help1.IDs;
        AllBfsTotBFPartsRes = (ArrayList<PatientLinkage2>[]) new ArrayList[numOfUsedBFs];

       
        for (int ci = 0; ci < ARecsCntInBlk; ci++) {
            for (int cj = 0; cj < BRecsCntInBlk; cj++) {
                chkPartDCi[ci][cj] = true;
                chkDCi[ci][cj] = true;

            }
        }

        
        PartyB_IDs = blkhelp.IDs;
        if (conf.useChkDci) {
            if(DEBUG)
            System.out.println((new Date()) + " :\n Eval.((V3)) starts patientlinkage algorithm with:\n -Partitioned Bfs \n-Vpartitioning \n -filtring using chk DC for those matched by any filter ...");

            //System.out.println("Eval. starts patientlinkage algorithm with filtring using chk DC for those matched by any filter ...");
            //t0 = System.currentTimeMillis();
            if (conf.PassNumOfOnes) {
                boolean[][][] numOf1s;
                //numOf1s = help1.numOfOnesInBFs;
                numOf1s = blkhelp.BlknumOfOnesInBFs;
                int[] intNumOf1s;
                // System.out.println(" patientlinkage With BF #1s passedin the Clr...");

                boolean[][][] BFpartData;
                //BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                BFHelper BFHlp1 = new BFHelper(blkhelp.Blkdata_bin, numOf1s, ARecsCntInBlk);
                BFHlp1.setnumOfParts(conf.numOfParts);
                //int BRecsToProcess = data_bin.length;
                int ARecsToProcess = ARecsCntInBlk;
                long t0 = 0, t1 = 0;
                
                
                
                for (int i = 0; i < numOfUsedBFs; i++) {
                    if(DEBUG)
                        System.out.println("\n now starting processing BF#"+(i+1));
                    else
                        System.out.print(".. BF#:"+(i+1));  
                    if (!conf.useBfsInSeq) {
                        for (int ci = 0; ci < ARecsCntInBlk; ci++) {
                            for (int cj = 0; cj < BRecsCntInBlk; cj++) {
                                chkPartDCi[ci][cj] = true;
                            }
                        }
                    }

                    t0 = System.currentTimeMillis();

                    int[][] AllParts1sCnt = BFHlp1.getBFAllPartitions1sCnt(i);
                    //numOf1s = BFHlp1.getBF1sCnt(i);
                    intNumOf1s = blkhelp.getIntBF1sCnt(i);

                    if (TotBFPartsRes != null) {
                        TotBFPartsRes.clear();
                    }
                    
                    
                    int port0=port;
                    int TotnParts = BFHlp1.getnumOfParts();
                    
                    for (int Part = 1; Part <= TotnParts; Part++) {
                        BFpartData = BFHlp1.getRecsBFPartition(i, Part);
                        System.out.print("-" + Part + "/" + TotnParts);
                         

                       if (Part % 2 == 0) {
                            port0=port+1;//+2;
                        } else {
                            port0=port;
                        }
                        
                        if(DEBUG)
                            System.out.println(" \n On Address:Port="+addr+":"+port0);
                        //EnvWssBFsWith1scntAndChkDCi<GCSignal> env = new EnvWssBFsWith1scntAndChkDCi<>(addr, port, Mode.REAL, threads, BFpartData, numOf1s, ws_bin, threshold_bin, chkPartDCi, ARecsToProcess, PartyB_IDs);
                        EnvWssBFsWithCLR1scntAndChkDCi_Vpart<GCSignal> env = new EnvWssBFsWithCLR1scntAndChkDCi_Vpart<>(addr, port0, Mode.REAL, threads, hTasks, vTasks, BFpartData, intNumOf1s, chkPartDCi, ARecsToProcess, PartyB_IDs);
                        
                        
                        try{
                        env.implement(Part + i); // to pass the IDs when processing the first part of the 1st BF only
                        } catch(Exception ex) {
                            setThrowable(ex);
                            setResult( null);
                                }
                        
                        
                        
                        tres2 = env.getLinkage();
                        
                        //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                        // get the linkages based on the org. indexes
                        if (tres2 != null) {
                            //System.out.println("EVA Tres2 size=" + tres2.size());
                            TotBFPartsRes = UpdateBFPartsRes(TotBFPartsRes, tres2);
                            chkPartDCi = UpdateChkPartDCi(chkPartDCi, TotBFPartsRes, AllParts1sCnt, Part, threshold, party);
                        }
                        if (Part == 1 && i == 0)// to pass the IDs when processing the first part of the 1st BF only
                        {
                            PartyA_IDs = env.getPartyA_IDs();
                        }
                        
                       // port0+=2;
                       // port0+=threads+1;
                    } // for part

                    if (AllBfsTotBFPartsRes[i] == null) {
                        AllBfsTotBFPartsRes[i] = new ArrayList<>();
                        //gen.getLinkage();
                        //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                    } //else {

//                                for (int ci = 0; ci < data_bin.length; ci++) {
//                                        for (int cj = 0; cj < records; cj++) {
//                                            chkPartDCi[ci][cj] = true;
//                                        }
//                                    }
                    for (int n = 0; n < TotBFPartsRes.size(); n++) {
                        PatientLinkage2 l = TotBFPartsRes.get(n);
                        // System.out.print(l.getScore()+" | ");

                        if (l.getScore() >= (double) threshold / 128.0) {
                            if (conf.useBfsInSeq) {
                                int indi = l.getI();
                                int indj = l.getJ();
                                chkDCi[indi][indj] = false;
                            }
                            AllBfsTotBFPartsRes[i].add(l);
                        }

                        //if(l.getScore()>=(double)threshold/128.0)
                        //  AllBfsTotBFPartsRes[i].add(l);
                    }
                    //System.out.println();

                    if (conf.useBfsInSeq) {
                        int p = 0;
                        for (boolean[] pDCi : chkDCi) {
                            System.arraycopy(pDCi, 0, chkPartDCi[p++], 0, pDCi.length);
                        }
                    }
                    //chkPartDCi=chkDCi;
                    //}
                    //chkDCi= UpdateChkDCi(chkDCi,AllBfsTotBFPartsRes[i]);
                    t1 = System.currentTimeMillis() - t0;

                    //System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                    //if (i == 0) {
                    //PartyA_IDs = env.getPartyA_IDs();
                    //}
                    //PartyB_IDs.addAll(B_IDs);
                    //BRecsToProcess = BFHlp1.getDataSize();
                    System.out.println("Round (" + i + ") found " + AllBfsTotBFPartsRes[i].size() + " Matches");
                } //for i #BFs

                for (int usedBFidx = 0; usedBFidx < AllBfsTotBFPartsRes.length; usedBFidx++) {
                    for (int n = 0; n < AllBfsTotBFPartsRes[usedBFidx].size(); n++) {
                        int[] link0 = AllBfsTotBFPartsRes[usedBFidx].get(n).getLinkage();

                        taskBlkResults.add(new PatientLinkageResults(PartyA_IDs.get(link0[0]), PartyB_IDs.get(link0[1]), AllBfsTotBFPartsRes[usedBFidx].get(n).getScore()));
                    }
                }

            }
        }// useChkDDi
        //return (taskBlkResults);
        setResult( taskBlkResults);
    }

} //class EvalTask 

