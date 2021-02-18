
package bufmgr;

import diskmgr.*;
import global.*;
import java.util.*;

/**
 * Clock replacement policy.
 */
 
class Clock extends Replacer {

    //
    // Frame State Constants
    //
    protected static final int AVAILABLE = 10;
    protected static final int REFERENCED = 11;
    protected static final int PINNED = 12;

    /** Clock head; required for the default clock algorithm. */
    protected int head;
    //Buffer size
    int numberOfBuffers;

    /**
     * Class constructor.
     */
   
    public Clock(BufMgr mgrArg) {
      
        super(mgrArg);
        //Get the buffer size
        numberOfBuffers = mgrArg.getNumBuffers();

        // Initialize the frame states
        for (int i = 0; i < frametab.length; i++) {
            frametab[i].state = AVAILABLE;
        }

        // Initialize the clock head
        head = -1;
    }
    /**
     * Notifies the replacer of a new page.
     */
    public void newPage(FrameDesc fdesc) {
        // no need to update frame state
    }

    /**
     * Notifies the replacer of a free page.
     */
    public void freePage(FrameDesc fdesc) {
        fdesc.state = AVAILABLE;
    }
    /**
     * Notifies the replacer of a pined page.
     */
    public void pinPage(FrameDesc fdesc) {
        fdesc.state = PINNED;
    }

    /**
     * Notifies the replacer of an unpinned page.
     */ 
    public void unpinPage(FrameDesc fdesc) {
        if (fdesc.pincnt == 0) {
            fdesc.state = REFERENCED;
        }
  
    }

    /**
     * Finding a free frame in the buffer pool
     * or choosing a page to replace using your policy
     *
     * @return  return the frame number
     *    return -1 if failed
     */
   
    public int pickVictim() {

        int i=0;
        while (i <= 2 * numberOfBuffers){
            head = (head+1) % numberOfBuffers;
            if (frametab[head].state == REFERENCED) {
                //Second chance 
                frametab[head].state = AVAILABLE;
            }
            else if (frametab[head].state == AVAILABLE) {
                //return available frame
                return head;
            }
            i++;
        }
        //Buffer pool full
        return -1;

    } 
}