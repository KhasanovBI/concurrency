package C10TODO.bucketBarrier;

/**
 * Created by bulat on 15.01.17.
 */
public interface Drop {
    void arrived();  // notifies Bucket.awaitDrop() that a thread has arrived to a barrier.
    // then blocks until Bucket.leak() is called.
}