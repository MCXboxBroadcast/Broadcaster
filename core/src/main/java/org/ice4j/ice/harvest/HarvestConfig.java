package org.ice4j.ice.harvest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is a stub for the <a href="https://github.com/jitsi/ice4j/blob/b64c86fba61cada49a68b8f17bf8d76abcdbd11b/src/main/kotlin/org/ice4j/ice/harvest/HarvestConfig.kt">HarvestConfig.kt</a>
 * This is here so we can exclude Kotlin from the build
 *
 * We set some defaults here too for when the class is used
 */
public class HarvestConfig {
    public static final HarvestConfig config = new HarvestConfig();

    public boolean useLinkLocalAddresses() {
        return true;
    }

    public Integer udpReceiveBufferSize() {
        return null;
    }

    public int udpSocketPoolSize() {
        return 0;
    }

    public boolean useIpv6() {
        return true;
    }

    public boolean useDynamicPorts() {
        return true;
    }

    public Duration timeout() {
        return Duration.ofSeconds(15);
    }

    public List<String> stunMappingCandidateHarvesterAddresses() {
        return new ArrayList<>();
    }

    public boolean enableAwsHarvester() {
        return false;
    }

    public boolean forceAwsHarvester() {
        return false;
    }

    public Set<String> getStaticMappings() {
        return Set.of();
    }

    public List<String> getAllowedAddresses() {
        return List.of();
    }

    public List<String> getBlockedAddresses() {
        return List.of();
    }

    public List<String> getAllowedInterfaces() {
        return List.of();
    }

    public List<String> getBlockedInterfaces() {
        return List.of();
    }
}
