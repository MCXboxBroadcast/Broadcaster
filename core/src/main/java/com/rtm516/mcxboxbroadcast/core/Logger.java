package com.rtm516.mcxboxbroadcast.core;

import net.raphimc.minecraftauth.util.logging.ILogger;

/**
 * A basic logger interface to allow for custom logger implementations and wrappers
 */
public interface Logger extends ILogger {
    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable ex);

    void debug(String message);

    Logger prefixed(String prefixString);
}
