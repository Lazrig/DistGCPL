/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import java.util.ArrayList;

/**
 *
 * @author cf
 */
public class BlkHelper  implements java.io.Serializable{
    public ArrayList<String> BlkIDs = new ArrayList<>(); // array of record IDs of this BlockID
    public int BlockID;
    public int blkSize;
    public boolean[][][] Blkdata_bin;
    public boolean[][][] BlknumOfOnesInBFs;
    public int[][] BlkintNumOfOnesInBFs;
   

    public ArrayList<String> getIDs() {
        return BlkIDs;
    }
   
 
    
public BlkHelper(Helper help1,int BlkId,int BlkIndx, boolean RmvMatch){
     //BlkIndx which bloking to use: 0:ln,1:yob,2:md
        BlockID=BlkId;
        ArrayList<boolean[][]> BlkOnes=new ArrayList<>();
        ArrayList<int[]> BlkIntOnes=new ArrayList<>();
        ArrayList<boolean[][]> BlkData=new ArrayList<>();
        
        for(int i=0;i<help1.data_bin.length;i++){
            if(help1.Blocks.get(i)[BlkIndx]==BlkId){
                if(RmvMatch && help1.Matched[i]){
                    
                 continue;  
                }
                else{
                BlkData.add(help1.data_bin[i]); 
                BlkIDs.add(help1.IDs.get(i));
                BlkIntOnes.add(help1.intNumOfOnesInBFs[i]);
                BlkOnes.add(help1.numOfOnesInBFs[i]);
                }
            }
            
        }
        blkSize=BlkData.size();
        Blkdata_bin=new boolean[blkSize][][];
        BlknumOfOnesInBFs=new boolean[blkSize][][];
        BlkintNumOfOnesInBFs=new int[blkSize][];
        for(int i=0;i<blkSize;i++){
            Blkdata_bin[i]=BlkData.get(i);
            BlknumOfOnesInBFs[i]=BlkOnes.get(i);
            BlkintNumOfOnesInBFs[i]=BlkIntOnes.get(i);
        }
    }
    
    
    
    
public BlkHelper(Helper help1,int BlkId,int BlkIndx){
     //BlkIndx which bloking to use: 0:ln,1:yob,2:md
        BlockID=BlkId;
        ArrayList<boolean[][]> BlkOnes=new ArrayList<>();
        ArrayList<int[]> BlkIntOnes=new ArrayList<>();
        ArrayList<boolean[][]> BlkData=new ArrayList<>();
        
        for(int i=0;i<help1.data_bin.length;i++){
            if(help1.Blocks.get(i)[BlkIndx]==BlkId){
                BlkData.add(help1.data_bin[i]); 
                BlkIDs.add(help1.IDs.get(i));
                BlkIntOnes.add(help1.intNumOfOnesInBFs[i]);
                BlkOnes.add(help1.numOfOnesInBFs[i]);
                
            }
            
        }
        blkSize=BlkData.size();
        Blkdata_bin=new boolean[blkSize][][];
        BlknumOfOnesInBFs=new boolean[blkSize][][];
        BlkintNumOfOnesInBFs=new int[blkSize][];
        for(int i=0;i<blkSize;i++){
            Blkdata_bin[i]=BlkData.get(i);
            BlknumOfOnesInBFs[i]=BlkOnes.get(i);
            BlkintNumOfOnesInBFs[i]=BlkIntOnes.get(i);
        }
    }
    
    
public int[] getIntBF1sCnt(int BFindex) { //
        
        int[] retdata=new int[BlkintNumOfOnesInBFs.length];
        for(int i=0;i<BlkintNumOfOnesInBFs.length;i++){
            retdata[i]=BlkintNumOfOnesInBFs[i][BFindex];
            
        }
    
        return retdata;
    }
    
  
    
}
