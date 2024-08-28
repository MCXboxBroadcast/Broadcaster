package com.rtm516.mcxboxbroadcast.core;

import net.raphimc.minecraftauth.util.logging.ILogger;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    /**
     * Helper to get the stack trace as a string
     *
     * @param ex the exception
     * @return the stack trace as a string
     */
    default String getStackTrace(Throwable ex) {
        // Create a PrintWriter to write the stack trace to
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Write the stack trace to the PrintWriter
        ex.printStackTrace(pw);

        // Close the PrintWriter
        pw.close();

        // Return the stack trace as a string
        return sw.toString();
    }
}
