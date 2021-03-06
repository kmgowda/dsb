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
import io.sbk.api.Time;

import javax.annotation.concurrent.NotThreadSafe;

/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class ArrayLatencyRecorder extends LatencyWindow {
    final private long[] latencies;
    private int minIndex;
    private int maxIndex;

    ArrayLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax, double[] percentiles, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        final int size = (int) Math.min(highLatency-lowLatency, Integer.MAX_VALUE);
        this.latencies = new long[size];
        this.minIndex = size;
        this.maxIndex = 0;
    }

    @Override
    public void reset(long startTime) {
        super.reset(startTime);
        this.maxIndex = 0;
        this.minIndex = Integer.MAX_VALUE;
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

        for (int i = minIndex; i < Math.min(latencies.length, this.maxIndex+1); i++) {

            if (latencies[i] > 0) {

                if (copyLatencies != null) {
                    copyLatencies.copyLatency(i, latencies[i]);
                }

                while (index < values.length) {
                    if (percentileIds[index] >= cur && percentileIds[index] < (cur + latencies[i])) {
                        values[index] = i + lowLatency;
                        index += 1;
                    } else {
                        break;
                    }
                }
                cur += latencies[i];
                latencies[i] = 0;
            }
        }
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
            final int index = (int) (latency - this.lowLatency);
            this.minIndex = Math.min(this.minIndex, index);
            this.maxIndex = Math.max(this.maxIndex, index);
            this.latencies[index] += events;
        }
    }
}
