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
public class TaskConfig implements java.io.Serializable{
 
    public String party = "nobody";
    public String addr = null;
    public int port = -1;
    public int threshold = 1;
    public int threads = 1;
    public int hTasks = 1;
    public int vTasks=1;
    public int  numOfParts=4;
    public String TaskId;
    //String results_save_path = null;
        
    //boolean useCombBF = false;//  True:  Use combinations of the BFs , False: use  BFs one by one and filter results by removing
        // those ones matched by the previous BF
    public boolean PassNumOfOnes = true; //Send BF's number of ones with each record BFs instead of securely computing it.        
   
    public boolean useChkDci = true;
    public boolean useBfsInSeq = true;
    
    public TaskConfig(
    String party , String addr, int port,int threshold, int threads,int hTasks, int vTasks, int  numOfParts,
    boolean PassNumOfOnes,boolean useChkDci, boolean useBfsInSeq){
     this.party =party;
     this.addr = addr;
     this.port = port;
     this.threshold =threshold;
     this.threads=threads;
     this.hTasks=hTasks;
     this.vTasks=vTasks;
     this.numOfParts=numOfParts;
     this.PassNumOfOnes = PassNumOfOnes;
     this.useChkDci = useChkDci;
     this.useBfsInSeq = useBfsInSeq;
        
        
  }
    
  
}
