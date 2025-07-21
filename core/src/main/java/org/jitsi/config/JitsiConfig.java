package org.jitsi.config;

import org.jitsi.metaconfig.ConfigSource;

/**
 * This class is a stub for the <a href="https://github.com/jitsi/jicoco/blob/9e95588e283cbc643c621f779cb43f1d455361de/jicoco-config/src/main/kotlin/org/jitsi/config/JitsiConfig.kt">JitsiConfig.kt</a>
 * This is here so we can exclude Kotlin from the build
 *
 * Flag any use of this class as a warning, as we replace all needed classes with stubs
 */
public class JitsiConfig {
    public static final Companion Companion = new Companion();

    public static class Companion {
        public ConfigSource getNewConfig() {
            throw new RuntimeException("Kotlin config was requested!");
        }
    }
}
