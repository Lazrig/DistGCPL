/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author cf
 */
public class BlocksHelper {
    public ArrayList<String> IDs = new ArrayList<>();
    public ArrayList<int[]> Blocks=new ArrayList<>();
    
    public boolean[] Matched;
    
    public ArrayList<String> getIDs() {
        return IDs;
    }
    
    
    
    public void FilterOutMatchedRecsByIDs(ArrayList<String> MatchedIds) //using Party_IDs
    {
        for(int i=0;i<IDs.size();i++){
          if(MatchedIds.contains(IDs.get(i))){
            Matched[i]=true;  
          }
        }
    }
    
    
    public boolean IsThisIDMatched(String id){
        int idx=IDs.indexOf(id);
        if(idx!=-1) return Matched[idx];
        return false;
    }
    
 
    
    
    
    //---------------------------------------
    public  ArrayList<int[]> getEachBlockCount(int BlkVar,boolean useElimination) {

        ArrayList<int[]> BlocksCnt = new ArrayList();
        HashMap<Integer, Integer> BlocksCntHm = new HashMap(); //<Integer, Integer>
        
        if(useElimination){
        for (int i = 0; i < Blocks.size(); i++) {
            int[] PartyBlks = Blocks.get(i);

            int blkid = PartyBlks[BlkVar];
            
            if (!Matched[i]) {

                if (!BlocksCntHm.containsKey(blkid)) {
                    BlocksCntHm.put(blkid, 1);
                } else {
                    int oldCnt = BlocksCntHm.get(blkid);
                    BlocksCntHm.put(blkid, oldCnt + 1);

                }
            } // if !h1.Matched[i]
            
        }
        } else{
            for (int i = 0; i < Blocks.size(); i++) {
            int[] PartyBlks = Blocks.get(i);

            int blkid = PartyBlks[BlkVar];
            
            

                if (!BlocksCntHm.containsKey(blkid)) {
                    BlocksCntHm.put(blkid, 1);
                } else {
                    int oldCnt = BlocksCntHm.get(blkid);
                    BlocksCntHm.put(blkid, oldCnt + 1);

                }
           } // for i
        }

        Iterator it = BlocksCntHm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            int[] el = {(int) pairs.getKey(), (int) pairs.getValue()};
            BlocksCnt.add(el);
        }

        return BlocksCnt;
    }

    public void setIDs(ArrayList<String> IDs) {
        this.IDs = IDs;
    }

}
