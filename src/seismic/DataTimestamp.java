/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.sql.Timestamp;

/**
 * [10/10/2012]
 * 
 * @author Aqeel
 */
public class DataTimestamp {
    
    private final Timestamp ts;
    private final DataGroupingRelation relation;
    private final int baselineSrcID, userSrcID, propVal;
    private final String userName;

    public DataTimestamp(Timestamp ts, DataGroupingRelation relation, int baselineSrcID, int propVal, int userSrcID, String userName) {
        this.ts = ts;
        this.relation = relation;
        this.baselineSrcID = baselineSrcID;
        this.userSrcID = userSrcID;
        this.propVal = propVal;
        this.userName = userName;
    }
    
    // a baseline timestamp
    public DataTimestamp(int baselineSrcID) {
        this.ts = null;
        this.relation = DataGroupingRelation.BASELINE;
        this.baselineSrcID = baselineSrcID;
        this.userSrcID = -1;
        this.propVal = -1;
        this.userName = null;
    }
    
    public DataTimestamp() {
        this.ts = null;
        this.relation = DataGroupingRelation.BASELINE;
        this.baselineSrcID = -1;
        this.userSrcID = -1;
        this.propVal = -1;
        this.userName = null;
    }
    
    // ------------------------------------------------------------------------

    public Timestamp getTimestamp() {
        return ts;
    }

    public String getTimestampAsString() {
        if (isBaseline())
            return "BASELINE";
        
        // otherwise:
        return ts.toString().split("\\.")[0];
    }
    
    public boolean isBaseline() {
        return (relation == DataGroupingRelation.BASELINE);
    }

    public int getBaselineSrcID() {
        if (baselineSrcID == -1)
            System.err.println("baselineSrcID was not initialized!");
        
        return baselineSrcID;
    }

    public int getUserSrcID() {
        if (isBaseline())
            System.err.println("User source ID is requested but this is a baseline timestamp!");
        
        return userSrcID;
    }

    public String getUserName() {
        if ( userName == null || userName.isEmpty()
                || userName.equals("null") || userName.equals("NULL"))
            return "?";
        
        return userName;
    }

    public DataGroupingRelation getRelation() {
        return relation;
    }

    public int getPropVal() {
        return propVal;
    }
    
    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        if (isBaseline())
            return getTimestampAsString();
        
        // otherwise:
        return "" + getTimestampAsString() 
                + " (Feature Value: " + getPropVal() + ")"
                + " (Relation Type: " + getRelation().name() + ")"
                + " by [" + getUserSrcID() + "] " + getUserName();
    }//toString

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.ts != null ? this.ts.hashCode() : 0);
        hash = 97 * hash + (this.relation != null ? this.relation.hashCode() : 0);
        hash = 97 * hash + this.baselineSrcID;
        hash = 97 * hash + this.userSrcID;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataTimestamp other = (DataTimestamp) obj;
        if (this.ts != other.ts && (this.ts == null || !this.ts.equals(other.ts))) {
            return false;
        }
        if (this.relation != other.relation) {
            return false;
        }
        if (this.baselineSrcID != other.baselineSrcID) {
            return false;
        }
        if (this.userSrcID != other.userSrcID) {
            return false;
        }
        return true;
    }
    
}
