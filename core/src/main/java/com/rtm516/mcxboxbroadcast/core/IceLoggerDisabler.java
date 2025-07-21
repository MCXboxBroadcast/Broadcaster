package com.rtm516.mcxboxbroadcast.core;

import org.ice4j.ice.harvest.MappingCandidateHarvesters;
import org.jitsi.utils.logging2.LoggerImpl;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.logging.Level;

public class IceLoggerDisabler {
    public static void disable(Logger logger) {
        // Disable the Jitsi loggers
        try {
            Function<String, java.util.logging.Logger> newLoggerFactory = s -> {
                java.util.logging.Logger newLogger = java.util.logging.Logger.getLogger(s);
                newLogger.setLevel(Level.OFF);
                return newLogger;
            };

            Field loggerFactoryField = LoggerImpl.class.getDeclaredField("loggerFactory");
            loggerFactoryField.setAccessible(true);
            loggerFactoryField.set(null, newLoggerFactory);

            // Disable the MappingCandidateHarvesters logger
            disableClassLogger(MappingCandidateHarvesters.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to disable Jitsi logger, may see more logs than expected", e);
        }
    }

    private static void disableClassLogger(Class<?> clazz) throws IllegalAccessException, NoSuchFieldException {
        Field loggerField = clazz.getDeclaredField("logger");
        loggerField.setAccessible(true);
        ((java.util.logging.Logger)loggerField.get(null)).setLevel(Level.OFF);
    }
}
