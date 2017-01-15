package C10TODO.cvCounter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

/**
 * Created by bulat on 12.01.17.
 */
public class CVCounter implements Counters {
    private Map<String, Long> currentMap = new HashMap<>();
    private Map<String, Long> result = new HashMap<>();

    public synchronized void increment(String tag) {
        Long value = currentMap.putIfAbsent(tag, 1L);
        if (value != null) {
            currentMap.put(tag, value + 1);
        }
    }

    public synchronized Map<String, Long> getCountersAndClear() {
        result.clear();
        Map<String, Long> temp = currentMap;
        currentMap = result;
        result = temp;
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
        System.out.printf("%d - real%n", realCount + results.get(tagName));
        System.out.println(duration + " ms");
    }
}
