# Buffer Manager with Clock, LRU or Random replacement policies

Course: CSE 5331 - DBMS Models and Implementation

Minibase Buffer Manager: 
The buffer manager reads disk pages into a mains memory page as needed. The collection of main memory pages (called frames) used by the buffer manager for this purpose is called the buffer pool. This is just an array of Page objects. The buffer manager is used by access methods, heap files, and relational operators to read, write, allocate, and de-allocate pages.

Replacement policies:

1. Clock: Give second chance to page which is currently not being used.
2. LRU: Select page for replacement which was least recently used. 
3. Random: Select any frame which is available for replacement.

Steps:

1. make bufmgr
2. make testFileName