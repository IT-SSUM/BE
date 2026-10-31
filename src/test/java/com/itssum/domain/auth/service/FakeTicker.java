package com.itssum.domain.auth.service;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/** 시간을 직접 돌리는 Caffeine Ticker. */
class FakeTicker implements Ticker {

    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
        return nanos.get();
    }

    void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
