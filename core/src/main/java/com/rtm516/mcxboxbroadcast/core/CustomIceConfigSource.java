package com.rtm516.mcxboxbroadcast.core;

import kotlin.jvm.functions.Function1;
import kotlin.reflect.KType;
import org.jetbrains.annotations.NotNull;
import org.jitsi.config.JitsiConfig;
import org.jitsi.metaconfig.ConfigSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomIceConfigSource implements ConfigSource {
    /**
     * The ice properties
     *
     * This needs to be a mix of legacy and new keys
     * Commented out ones are not requested during use
     *
     * Based on https://github.com/jitsi/ice4j/blob/824cd4b18cba5f6d130125080efe8585e78ecba2/src/main/resources/reference.conf
     */
    private static final Map<String, String> config = new HashMap<>() {{
        // ICE settings
//        put("ice4j.ice.max-check-list-size", "100");
        put("org.ice4j.MAX_CHECK_LIST_SIZE", "100");

        put("ice4j.ice.termination-delay", "3000"); // 3 seconds = 3000 milliseconds

        // Software attribute, blank so its removed
//        put("ice4j.software", "");
        put("org.ice4j.SOFTWARE", "");

        // Component socket usage
//        put("ice4j.use-component-socket", "true");

        // Redact remote addresses
//        put("ice4j.redact-remote-addresses", "false");
        put("org.ice4j.REDACT_REMOTE_ADDRESSES", "false");

        // Consent freshness settings
//        put("ice4j.consent-freshness.interval", "15000"); // 15 seconds = 15000 milliseconds
//        put("ice4j.consent-freshness.original-wait-interval", "500"); // 500 milliseconds
//        put("ice4j.consent-freshness.max-wait-interval", "500"); // 500 milliseconds
//        put("ice4j.consent-freshness.max-retransmissions", "30");
        put("ice4j.consent-freshness.randomize-interval", "true");

        // Consent freshness settings - Legacy
        put("org.ice4j.ice.CONSENT_FRESHNESS_INTERVAL", "15000"); // 15 seconds = 15000 milliseconds
        put("org.ice4j.ice.CONSENT_FRESHNESS_WAIT_INTERVAL", "500"); // 500 milliseconds
        put("org.ice4j.ice.CONSENT_FRESHNESS_MAX_WAIT_INTERVAL", "500"); // 500 milliseconds
        put("org.ice4j.ice.CONSENT_FRESHNESS_MAX_RETRANSMISSIONS", "30");

        // Harvest settings
        put("ice4j.harvest.use-ipv6", "true");
        put("ice4j.harvest.use-link-local-addresses", "true");
        put("ice4j.harvest.timeout", "15000"); // 15 seconds = 15000 milliseconds

        // UDP harvester settings
//        put("ice4j.harvest.udp.use-dynamic-ports", "true");
        put("org.ice4j.ice.harvest.USE_DYNAMIC_HOST_HARVESTER", "true");

        // Allowed and blocked addresses/interfaces
        put("ice4j.harvest.allowed-addresses", "");
        put("ice4j.harvest.blocked-addresses", "");
        put("ice4j.harvest.allowed-interfaces", "");
        put("ice4j.harvest.blocked-interfaces", "");

        // AWS mapping harvester settings
        put("ice4j.harvest.mapping.aws.enabled", "false");
//        put("ice4j.harvest.mapping.aws.force", "false");

        // STUN mapping harvester settings
        put("ice4j.harvest.mapping.stun.addresses", "");

        // Static mappings
        put("ice4j.harvest.mapping.static-mappings", "");
    }};

    @NotNull
    @Override
    public String getDescription() {
        return "";
    }

    @NotNull
    @Override
    public Function1<String, Object> getterFor(@NotNull KType kType) {
        return s -> {
            System.out.println("getterFor: " + s + " " + kType);

            String value = config.get(s);
            if (value != null) {
                String typeName = kType.getClassifier().toString();
                // Remove the "class " prefix
                typeName = typeName.replaceFirst("class ", "");

                if (typeName.startsWith("kotlin.collections.List")) {
                    if (value.isEmpty()) {
                        return new ArrayList<>();
                    } else {
                        System.out.println("List not empty: " + s + " " + value + " " + kType);
                        return null;
                    }
                }

                Object out = switch (typeName) {
                    case "kotlin.Boolean" -> Boolean.parseBoolean(value);
                    case "kotlin.Int" -> Integer.parseInt(value);
                    case "kotlin.Long" -> Long.parseLong(value);
                    case "kotlin.Double" -> Double.parseDouble(value);
                    case "java.time.Duration" -> Duration.ofMillis(Long.parseLong(value));
                    default -> {
                        System.out.println("Unknown type: " + typeName);
                        yield null;
                    }
                };

                if (out == null) {
                    System.out.println("Failed to parse: " + s + " " + value + " " + kType);
                }

                return out;
            }

            System.out.println("Unknown key: " + s + " " + kType);
            return null;
        };
    }

    @NotNull
    @Override
    public String getName() {
        return "";
    }

    public static void install() {
        JitsiConfig.Companion.useDebugNewConfig(new CustomIceConfigSource());
    }
}
