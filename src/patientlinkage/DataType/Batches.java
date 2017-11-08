/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;


/**
 *
 * @author ibrahim
 */

import java.util.ArrayList;
import patientlinkage.Util.Util;

public class Batches {
    //public  int batchSize;
    //public int nLoadedBatches;
    public  int maxRecsInBatch;
    public  int maxBlkSize;
    public String file_data;
    int BlkVar;
    boolean Elimination;
    public  ArrayList<int[]> ABblocksCnt = new ArrayList();
                                                               // remove batch when BlkCountDn=0
    public  ArrayList<LoadedBatch> loadedBatches =new ArrayList<>();
    public BlocksHelper AllBlocksHelper;
    public int IdLoc;
    public int[] BlksLoc;
    public int[]  BFDataLoc;
    public Batches(int maxBlkSze,int maxBatchRecs,ArrayList<int[]> ABblksCnt, String file, int blockVar, 
                       boolean Elim, BlocksHelper AllBlocksHlpr, int IdLoc, int[] BlksLoc,int[] BFDataLoc ){
       //batchSize=bSize ;
       maxRecsInBatch=maxBatchRecs;
       maxBlkSize =  maxBlkSze;
       ABblocksCnt=ABblksCnt;
       file_data=file;
       BlkVar=blockVar;
        Elimination=Elim;
        AllBlocksHelper=AllBlocksHlpr;  // for finding matched Ids
        this.IdLoc=IdLoc;
        this.BlksLoc=BlksLoc;
        this.BFDataLoc=BFDataLoc;
       
    }
    
    public void loadBatch(int[] batchRange ){
        
        int fromIdx=batchRange[1];//    id*batchSize;
        int toIndx= batchRange[2];
        int batchSize = toIndx-fromIdx+1;
        int id = batchRange[0]; //batch id
        ArrayList<int[]> batchABblocksCnt;// =  new ArrayList<>();
        ArrayList<int[]> batchABblocksJobCnt = new ArrayList<>();
        int JobCount=0,BlkJobCnt=0;
        
        batchABblocksCnt=Util.getABBlocks4Range(ABblocksCnt,fromIdx,batchSize);
        
        for (int blk = 0; blk < batchABblocksCnt.size(); blk++) {
               int[] ABblk = batchABblocksCnt.get(blk);
               
                  BlkJobCnt=getNumOfJobsInSegmBlock(ABblk,maxBlkSize);
                  
                  int[] e ={id,ABblk[0],ABblk[1],ABblk[2],BlkJobCnt};
                  batchABblocksJobCnt.add(e);
                  JobCount+=BlkJobCnt;
               
            }
                                
     
       BatchDataHelper batchHelper=new BatchDataHelper(BlkVar,Elimination);
       Util.readBatchBFsDataForBlocksRange(file_data, batchHelper,batchABblocksCnt,AllBlocksHelper,IdLoc,BlksLoc,BFDataLoc);
       
       LoadedBatch newBatch= new LoadedBatch(id,JobCount, batchABblocksJobCnt,  batchHelper);
       loadedBatches.add(newBatch);
       
       
    }
    
public int getNumOfJobsInSegmBlock(int[] ABblk,int mxBlkSz){

        int BlkId = ABblk[0];
        int AsegmentsCnt = 1;
        int BsegmentsCnt = 1;
        int ARecsCntInBlk = ABblk[1];
        int BRecsCntInBlk = ABblk[2];
        //if (ARecsCntInBlk > 0 && BRecsCntInBlk > 0) {

            if (ARecsCntInBlk > mxBlkSz) {
                AsegmentsCnt = (int) (ARecsCntInBlk / mxBlkSz);
                if ((ARecsCntInBlk % mxBlkSz) != 0) {
                    AsegmentsCnt++;
                }
            }
            if (BRecsCntInBlk > mxBlkSz) {
                BsegmentsCnt = (int) (BRecsCntInBlk / mxBlkSz);
                if ((BRecsCntInBlk % mxBlkSz) != 0) {
                    BsegmentsCnt++;
                }
            }
        //}
        return AsegmentsCnt * BsegmentsCnt;
    }
public boolean isBatchLoaded(int id){
    int lid;
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(id==lid) return true;
    }
    return false;
}



public BatchDataHelper getBatchHelper(int Id){
    int lid;
    //BatchDataHelper Batchhlp=null;
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(Id==lid) return(loadedBatches.get(i).batchDataHelper);
    }
    
    return null;//Batchhlp;
}

public LoadedBatch getBatch(int Id){
    int lid;
    //BatchDataHelper Batchhlp=null;
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(Id==lid) return(loadedBatches.get(i));
    }
    
    return null;//;
}


public ArrayList<int[]>  getBatchABblkCnt(int Id){
    int lid;
    
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(Id==lid) return  loadedBatches.get(i).batchABblocksJobCnt;  //.batchABblocksCnt;
    }
    
    return null;
}
public void removeBatch(int id){
    int lid;
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(id==lid) loadedBatches.remove(i);
    }
}

public boolean isBatchCompleted(int id){
    int cnt;
    for(int i=0;i<loadedBatches.size();i++){
        cnt=loadedBatches.get(i).BatchBlkCountDn;
        if(cnt<=0) return true;
    }
    return false;
}

public void batchCountDn(int id){
    int lid;
    for(int i=0;i<loadedBatches.size();i++){
        lid=loadedBatches.get(i).batchId;
        if(id==lid) loadedBatches.get(i).CountDn();
    }
    
    
    
    
}

public class LoadedBatch{
    public int batchId;
    public int totalJobs;
    //public  ArrayList<int[]> batchABblocksCnt;
    public  ArrayList<int[]> batchABblocksJobCnt;
    public BatchDataHelper batchDataHelper ;
    public int BatchBlkCountDn; //[BlkCountDn] keep track of each loaded batch recived blocks, --BlkCountDn for each  recvd Blk
                                    // need to becorrected to count the segments
    
    public LoadedBatch(int id, int tJobs, ArrayList<int[]> bABblocksJobCnt, BatchDataHelper batchHelper){
        batchId = id;
        totalJobs= tJobs;
        batchABblocksJobCnt=bABblocksJobCnt;
        batchDataHelper=batchHelper;
        BatchBlkCountDn=totalJobs;//bABblocksCnt.size();
    }
    
    
    public void CountDn(){
        BatchBlkCountDn--;
    }
    
    public void removeBlock(int blkId){
        //make sure the blk job count is 0
        int blkJobCnt=0;
        int numRecInThisBlk=0;
        int[] batchABblocksJobCnt1;
       for ( int i=0; i< batchABblocksJobCnt.size();i++) {
           batchABblocksJobCnt1=batchABblocksJobCnt.get(i);
            if(batchABblocksJobCnt1[1]==blkId){   //batchABblocksJobCnt1[batchid,blkId,A,B,Jobcnt]
                blkJobCnt=batchABblocksJobCnt1[4];
                numRecInThisBlk=batchABblocksJobCnt1[3];
               batchABblocksJobCnt.remove(batchABblocksJobCnt1);
            }
            }
        if(blkJobCnt>0 || numRecInThisBlk<=0) {
            System.out.println("\n ========>  JobCnt is not 0 or Blk has 0 recs");
            return;
        }
        ArrayList<String> batchRecIDs = new ArrayList<>(); // array of record IDs of this batch
   
        ArrayList<Integer> batchBlockIDs= new ArrayList<>();
        ArrayList<boolean[][]> batchOnes = new ArrayList<>();
        ArrayList<int[]> batchIntOnes = new ArrayList<>();
        ArrayList<boolean[][]> batchData = new ArrayList<>();
        
        
        for (int i=0;i<batchDataHelper.IDs.size();i++){
            if((int)batchDataHelper.BlockIDs.get(i)!=blkId){
                batchBlockIDs.add(batchDataHelper.BlockIDs.get(i));
                batchRecIDs.add(batchDataHelper.IDs.get(i));
                batchOnes.add(batchDataHelper.BlknumOfOnesInBFs[i]);
                batchIntOnes.add(batchDataHelper.BlkintNumOfOnesInBFs[i]);
                batchData.add(batchDataHelper.Blkdata_bin[i]);
           }
        }
        int newSize = batchData.size();
        batchDataHelper.Blkdata_bin = new boolean[newSize][][];
        batchDataHelper.BlknumOfOnesInBFs = new boolean[newSize][][];
        batchDataHelper.BlkintNumOfOnesInBFs = new int[newSize][];
        batchDataHelper.IDs.clear();
        batchDataHelper.BlockIDs.clear();
        for (int i = 0; i < newSize; i++) {
            batchDataHelper.Blkdata_bin[i] = batchData.get(i);
            batchDataHelper.BlknumOfOnesInBFs[i] = batchOnes.get(i);
            batchDataHelper.BlkintNumOfOnesInBFs[i] = batchIntOnes.get(i);
            batchDataHelper.IDs.add(batchRecIDs.get(i));
            batchDataHelper.BlockIDs.add(batchBlockIDs.get(i));
        }
    }
    
    public void batchBlkJobCountDn(int BlkId){
        for (int[] batchABblocksJobCnt1 : batchABblocksJobCnt) {
            if(batchABblocksJobCnt1[1]==BlkId){
                int[] ABblocksJobCnt1= new int[5];
                System.arraycopy( batchABblocksJobCnt1, 0, ABblocksJobCnt1, 0, batchABblocksJobCnt1.length );
                ABblocksJobCnt1[4]--;       
                batchABblocksJobCnt.remove(batchABblocksJobCnt1);
                batchABblocksJobCnt.add(ABblocksJobCnt1);
                
                break;
        }
        }
        
        
    }
}
}
