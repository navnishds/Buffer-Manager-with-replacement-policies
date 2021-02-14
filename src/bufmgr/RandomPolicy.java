
package bufmgr;

import diskmgr.*;
import global.*;
import java.util.*;

  /**
   * Random replacement policy.
   */
class RandomPolicy extends  Replacer {

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
 
  /**
   * private field
   * number of frames used
   */   
  private int  nframes;
  int bufferSize;
  int index;

  /** Clock head; required for the default clock algorithm. */
  protected int head;

  /**
   * This pushes the given frame to the end of the list.
   * @param frameNo	the frame number
   */
  private void update(int frameNo)
  {
     //This function is to be used for LRU and MRU
  }

  /**
   * Class constructor
   */
    public RandomPolicy(BufMgr mgrArg)
    {
      super(mgrArg);
      //Get the buffer size
      bufferSize = mgrArg.getNumBuffers();
      availableFrames = new ArrayList<Integer>(bufferSize);
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
      fdesc.state = AVAILABLE;
    }
  }
  
  /**
   * Finding a free frame in the buffer pool
   * or choosing a page to replace using your policy
   *
   * @return 	return the frame number
   *		return -1 if failed
   */

 public int pickVictim()
 {
    //check if any frame is empty
    if (availableFrames.size() > 0) {
      index = availableFrames.remove(0);
      return index;
    }
    //select random available frame for replacement 
    else {
      int findVictim = 0;
      List<FrameDesc> tempFrametab = new ArrayList<FrameDesc>(Arrays.asList(frametab));
      FrameDesc fdesc;
      Random rand = new Random();
      while(findVictim == 0 && tempFrametab.size() > 0) {
          index = rand.nextInt(tempFrametab.size());
          fdesc = tempFrametab.get(index);
          if (fdesc.state == AVAILABLE) {
              findVictim = 1;
              index = fdesc.index;
              return index;
          }
          else {
              tempFrametab.remove((int)index);
          }
      }
    }
    //Buffer size full
    return -1;
 }

}