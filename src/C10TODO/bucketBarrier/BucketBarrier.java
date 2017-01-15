package C10TODO.bucketBarrier;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;

/**
 * Created by bulat on 15.01.17.
 */
public class BucketBarrier implements Bucket, Drop {
    private final Deque<Thread> queue = new LinkedList<>();
    private final Object threadEnterMonitor = new Object();
    private final Object threadPassMonitor = new Object();
    private final Object threadExitMonitor = new Object();
    /**
     * Blocks until some other thread calls Drop.arrived()
     * second invocation of the await() must wait another thread to call Drop.ready()
     */
    @Override
    public void awaitDrop() {
        System.out.println("awaitDrop q: " + queue.size());
        try {
            synchronized (threadEnterMonitor) {
                if (queue.isEmpty()) {
                    threadEnterMonitor.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Notifies Bucket.awaitDrop() that a thread has arrived to a barrier.
     * Then blocks until Bucket.leak() is called.
     */
    @Override
    public void arrived() {
        System.out.println("arrived q: " + queue.size());
        synchronized (threadEnterMonitor) {
            threadEnterMonitor.notifyAll();
        }
        try {
            synchronized (threadPassMonitor) {
                Thread currentThread = Thread.currentThread();
                queue.add(currentThread);
                System.out.println(queue);
                threadPassMonitor.wait();
                while (queue.getFirst() != currentThread) {
                    System.out.printf("%s wait%n", currentThread);
                    threadPassMonitor.wait();
                }
                System.out.printf("%s queue exit q:%d%n", currentThread, queue.size());
            }
            synchronized (threadExitMonitor) {
                threadExitMonitor.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unblocks a thread arrived at Drop.arrived() point in a FIFO order.
     */
    @Override
    public void leak() {
        System.out.println("leak q: " + queue.size());
        try {
            synchronized (threadExitMonitor) {  // Чтобы leak был синхронным.
                synchronized (threadPassMonitor) {
                    threadPassMonitor.notifyAll();
                }
                threadExitMonitor.wait();
                System.out.println("leak exit");
            }
            synchronized (threadPassMonitor) {
                queue.removeFirst();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Deque<Thread> getQueue() {
        return queue;
    }

    public static void main(String[] args) {
        int numOfThreads = 5;
        BucketBarrier bucketBarrier = new BucketBarrier();
        List<Thread> threads = IntStream.range(0, numOfThreads)
                .mapToObj(i -> new Thread(bucketBarrier::arrived, "Thread " + i)).collect(toList());
        threads.forEach(Thread::start);
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < numOfThreads; ++i) {
            bucketBarrier.awaitDrop();
            bucketBarrier.leak();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Leak remained: " + bucketBarrier.getQueue().size());
    }
}
