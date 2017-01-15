package C10TODO.cvCounter;

import java.util.Map;

/**
 * Created by bulat on 12.01.17.
 */
interface Counters {
    void increment(String tag); // called often from different threads

    Map<String, Long> getCountersAndClear(); // called rarely (for example once per minute by a scheduled thread)
}
