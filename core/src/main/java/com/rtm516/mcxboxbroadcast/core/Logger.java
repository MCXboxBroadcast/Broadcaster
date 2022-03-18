package com.rtm516.mcxboxbroadcast.core;

/**
 * A basic logger interface to allow for custom logger implementations and wrappers
 */
public interface Logger {
    void info(String message);

    void warning(String message);

    void error(String message);

    void error(String message, Throwable ex);

    void debug(String message);
}
