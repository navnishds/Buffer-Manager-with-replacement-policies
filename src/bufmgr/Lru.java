
package bufmgr;

import diskmgr.*;
import global.*;
import java.util.*;

  /**
   * Lru replacement policy.
   */
class Lru extends  Replacer {

    //
    // Frame State Constants
    //
    protected static final int AVAILABLE = 10;
    protected static final int REFERENCED = 11;
    protected static final int PINNED = 12;

    //Following are the fields required for LRU and MRU policies:
    /**
     * private field
     * An array to hold number of frames in the buffer pool
     */

    private int  frames[];
    //array to keep track of available frame
    ArrayList<Integer> availableFrames;
    //array to keep track of least recently used pages
    ArrayList<Integer> leastRecentlyUsed;
    /**
     * private field
     * number of frames used
     */   
    private int  nframes;
    //Buffer size
    int numberOfBuffers;
    int index;

    /** Clock head; required for the default clock algorithm. */
    protected int head;

    /**
     * This pushes the given frame to the end of the list.
     * @param frameNo   the frame number
     */
    private void update(FrameDesc fdesc)
    {
        //add page which is not in pool at the end or if in pool remove from array and add it at end since it's most recently used
        int i = leastRecentlyUsed.indexOf(fdesc.index);
        if (i == -1) {
            leastRecentlyUsed.add(fdesc.index);
        }
        else {
            leastRecentlyUsed.remove(i);
            leastRecentlyUsed.add(fdesc.index);
        }
    }

    /**
     * Class constructor
     */
    public Lru(BufMgr mgrArg)
    {
        super(mgrArg);
        //Get the buffer size
        numberOfBuffers = mgrArg.getNumBuffers();
        availableFrames = new ArrayList<Integer>(numberOfBuffers);
        leastRecentlyUsed = new ArrayList<Integer>();
        // initialize the frame states
        for (int i = 0; i < frametab.length; i++) {
            frametab[i].state = AVAILABLE;
            availableFrames.add(i);
        }
        // initialize parameters for LRU and MRU
        nframes = 0;
        frames = new int[frametab.length];
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
        //add frame since page is deallocated
        availableFrames.add(fdesc.index);
        leastRecentlyUsed.remove(leastRecentlyUsed.indexOf(fdesc.index));
    }

    /**
     * Notifies the replacer of a pined page.
     */
    public void pinPage(FrameDesc fdesc) {
        fdesc.state = PINNED;
        update(fdesc);
    }

    /**
     * Notifies the replacer of an unpinned page.
     */
    public void unpinPage(FrameDesc fdesc) {
        if (fdesc.pincnt == 0) {
            fdesc.state = AVAILABLE;
        }
    }
  
    /**
     * Finding a free frame in the buffer pool
     * or choosing a page to replace using your policy
     *
     * @return  return the frame number
     *      return -1 if failed
     */

   public int pickVictim()
   {
        nframes = availableFrames.size();
        //check if any frame is empty
        if (nframes > 0) {
            index = availableFrames.remove(0);
            return index;
        }
        //select least recently used available frame for replacement 
        else {
            for (int i = 0; i < leastRecentlyUsed.size(); i++) {
                FrameDesc tempfd = frametab[leastRecentlyUsed.get(i)];
                if (tempfd.state == AVAILABLE) {
                    leastRecentlyUsed.remove(i);
                    return tempfd.index;
                }
            }
        }
        //Buffer size full
        return -1;
   }
}
