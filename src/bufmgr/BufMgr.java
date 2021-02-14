package bufmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;
import java.util.HashMap;

/**
 * <h3>Minibase Buffer Manager</h3>
 * The buffer manager reads disk pages into a mains memory page as needed. The
 * collection of main memory pages (called frames) used by the buffer manager
 * for this purpose is called the buffer pool. This is just an array of Page
 * objects. The buffer manager is used by access methods, heap files, and
 * relational operators to read, write, allocate, and de-allocate pages.
 */
public class BufMgr implements GlobalConst {

    /** Actual pool of pages (can be viewed as an array of byte arrays). */
    protected Page[] bufpool;

    /** Array of descriptors, each containing the pin count, dirty status, etc\
	. */
    protected FrameDesc[] frametab;

    /** Maps current page numbers to frames; used for efficient lookups. */
    protected HashMap<Integer, FrameDesc> pagemap;

    /** The replacement policy to use. */
    protected Replacer replacer;

    // Variable to keep track of request and hit
    protected int bhr1RequestCount;
    protected int bhr1HitCount;
    protected int bhr2RequestCount;
    protected int bhr2HitCount;
    
//-------------------------------------------------------------

  /**
   * Constructs a buffer mamanger with the given settings.
   * 
   * @param numbufs number of buffers in the buffer pool
   */
  public BufMgr(int numbufs) 
  {   
	  //initializing buffer pool and frame table 
	    bufpool = new Page[numbufs];
      frametab = new FrameDesc[numbufs];
      // System.out.println(numbufs);
      for(int i = 0; i < frametab.length; i++)
      {
              bufpool[i] = new Page();
    	  	  frametab[i] = new FrameDesc(i);
      }
      
      //initializing page map and replacer here. 
      pagemap = new HashMap<Integer, FrameDesc>(numbufs);
      replacer = new Clock(this);

      // initializing hit and request count
      bhr1RequestCount = 0;
      bhr1HitCount = 0;
      bhr2RequestCount = 0;
      bhr2HitCount = 0;
  }

  /**
   * Allocates a set of new pages, and pins the first one in an appropriate
   * frame in the buffer pool.
   * 
   * @param firstpg holds the contents of the first page
   * @param run_size number of pages to allocate
   * @return page id of the first new page
   * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
   * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
   */

  public PageId newPage(Page firstpg, int run_size)
  {
	  //Allocating set of new pages on disk using run size.
	  PageId firstpgid = Minibase.DiskManager.allocate_page(run_size);
	  try {
	
		  pinPage(firstpgid, firstpg, true);
          }
          catch (Exception e) {
        	  //pinning failed so deallocating the pages from disk
        	  for(int i=0; i < run_size; i++)
        	  {   
        		  firstpgid.pid += i;
 	  	        Minibase.DiskManager.deallocate_page(firstpgid);
        	  }
        	  return null;
      }
	  
	  //notifying replacer
      replacer.newPage(pagemap.get(Integer.valueOf(firstpgid.pid)));
      //return the page id of the first page
      return firstpgid; 
  }
  
  /**
   * Deallocates a single page from disk, freeing it from the pool if needed.
   * 
   * @param pageno identifies the page to remove
   * @throws IllegalArgumentException if the page is pinned
   */
  public void freePage(PageId pageno) 
  {  
	  //the frame descriptor as the page is in the buffer pool 
	  FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));
	  //the page is in the pool so it cannot be null.
      if(tempfd != null)
      {
    	  //checking the pin count of frame descriptor
          if(tempfd.pincnt > 0)
              throw new IllegalArgumentException("Page currently pinned");
          //remove page as it's pin count is 0, remove the page, updating its pin count and dirty status, the policy and notifying replacer.
          pagemap.remove(Integer.valueOf(pageno.pid));
          tempfd.pageno.pid = INVALID_PAGEID;
          tempfd.pincnt = 0;
          tempfd.dirty = false;
          // tempfd.state = Clock.AVAILABLE;
          replacer.freePage(tempfd);
      }
      //deallocate the page from disk 
      Minibase.DiskManager.deallocate_page(pageno);
      //Intialize to 0 since all the pages are removed from pool
  }

  /**
   * Pins a disk page into the buffer pool. If the page is already pinned, this
   * simply increments the pin count. Otherwise, this selects another page in
   * the pool to replace, flushing it to disk if dirty.
   * 
   * @param pageno identifies the page to pin
   * @param page holds contents of the page, either an input or output param
   * @param skipRead PIN_MEMCPY (replace in pool); PIN_DISKIO (read the page in)
   * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
   * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
   */
  public void pinPage(PageId pageno, Page page, boolean skipRead) 
  {  
	  //the frame descriptor as the page is in the buffer pool 
	  FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));
    //pin the first page using pinpage() function using the id of firstpage, page firstpg and skipread = PIN_MEMCPY(true)
    
    //Increment bhr1RequestCount
    if (pageno.pid > 8) {
      bhr1RequestCount++;
    }
	  if(tempfd != null)
	  {
		  //if the page is in the pool and already pinned then by using PIN_MEMCPY(true) throws an exception "Page pinned PIN_MEMCPY not allowed"
          if(skipRead) {
        	  throw new IllegalArgumentException("Page pinned so PIN_MEMCPY not allowed");
          }
          else
          {
        	  //else the page is in the pool and has not been pinned so incrementing the pincount and setting Policy status to pinned
        	  tempfd.pincnt++;
            //Increment bh1HitCount and bhr2HitCount since page in pool and it's a hit
            if (pageno.pid > 8) {
              bhr1HitCount++;
              bhr2HitCount++;
              // System.out.println(pageno.pid);
            }
        	  // tempfd.state = Clock.PINNED;
            replacer.pinPage(tempfd);
            page.setPage(bufpool[tempfd.index]);
            return;
          }
	  }
	  else
	  {
		  //as the page is not in pool choosing a victim
          int i = replacer.pickVictim();
          //if buffer pool is full throws an Exception("Buffer pool exceeded")
          if(i < 0)
        	  throw new IllegalStateException("Buffer pool exceeded");
                
          tempfd = frametab[i];

          //if the victim is dirty writing it to disk 
          if(tempfd.pageno.pid != -1)
          {
        	  pagemap.remove(Integer.valueOf(tempfd.pageno.pid));
        	  if(tempfd.dirty)
           		  Minibase.DiskManager.write_page(tempfd.pageno, bufpool[i]);
        	  
          }
          //reading the page from disk to the page given and pinning it. 
          if(skipRead)
        	  bufpool[i].copyPage(page);
          else
          	  Minibase.DiskManager.read_page(pageno, bufpool[i]);
          page.setPage(bufpool[i]);
	  }
	  //updating frame descriptor and notifying to replacer
    tempfd.pageno.pid = pageno.pid;
    tempfd.pincnt = 1;
    tempfd.dirty = false;
    pagemap.put(Integer.valueOf(pageno.pid), tempfd);
    // tempfd.state = Clock.PINNED;
	  replacer.pinPage(tempfd);
    //Increment bh2RequestCount
    if (pageno.pid > 8) {
      bhr2RequestCount++;
    }
  }

  /**
   * Unpins a disk page from the buffer pool, decreasing its pin count.
   * 
   * @param pageno identifies the page to unpin
   * @param dirty UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherrwise
   * @throws IllegalArgumentException if the page is not present or not pinned
   */
  public void unpinPage(PageId pageno, boolean dirty) 
  {  
	  //the frame descriptor as the page is in the buffer pool 
	  FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));
	  
	  //if page is not present an exception is thrown as "Page not present"
      if(tempfd == null)
          throw new IllegalArgumentException("Page not present");
      
      //if the page is present but not pinned an exception is thrown as "page not pinned"
      if(tempfd.pincnt == 0)
      {
          throw new IllegalArgumentException("Page not pinned");
      } 
      else
      {
    	  //unpinning the page by decrementing pincount and updating the frame descriptor and notifying replacer
          tempfd.pincnt--;
          tempfd.dirty = dirty;
          // if(tempfd.pincnt== 0)
          // tempfd.state = Clock.REFERENCED;
          replacer.unpinPage(tempfd);
          return;
      }
  }

  /**
   * Immediately writes a page in the buffer pool to disk, if dirty.
   */
  public void flushPage(PageId pageno) 
  {  
	  for(int i = 0; i < frametab.length; i++)
	 	  //checking for pageid or id the pageid is the frame descriptor and the dirty status of the page
          if((pageno == null || frametab[i].pageno.pid == pageno.pid) && frametab[i].dirty)
          {
        	  //writing down to disk if dirty status is true and updating dirty status of page to clean
              Minibase.DiskManager.write_page(frametab[i].pageno, bufpool[i]);
              frametab[i].dirty = false;
          }
  }

  /**
   * Immediately writes all dirty pages in the buffer pool to disk.
   */
  public void flushAllPages() 
  {
	  for(int i=0; i<frametab.length; i++) 
		  flushPage(frametab[i].pageno);
  }

  /**
   * Gets the total number of buffer frames.
   */
  public int getNumBuffers() 
  {
	  return bufpool.length;
  }

  /**
   * Gets the total number of unpinned buffer frames.
   */
  public int getNumUnpinned() 
  {
	  int numUnpinned = 0;
	  for(int i=0; i<frametab.length; i++) 
	  {
		  if(frametab[i].pincnt == 0)
			  numUnpinned++;
	  }
    // System.out.println(numUnpinned);
	  return numUnpinned;
  }

  public void printBhrAndRefCount(){ 
  
    //print counts:
    System.out.println("totPageHits: "+bhr1HitCount);
    System.out.println("totPageRequests: "+bhr1RequestCount);
    System.out.println("pageLoadHits: "+bhr2HitCount);
    System.out.println("pageLoadRequests: "+bhr2RequestCount);
    System.out.println("+----------------------------------------+");
    
    
    //compute BHR1 and BHR2 
    float aggregateBHR = (float)bhr1HitCount/bhr1RequestCount;   
    float pageLoadBHR = (float)bhr2HitCount/bhr2RequestCount;
  
    // System.out.println("Aggregate BHR (BHR1): "+ aggregateBHR);
    // System.out.println("Load-based BHR (BHR2): "+ pageLoadBHR);
    System.out.print("Aggregate BHR (BHR1): ");
    System.out.printf("%9.5f\n", aggregateBHR);
    System.out.print("Load-based BHR (BHR2): ");
    System.out.printf("%9.5f\n", pageLoadBHR);
    System.out.println("+----------------------------------------+");
    
    
    //sc: set the first 8 page refs to 0 so it wont distort ref count
    // for (int i = 0; i < 9 ; i++)  {  
    //   //* System.out.println("+---Before setting it to zero: page, refCount: "+pageRefCount[i][2]+", "+pageRefCount[i][1]);
    //   pageRefCount[i][1]=0;
    // }
    
    // //before sorting, need to compare the FINAL round refcounts and fix it
    // for (int i = 0; i < pageRefCount.length ; i++) {
    //     if (pageRefCount[i][0] > pageRefCount[i][1]) pageRefCount[i][1] = pageRefCount[i][0];
    //     pageRefCount[i][0] = 0;
    // }
    // //Sort and print top 5 page references here. uncomment and substitute your code
    // sortbyColumn(pageRefCount, 1);
    
    // System.out.println("The top k referenced pages are:");
    // System.out.println("       Page No.\t\tNo. of references");
       
    // for (int i = 0; i < 10 ; i++)    
    //   System.out.println("\t"+pageRefCount[i][2]+"\t\t"+pageRefCount[i][1]);
    
    // System.out.println("+----------------------------------------+");
    //* System.out.println("pageRefCount.length: "+pageRefCount.length);
    // *for (int i = 0; i < pageRefCount.length ; i++)    
      // *System.out.println("\t"+pageRefCount[i][2]+"\t\t"+pageRefCount[i][1]+"\t\t"+pageRefCount[i][0]);
}


} // public class BufMgr implements GlobalConst