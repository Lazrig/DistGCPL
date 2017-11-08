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
public class DispatchWorkerInfo implements java.io.Serializable{

    public String WorkerIP;
    public int DispatchAssigned2Port;
    public int DispatchedBlockID;
    public boolean processed;  // for Gen, it means sent to eval(maybe not needed) . For Eval, it means sent to workers

    public DispatchWorkerInfo(int blkId, String WIp, int dPort) {
        WorkerIP = WIp;
        DispatchAssigned2Port = dPort;
        DispatchedBlockID = blkId;
    }

}
