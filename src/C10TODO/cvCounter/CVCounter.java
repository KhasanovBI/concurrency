package C10TODO.cvCounter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

/**
 * Created by bulat on 12.01.17.
 */
public class CVCounter implements Counters {
    private volatile Map<String, LongAdder> currentMap = new ConcurrentHashMap<>();
    private Map<String, Long> result = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public void increment(String tag) {
        readLock.lock();
        LongAdder value = currentMap.get(tag);
        if (value == null) {
            LongAdder longAdder = new LongAdder();
            longAdder.increment();
            value = currentMap.putIfAbsent(tag, longAdder);

        }
        if (value != null) {
            value.increment();
        }
        readLock.unlock();
    }

    public Map<String, Long> getCountersAndClear() {
        writeLock.lock();
        result.clear();
        currentMap.replaceAll((s, longAdder) -> {
            result.put(s, longAdder.longValue());
            return new LongAdder();
        });
        writeLock.unlock();
        return result;
    }

    public static void main(String[] args) {
        long start = currentTimeMillis();
        int numOfThreads = 100;
        String tagName = "tag";
        int numOfIterations = 100000;
        CVCounter cvCounter = new CVCounter();
        List<Thread> threads = IntStream.range(0, numOfThreads)
                .mapToObj(i -> new Thread(() -> {
                    for (int j = 0; j < numOfIterations; ++j) {
                        cvCounter.increment(tagName);
                    }
                }, "thread" + i)).collect(toList());
        threads.forEach(Thread::start);
        Map<String, Long> results = cvCounter.getCountersAndClear();
        // Ненадежно так тестировать, но для полуавтоматического режима сойдет.
        long realCount = results.get(tagName);
        System.out.println(results);
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long duration = currentTimeMillis() - start;
        System.out.println(numOfIterations * numOfThreads + " - expected");
        results = cvCounter.getCountersAndClear();
        System.out.println(results);
        System.out.println(realCount + results.get(tagName) + " - real");
        System.out.println(duration + " ms");
    }
}
