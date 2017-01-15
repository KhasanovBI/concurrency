package C10TODO.bucketBarrier;

/**
 * Created by bulat on 15.01.17.
 */
public interface Bucket {
    void awaitDrop(); // blocks until some other thread calls Drop.arrived()

    // second invocation of the await() must wait another thread to call Drop.ready()
    void leak();      // unblocks a thread arrived at Drop.arrived() point in a FIFO order.
}