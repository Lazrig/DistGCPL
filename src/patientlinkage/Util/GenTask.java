/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.Util;

import flexsc.Mode;
import gc.GCSignal;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import patientlinkage.DataType.BFHelper;
import patientlinkage.DataType.BlkHelper;
import patientlinkage.DataType.PatientLinkage2;
import patientlinkage.DataType.PatientLinkageResults;
import patientlinkage.DataType.TaskConfig;
import static patientlinkage.Util.Main.UpdateBFPartsRes;
import static patientlinkage.Util.Main.UpdateChkPartDCi;
import patientlinkage.parties.GenWssBFsWithCLR1scntAndChkDCi_Vpart;




import org.jppf.node.protocol.AbstractTask;
import patientlinkage.DataType.BlkDataHelper;
import patientlinkage.DataType.BlkSegDataHelper;
import patientlinkage.DataType.DispatchWorkerInfo;
import static patientlinkage.Util.Main.DEBUG;
       

/**
 *
 * @author ibrahim
 */
//------------------------------------------------------------------------------------------------
public class GenTask extends AbstractTask<ArrayList<PatientLinkageResults>> implements Serializable{

    public BlkSegDataHelper blkhelp;

    public TaskConfig conf;
    public int BRecsCntInBlk;

    public GenTask(BlkSegDataHelper blkhelp, TaskConfig conf, int BRecsCntInBlk) {
        this.conf = conf;
        this.blkhelp = blkhelp;
        this.BRecsCntInBlk = BRecsCntInBlk;

    }

    //public ArrayList<PatientLinkageResults> 
      @Override public void   run() {
        boolean[][] chkPartDCi;
        boolean[][] chkDCi;

        //ArrayList<PatientLinkage> res = null;
        //ArrayList<PatientLinkage> tres = null;
        ArrayList<PatientLinkage2> tres2 = null;
        ArrayList<PatientLinkage2> TotBFPartsRes = new ArrayList<>();
        ArrayList<PatientLinkage2>[] AllBfsTotBFPartsRes = null;
        ArrayList<PatientLinkageResults> taskBlkResults = new ArrayList<>();
        ArrayList<String> PartyA_IDs = null;
        ArrayList<String> PartyB_IDs = new ArrayList<>();

        
        InetAddress lhost=null;
        // fire notification with info about the worker node
        //InetAddress IP=InetAddress.getLocalHost();
        //System.out.println("IP of my system is := "+IP.getHostAddress());
        try{
        lhost = InetAddress.getLocalHost(); 
        if(DEBUG)
            System.out.println("\n inside task: lhost="+lhost.getHostAddress());
        //DispatchWorkerInfo dispWrkrInfo=new DispatchWorkerInfo(blkhelp.BlockID, ""+lhost.getHostAddress(), conf.port);
       
        //fireNotification(dispWrkrInfo , true);
        } catch (Exception e) {
            setThrowable(e);
        }
        
        int port = conf.port;
        String party = "generator"; // conf.party;

        int threads = conf.threads, hTasks = conf.hTasks, vTasks = conf.vTasks;
        int threshold = conf.threshold;
        int numOfUsedBFs = blkhelp.Blkdata_bin[0].length; // need to verify
        int ARecsCntInBlk = blkhelp.Blkdata_bin.length;
        // this what we need to return
        AllBfsTotBFPartsRes = (ArrayList<PatientLinkage2>[]) new ArrayList[numOfUsedBFs];
        chkPartDCi = new boolean[ARecsCntInBlk][BRecsCntInBlk];
        chkDCi = new boolean[ARecsCntInBlk][BRecsCntInBlk];

        for (int ci = 0; ci < ARecsCntInBlk; ci++) {
            for (int cj = 0; cj < BRecsCntInBlk; cj++) {
                chkPartDCi[ci][cj] = true;
                chkDCi[ci][cj] = true;

            }
        }

        PartyA_IDs = blkhelp.IDs;

        if (conf.useChkDci) {
            if(DEBUG){
            System.out.println((new Date()) + " :\n Gen.((V3)) starts with:\n -Partitioned Bfs \n-Vpart \n -filtring using chk DC for those matched by any BF ...");
            System.out.println("\n\n ==========> ... Now Processing:"+conf.TaskId);
            }
            else
               System.out.print("\nTask: "+conf.TaskId); 
               //System.out.print("\nBlk:"+blkhelp.BlockID); 
            
            if (conf.PassNumOfOnes) {
                boolean[][][] numOf1s;
                int[] intNumOf1s;
                numOf1s = blkhelp.BlknumOfOnesInBFs;
                //numOf1s = help1.numOfOnesInBFs;

                //System.out.println(" patientlinkage With BF #1s passed in the Clr...");
                boolean[][][] BFpartData;
                //BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                BFHelper BFHlp1 = new BFHelper(blkhelp.Blkdata_bin, numOf1s, BRecsCntInBlk);
                BFHlp1.setnumOfParts(conf.numOfParts);
                //int BRecsToProcess = records;
                int BRecsToProcess = BRecsCntInBlk;
                //int ArecsToProcess = data_bin.length; 
                long t0 = 0, t1 = 0, tot_t = 0;
                
                
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

                    int[][] AllParts1sCnt = BFHlp1.getBFAllPartitions1sCnt(i); //#1s in each partition
                    //numOf1s = BFHlp1.getBF1sCnt(i);
                    //intNumOf1s=help1.getIntBF1sCnt(i);
                    intNumOf1s = blkhelp.getIntBF1sCnt(i);
                    if (TotBFPartsRes != null) {
                        TotBFPartsRes.clear();
                    }
                    
                    int port0=port;
                    int TotnParts = BFHlp1.getnumOfParts();
                    
                    for (int Part = 1; Part <= TotnParts; Part++) {
                        if(DEBUG)
                            System.out.println("\nStart processing BF part# " + Part + " out of " + TotnParts);
                        else
                            System.out.print("-" + Part + "/" + TotnParts);
                        t1 = System.currentTimeMillis();
                        BFpartData = BFHlp1.getRecsBFPartition(i, Part);
                        

                        if (Part % 2 == 0) {
                            port0=port+1;//+2;
                        } else {
                            port0=port;
                        }
                        if(DEBUG)
                            System.out.println(" \n On Address:Port="+lhost.getHostAddress()+":"+port0);
                        //GenWssBFsWith1scntAndChkDCi<GCSignal> gen = new GenWssBFsWith1scntAndChkDCi<>(port, Mode.REAL, threads, BFpartData, numOf1s, ws_bin, threshold_bin, chkPartDCi, BRecsToProcess, PartyA_IDs);
                        GenWssBFsWithCLR1scntAndChkDCi_Vpart<GCSignal> gen = new GenWssBFsWithCLR1scntAndChkDCi_Vpart<>(lhost.getHostAddress(),port0, Mode.REAL, threads, hTasks, vTasks, BFpartData, intNumOf1s, chkPartDCi, BRecsToProcess, PartyA_IDs);
                        try{
                        gen.implement(Part + i); // to pass the IDs when processing the first part of the 1st BF only
                        } catch(Exception ex) {
                            setThrowable(ex);
                            setResult( null);
                                }
                        tres2 = gen.getLinkage();
                        
                        
                        //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                        if (tres2 != null) {

                            TotBFPartsRes = UpdateBFPartsRes(TotBFPartsRes, tres2);
                            chkPartDCi = UpdateChkPartDCi(chkPartDCi, TotBFPartsRes, AllParts1sCnt, Part, threshold, party);
                        }
                        if (Part == 1 && i == 0) // to pass the IDs when processing the first part of the 1st BF only
                        {
                            PartyB_IDs = gen.getPartyB_IDs();
                        }

                        //System.out.println("BF part# "+Part +" Completed");
                        t1 = System.currentTimeMillis() - t1;
                        //System.out.println("The running time of part# " + Part + " is " + t1 / 1e3 + " seconds!");
                    
                    
                    //port0+=threads+1;
                    } //for part
                    if (AllBfsTotBFPartsRes[i] == null) {
                        AllBfsTotBFPartsRes[i] = new ArrayList<>();

                        //gen.getLinkage();
                        //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                    } //else {

                    for (int n = 0; n < TotBFPartsRes.size(); n++) {
                        PatientLinkage2 l = TotBFPartsRes.get(n);
                        if (l.getScore() >= (double) threshold / 128.0) {
                            if (conf.useBfsInSeq) {
                                int indi = l.getI();
                                int indj = l.getJ();
                                chkDCi[indi][indj] = false;
                            }

                            AllBfsTotBFPartsRes[i].add(l);
                        }
                    }

                    if (conf.useBfsInSeq) {
                        int p = 0;
                        for (boolean[] pDCi : chkDCi) {
                            System.arraycopy(pDCi, 0, chkPartDCi[p++], 0, pDCi.length);
                        }
                    }

                    // chkPartDCi=chkDCi;
                    //}
                    //TODO: add check DCi for the entire filter
                    //chkDCi= UpdateChkDCi(chkDCi,AllBfsTotBFPartsRes[i]);
                    t1 = System.currentTimeMillis() - t0;
                    tot_t += t1;
                    //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                    //System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");

                    //PartyB_IDs.addAll(B_IDs);
                    //BFData= BFHlp1.getData_bin(i);
                    //ArecsToProcess = BFHlp1.getDataSize();
                    System.out.println("Round (" + i + ") found " + AllBfsTotBFPartsRes[i].size() + " Matches");

                } //for i
                System.out.println("\nTotal time: " + tot_t / 1e3 + " sec!");

                for (int usedBFidx = 0; usedBFidx < AllBfsTotBFPartsRes.length; usedBFidx++) {
                    for (int n = 0; n < AllBfsTotBFPartsRes[usedBFidx].size(); n++) {
                        int[] link0 = AllBfsTotBFPartsRes[usedBFidx].get(n).getLinkage();
                         //System.out.println(link0[0]+"---"+ link0[1]);
                         String aID=PartyA_IDs.get(link0[0]);
                         String bID=PartyB_IDs.get(link0[1]);
                         double score=AllBfsTotBFPartsRes[usedBFidx].get(n).getScore();
                         //System.out.println(aID +"---"+ bID + " : "+ score);
                         PatientLinkageResults tr=new PatientLinkageResults(aID,bID ,score );
                        taskBlkResults.add(tr);
                    }
                }

            } //if PassNumOfOnes

        }// if UseChckDCi

        setResult( taskBlkResults);
    }

} //class GenTask 
