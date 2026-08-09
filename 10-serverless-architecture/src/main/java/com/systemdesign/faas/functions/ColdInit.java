package com.systemdesign.faas.functions;

/**
 * Small shared helper so every function's constructor and simulated handler work sleep the same
 * uninterruptible way. Not part of the public API of this package — each function's constructor
 * uses this to spend real time doing "init work" so a fresh instance is genuinely, measurably
 * more expensive to build than reusing a warm one.
 */
final class ColdInit {

    private ColdInit() {
    }

    static void simulateWork(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
