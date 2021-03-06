/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.CloneLatencies;
import io.sbk.api.Config;
import io.sbk.api.Time;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Iterator;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class HashMapLatencyRecorder extends LatencyWindow {
    final public HashMap<Long, Long> latencies;
    final public int maxHashMapSizeMB;
    final public long maxHashMapSizeBytes;
    final public int incBytes;
    public long hashMapBytesCount;

    HashMapLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                           double[] percentiles, Time time, int maxHashMapSizeMB) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        this.latencies = new HashMap<>();
        this.maxHashMapSizeMB = maxHashMapSizeMB;
        this.maxHashMapSizeBytes = ((long) maxHashMapSizeMB) * Config.BYTES_PER_MB;
        this.incBytes = Config.LATENCY_VALUE_SIZE_BYTES * 2;
        this.hashMapBytesCount = 0;
    }

    @Override
    public  boolean isOverflow() {
        return (this.hashMapBytesCount > this.maxHashMapSizeBytes ) || super.isOverflow();
    }

    @Override
    public long[] getPercentiles(CloneLatencies copyLatencies) {
        final long[] values = new long[percentileFractions.length];
        final long[] percentileIds = new long[percentileFractions.length];
        long cur = 0;
        int index = 0;

        if (copyLatencies != null) {
            copyLatencies.updateLatencyRecords(this);
        }

        for (int i = 0; i < percentileIds.length; i++) {
            percentileIds[i] = (long) (validLatencyRecords * percentileFractions[i]);
        }

        Iterator<Long> keys =  latencies.keySet().stream().sorted().iterator();
        while (keys.hasNext()) {
            final long key  = keys.next();
            final long val = latencies.get(key);
            final long next =  cur + val;

            if (copyLatencies != null) {
                copyLatencies.copyLatency(key, val);
            }

            while (index < values.length) {
                if (percentileIds[index] >= cur && percentileIds[index] <  next) {
                    values[index] = key;
                    index += 1;
                } else {
                    break;
                }
            }
            cur = next;
            latencies.remove(key);
        }
        hashMapBytesCount = 0;
        return values;
    }

    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     */
    @Override
    public void record(long startTime, int bytes, int events, long latency) {
        if (record(bytes, events, latency)) {
            Long val = latencies.get(latency);
            if (val == null) {
                val = 0L;
                hashMapBytesCount += incBytes;
            }
            latencies.put(latency, val + events);
        }
    }
}
