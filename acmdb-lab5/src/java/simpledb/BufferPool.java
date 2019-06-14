package simpledb;

import com.sun.webkit.PageCache;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    private static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;

    private final int pageLimit;
    private ConcurrentHashMap<PageId, Page> pageCache;
    private ConcurrentHashMap<PageId, Integer> LRUCount;
    private int counter = 0;

    private class PageLock {
        private Set<TransactionId> sharedLock = ConcurrentHashMap.newKeySet();
        private TransactionId exclusiveLock = null;
        public PageId pageId;

        PageLock(PageId pid) {
            pageId = pid;
        }

        boolean requireLock(Permissions perm, TransactionId tid) {
            if (perm.equals(Permissions.READ_ONLY)) return requireShared(tid);
            return requireExclusive(tid);
        }

        private boolean requireShared(TransactionId tid) {
            if (exclusiveLock != null) return exclusiveLock.equals(tid);
            sharedLock.add(tid);
            return true;
        }

        private boolean requireExclusive(TransactionId tid) {
            if (exclusiveLock != null) return exclusiveLock.equals(tid);
            if (sharedLock.size() > 1) return false;
            if (sharedLock.isEmpty() || sharedLock.contains(tid)) {
                exclusiveLock = tid;
                sharedLock.clear();
                return true;
            }
            return false;
        }

        void releaseLock(TransactionId tid) {
            assert exclusiveLock == null || tid.equals(exclusiveLock);
            if (tid.equals(exclusiveLock)) exclusiveLock = null;
            else sharedLock.remove(tid);
        }

        boolean holdsLock(TransactionId tid) {
            return exclusiveLock.equals(tid) || sharedLock.contains(tid);
        }

        boolean exclusive() {
            return exclusiveLock != null;
        }

        Set<TransactionId> relatedTransactions() {
            Set<TransactionId> transactionIds = new HashSet<>(sharedLock);
            if (exclusiveLock != null) transactionIds.add(exclusiveLock);
            return transactionIds;
        }

    }

    private class DependencyGraph {
        // The edges are directed. Each of the edge is pointed to owner from requestor
        private HashMap<TransactionId, HashSet<TransactionId>> edges = new HashMap<>();

        synchronized boolean updateEdges(TransactionId requestor, PageId pid) {
            edges.putIfAbsent(requestor, new HashSet<>());
            HashSet<TransactionId> requestEdges = edges.get(requestor);
            requestEdges.clear();
            if (pid == null) return false;

            Set<TransactionId> owners;
            synchronized (lockManager.get(pid)) {
                owners = lockManager.get(pid).relatedTransactions();
            }

            requestEdges.addAll(owners);
            boolean hasCycle = findCycle(requestor);
            if (hasCycle) edges.remove(requestor);
            return hasCycle;
        }

        private boolean findCycle(TransactionId start) {
            HashSet<TransactionId> visited = new HashSet<>();
            Queue<TransactionId> bfsQueue = new LinkedList<>();
            bfsQueue.add(start);
            while (!bfsQueue.isEmpty()) { // bfs
                TransactionId curPoint = bfsQueue.poll();
                HashSet<TransactionId> curEdges = edges.get(curPoint);
                if (curEdges == null) continue;
                for (TransactionId toPoint : curEdges) {
                    if (!visited.contains(toPoint)) {
                        bfsQueue.add(toPoint);
                        visited.add(toPoint);
                    }
                    if (toPoint.equals(start)) break;
                }
            }
            return visited.contains(start);
        }

    }

    private ConcurrentHashMap<PageId, PageLock> lockManager;
    private ConcurrentHashMap<TransactionId, Set<PageId>> transactionLockHolder;
    private DependencyGraph dependencyGraph;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        pageLimit = numPages;
        pageCache = new ConcurrentHashMap<>();
        LRUCount = new ConcurrentHashMap<>();
        lockManager = new ConcurrentHashMap<>();
        transactionLockHolder = new ConcurrentHashMap<>();
        dependencyGraph = new DependencyGraph();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = PAGE_SIZE;
    }

    private void LRUUpdate(PageId pid) {
        LRUCount.put(pid, counter++);
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        // some code goes here
        lockManager.putIfAbsent(pid, new PageLock(pid));
        boolean requiredLock;
        synchronized (lockManager.get(pid)) {
             requiredLock = lockManager.get(pid).requireLock(perm, tid);
        }

        while (!requiredLock) {
            if (dependencyGraph.updateEdges(tid, pid)) {
                throw new TransactionAbortedException();
            }
            Thread.yield();
            synchronized (lockManager.get(pid)) {
                requiredLock = lockManager.get(pid).requireLock(perm, tid);
            }
        }
        dependencyGraph.updateEdges(tid, null);

        transactionLockHolder.putIfAbsent(tid, new HashSet<>());
        Set<PageId> lockedPages = transactionLockHolder.get(tid);
        lockedPages.add(pid);

        Page page;
        if (!pageCache.containsKey(pid)) {
            while (pageCache.size() >= pageLimit) {
                evictPage();
            }
            page = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
            pageCache.put(pid, page);
        }
        page = pageCache.get(pid);
//        if (perm == Permissions.READ_WRITE) {
//            page.markDirty(true, tid);
//        }
        LRUUpdate(pid);
        return page;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        synchronized (lockManager.get(pid)) {
            lockManager.get(pid).releaseLock(tid);
        }
        transactionLockHolder.get(tid).remove(pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        boolean flag = false;
        synchronized (lockManager.get(p)) {
            flag = lockManager.get(p).holdsLock(tid);
        }
        return flag;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        Set<PageId> lockedPages = transactionLockHolder.get(tid);
        if (lockedPages == null) return;
        for (PageId pid : lockedPages) {
            if (lockManager.get(pid).exclusive()) {
                Page page = pageCache.get(pid);
                if (page != null && page.isDirty() != null) {
                    assert page.isDirty().equals(tid);
                    if (commit) {
                        // write back
                        flushPage(pid);
                    } else {
                        discardPage(pid);
//                        page = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
//                        page.markDirty(false, null);
//
//                        pageCache.put(pid, page);
//                        LRUUpdate(pid);
                    }
                }
            }
            synchronized (lockManager.get(pid)) {
                lockManager.get(pid).releaseLock(tid);
            }
        }
        transactionLockHolder.remove(tid);
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
      public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        ArrayList<Page> pages = Database.getCatalog().getDatabaseFile(tableId).insertTuple(tid, t);
        handleDirtyPages(tid, pages);
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        int tableId = t.getRecordId().getPageId().getTableId();

        ArrayList<Page> pages = Database.getCatalog().getDatabaseFile(tableId).deleteTuple(tid, t);
        handleDirtyPages(tid, pages);

    }

    private void handleDirtyPages(TransactionId tid, List<Page> dirtypages) throws DbException {
        for (Page page : dirtypages) {
            PageId pid = page.getId();
            while (!pageCache.containsKey(pid) && pageCache.size() >= pageLimit) {
                evictPage();
            }
            page.markDirty(true, tid);
            pageCache.put(pid, page);
            LRUUpdate(pid);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (PageId pageId : pageCache.keySet()) {
            flushPage(pageId);
        }
    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * <p>
     * Also used by B+ tree files to ensure that deleted pages
     * are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        if (!pageCache.containsKey(pid)) return;
        remove(pid);
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        Page page = pageCache.get(pid);
        if (page == null) throw new IOException();
        if (page.isDirty() == null) return;
        page.markDirty(false, null);
        Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(page);
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        for (Map.Entry<PageId, Page> entry : pageCache.entrySet()) {
            PageId pid = entry.getKey();
            synchronized (pageCache.get(pid)) {
                if (pageCache.get(pid).isDirty() == null) {
                    evict(pid);
                    return;
                }
            }
        }
        throw new DbException("None page can be evicted for NO STEAL POLICY!");
    }

    private void evict(PageId evictPageId) {
        try {
            flushPage(evictPageId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        remove(evictPageId);
    }

    private void remove(PageId evictPageId) {
        pageCache.remove(evictPageId);
        LRUCount.remove(evictPageId);
    }
}
