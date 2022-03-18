package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.Logger;

public class LoggerImpl implements Logger {
    @Override
    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    @Override
    public void warning(String message) {
        System.out.println("[WARN] " + message);
    }

    @Override
    public void error(String message) {
        System.out.println("[ERROR] " + message);
    }

    @Override
    public void error(String message, Throwable ex) {
        System.out.println("[ERROR] " + message + ": " + ex.getMessage());
    }

    @Override
    public void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }
}
