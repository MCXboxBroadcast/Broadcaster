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

    /**
     * Get the error message for the Xbox error code
     * <p>
     * Ported from <a href="https://github.com/HashimTheArab/gophertunnel/commit/b16d5d5f387a9ccee184e87bde0b514cb484d60d">HashimTheArab/gophertunnel</a>
     *
     * @param code The Xbox error code
     * @return The error message for the Xbox error code
     */
    public static String getXboxErrorCode(String code) {
        return switch (code) {
            case "2148916227" ->
                "Your account was banned by Xbox for violating one or more Community Standards for Xbox and is unable to be used.";
            case "2148916229" ->
                "Your account is currently restricted and your guardian has not given you permission to play online. Login to https://account.microsoft.com/family/ and have your guardian change your permissions.";
            case "2148916233" ->
                "Your account currently does not have an Xbox profile. Please create one at https://www.xbox.com/live";
            case "2148916234" -> "Your account has not accepted Xbox's Terms of Service. Please login and accept them.";
            case "2148916235" ->
                "Your account resides in a region that Xbox has not authorized use from. Xbox has blocked your attempt at logging in.";
            case "2148916236" ->
                "Your account requires proof of age. Please login to https://login.live.com/login.srf and provide proof of age.";
            case "2148916237" ->
                "Your account has reached its limit for playtime. Your account has been blocked from logging in.";
            case "2148916238" ->
                "The account date of birth is under 18 years and cannot proceed unless the account is added to a family by an adult.";
            default -> "";
        };
    }
}
