/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import java.util.Objects;

/**
 *
 * @author cf
 */
public class PatientLinkageResults implements java.io.Serializable{
    public String AID;
    public String BID;
    public double score;

   
    public PatientLinkageResults(String AID, String BID, double score) {
        this.AID = AID;
        this.BID = BID;
        this.score = score;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.AID);
        hash = 31 * hash + Objects.hashCode(this.BID);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PatientLinkageResults other = (PatientLinkageResults) obj;
        if (!Objects.equals(this.AID, other.AID)) {
            return false;
        }
        if (!Objects.equals(this.BID, other.BID)) {
            return false;
        }
        return true;
    }

   
  
}
