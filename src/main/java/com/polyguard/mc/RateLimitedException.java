package com.polyguard.mc;

import java.io.IOException;

public class RateLimitedException extends IOException {
    private final String retryAfter;

    public RateLimitedException(String retryAfter) {
        super("Rate limited, retry in " + retryAfter + "s");
        this.retryAfter = retryAfter;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
