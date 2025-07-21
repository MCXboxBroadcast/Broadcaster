package org.ice4j.ice;

import java.time.Duration;

/**
 * This class is a stub for the <a href="https://github.com/jitsi/ice4j/blob/b64c86fba61cada49a68b8f17bf8d76abcdbd11b/src/main/kotlin/org/ice4j/ice/AgentConfig.kt">AgentConfig.kt</a>
 * This is here so we can exclude Kotlin from the build
 *
 * We set some defaults here too for when the class is used
 */
public class AgentConfig {
    public static final AgentConfig config = new AgentConfig();

    public Duration getConsentFreshnessInterval() {
        return Duration.ofSeconds(15);
    }

    public boolean getRandomizeConsentFreshnessInterval() {
        return true;
    }

    public Duration getConsentFreshnessOriginalWaitInterval() {
        return Duration.ofMillis(500);
    }

    public Duration getConsentFreshnessMaxWaitInterval() {
        return Duration.ofMillis(500);
    }

    public int getMaxConsentFreshnessRetransmissions() {
        return 30;
    }

    public Duration getTerminationDelay() {
        return Duration.ofSeconds(3);
    }

    public int getMaxCheckListSize() {
        return 100;
    }

    public String getSoftware() {
        return null;
    }

    public boolean getRedactRemoteAddresses() {
        return false;
    }

    public boolean getUseComponentSocket() {
        return false;
    }
}
