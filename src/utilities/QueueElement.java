/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.util.HashSet;
import java.util.Set;

/**
 * This serves class DataLoader_SRDS.
 * A queue element consists of a point and one or more IDs associated with the point.
 *
 * @author Aqeel
 */
public class QueueElement {
    public final Point point;
    private Set<Integer> requiredIDs;

    // private constructor
    // at least one ID should be added:
    private QueueElement(Point point) {
        this.point = point;
        this.requiredIDs = new HashSet<Integer>();
    }
    
    public QueueElement(Point point, int id) {
        this.point = point;
        this.requiredIDs = new HashSet<Integer>();
    }
}
