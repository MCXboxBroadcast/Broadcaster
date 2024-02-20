package com.rtm516.mcxboxbroadcast.core;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
    public static String getStackTrace(Throwable ex) {
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
