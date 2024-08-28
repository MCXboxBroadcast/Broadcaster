package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Logger;

import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;

public class IceLogger implements org.jitsi.utils.logging2.Logger {
    private final Logger logger;

    public IceLogger(Logger logger) {
        this.logger = logger.prefixed("Ice4J");
    }


    @Override
    public org.jitsi.utils.logging2.Logger createChildLogger(String name, Map<String, String> context) {
        return this;
    }

    @Override
    public org.jitsi.utils.logging2.Logger createChildLogger(String name) {
        return this;
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {

    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {

    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {

    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(Object msg) {

    }

    @Override
    public void trace(Supplier<String> msgSupplier) {

    }

    @Override
    public boolean isDebugEnabled() {
        return true; // We handle this in the logger so just claim its always enabled
    }

    @Override
    public void debug(Object msg) {
        logger.debug(msg.toString());
    }

    @Override
    public void debug(Supplier<String> msgSupplier) {
        logger.debug(msgSupplier.get());
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(Object msg) {
        logger.debug(msg.toString());
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        logger.debug(msgSupplier.get());
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(Object msg) {
        logger.debug(msg.toString());
    }

    @Override
    public void warn(Supplier<String> msgSupplier) {
        logger.debug(msgSupplier.get());
    }

    @Override
    public void warn(Object msg, Throwable t) {
        logger.debug(msg.toString() + "\n" + logger.getStackTrace(t));
    }

    @Override
    public void error(Object msg) {
        logger.error(msg.toString());
    }

    @Override
    public void error(Supplier<String> msgSupplier) {
        logger.error(msgSupplier.get());
    }

    @Override
    public void error(Object msg, Throwable t) {
        logger.error(msg.toString(), t);
    }

    @Override
    public void setLevelError() {

    }

    @Override
    public void setLevelWarn() {

    }

    @Override
    public void setLevelInfo() {

    }

    @Override
    public void setLevelDebug() {

    }

    @Override
    public void setLevelTrace() {

    }

    @Override
    public void setLevelAll() {

    }

    @Override
    public void setLevelOff() {

    }

    @Override
    public void setLevel(Level level) {

    }

    @Override
    public Level getLevel() {
        return null;
    }

    @Override
    public void addContext(Map<String, String> addedContext) {

    }

    @Override
    public void addContext(String key, String value) {

    }
}
