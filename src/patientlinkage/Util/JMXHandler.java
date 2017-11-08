

package patientlinkage.Util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

import javax.management.*;

import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.ExceptionUtils;

import org.jppf.node.protocol.Task;
import java.util.*;
import org.jppf.client.monitoring.jobs.*;
import java.util.concurrent.*;
import patientlinkage.DataType.PatientLinkageResults;

public class JMXHandler implements NotificationListener {
  private final JMXDriverConnectionWrapper jmx;
  private boolean registered = false;
  public boolean verbose=false; 
    //ArrayList<PatientLinkageResults> AllBlksResults;
    //CountDownLatch cDown;
    ObjectOutputStream genOs;

  public JMXHandler(JMXDriverConnectionWrapper jmx, ObjectOutputStream gOs) {
     //AllBlksResults=Results;
    this.jmx = jmx;
     // cDown=cD;
      genOs=gOs;
  }

  public void register() {
    try {
      // get a proxy to the job management MBean
      DriverJobManagementMBean jobManager = jmx.getJobManager();
      // register this notification listener
      jobManager.addNotificationListener(this, null, null);
      synchronized(this) {
        registered = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void unregister() {
    try {
      DriverJobManagementMBean jobManager = jmx.getJobManager();
      // unregister this notification listener
      jobManager.removeNotificationListener(this, null, null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      synchronized(this) {
        registered = false;
      }
    }
  }

  public synchronized boolean isRegistered() {
    return registered;
  }

  @Override
  public synchronized void handleNotification(Notification notification, Object handback) {
    JobNotification jobNotif = (JobNotification) notification;
    JobEventType eventType = jobNotif.getEventType();
    if (eventType == JobEventType.JOB_RETURNED) {
      // start a slave of the node where the job was dispatched
      JPPFManagementInfo nodeInfo = jobNotif.getNodeInfo();
      if (verbose){
        System.out.printf("\n job '%s' returned%n", jobNotif.getJobInformation().getJobName());
        System.out.printf("\n Job was in node %s \n",nodeInfo.getHost());
      }
      if (nodeInfo.isMasterNode()) {
        //startOrStopSlaveNodes(nodeInfo.getUuid(), 1);
      }
    } else if (eventType == JobEventType.JOB_ENDED) {
      // handle completed jobs
      if (verbose){
      System.out.printf("\n job '%s' ended%n", jobNotif.getJobInformation().getJobName());
      }
    /*    Job job =new Job(jobNotif.getJobInformation());
        List<Task<?>> results = job.getAllResults();
       //List<Task<?>> results = (event.getJob()).getAllResults();
        for (Task<?> task : results) {
            if (task.getThrowable() != null) { // if the task execution raised an exception
                System.out.printf("%s raised an exception : %s%n", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
            } else { // otherwise display the task result
                //taskBlkResults=(ArrayList<PatientLinkageResults>)task.getResult();
                for (int tr : (ArrayList<Integer>) task.getResult()) {
                    
                    AllBlksResults.add(tr);
                }
                //System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
            }
        }
        cDown.countDown();
    
*/
      
      
      //restartAllNodes();
    }
    else if (eventType == JobEventType.JOB_DISPATCHED) {
        // handle
        JPPFManagementInfo nodeInfo = jobNotif.getNodeInfo();
        if (verbose)
            System.out.printf("\n job '%s' Dispatched to node %s %n", jobNotif.getJobInformation().getJobName(),nodeInfo.getIpAddress());
       
        try {

                    genOs.writeObject(jobNotif.getJobInformation().getJobName()+"-"+nodeInfo.getIpAddress());
                    genOs.flush();
                    if (verbose)
                        System.out.printf("\n Data in notification written to genOs");
                    // Files.write(Paths.get("IPs.txt"), ("Node sent this :"+  lhost+"\n").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    //exception handling
                } 
    }
    else if (eventType == JobEventType.JOB_QUEUED) {
        // handle
        if (verbose)
            System.out.printf("\n job '%s' queued %n", jobNotif.getJobInformation().getJobName());
        
    }
      else if (eventType == JobEventType.JOB_UPDATED) {
        // handle
        if (verbose)
            System.out.printf("\n job '%s' updated %n", jobNotif.getJobInformation().getJobName());
        
    }
  }

  // beware that killing a master node will also kill all its slaves
  private void restartAllNodes() {
    try {
      // node forwarder fowards requests to selected nodes
      JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
      // restart all nodes
      Map<String, Object> result = forwarder.restart(NodeSelector.ALL_NODES);
      // check the result for each node
      for (Map.Entry<String, Object> entry: result.entrySet()) {
        if (entry.getValue() instanceof Exception) {
          throw (Exception) entry.getValue();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // use nbSlave = 1 to start a new slave
  // use nbSlave = -1 to stop a slave
  private void startOrStopSlaveNodes(String masterNodeUuid, int nbSlaves) {
    try {
      // node forwarder fowards requests to selected nodes
      JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
      NodeSelector selector = new UuidSelector(masterNodeUuid);
      // get the currebt bumber of slaves
      Map<String, Object> result = forwarder.getNbSlaves(selector);
      Object o = result.get(masterNodeUuid);
      if (o instanceof Integer) {
        int n = (Integer) o;
        // provision or un-provision slaves based on nbSlaves
        result = forwarder.provisionSlaveNodes(selector, n + nbSlaves);
        // check the result for each node
        for (Map.Entry<String, Object> entry: result.entrySet()) {
          if (entry.getValue() instanceof Exception) {
            System.out.printf("\n restarting node uuid '%s' raised throwable: %s%n", entry.getKey(), ExceptionUtils.getStackTrace((Exception) entry.getValue()));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
