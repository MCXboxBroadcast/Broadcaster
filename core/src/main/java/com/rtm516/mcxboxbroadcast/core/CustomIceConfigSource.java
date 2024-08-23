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
    public String getName() {
        return "";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "";
    }

    @NotNull
    @Override
    public Function1<String, Object> getterFor(@NotNull KType kType) {
        // We only need to do this once, not every call
        String typeName = cleanType(kType);

        return s -> {
            String value = config.get(s);
            if (value != null) {
                // Handle the list type, should always be empty
                if (typeName.startsWith("list")) {
                    if (value.isEmpty()) {
                        return new ArrayList<>();
                    } else {
                        return null;
                    }
                }

                // Handle the other types
                return switch (typeName) {
                    case "boolean" -> Boolean.parseBoolean(value);
                    case "int" -> Integer.parseInt(value);
                    case "long" -> Long.parseLong(value);
                    case "double" -> Double.parseDouble(value);
                    case "duration" -> Duration.ofMillis(Long.parseLong(value));
                    default -> null;
                };
            }

            return null;
        };
    }

    /**
     * Normalise the type names
     *
     * class kotlin.Long -> long
     * class kotlin.collections.List -> list
     * long (Kotlin reflection is not available) -> long
     * interface java.util.List (Kotlin reflection is not available) -> list
     *
     * @param type The kotlin type to clean
     * @return The cleaned type name
     */
    private String cleanType(KType type) {
        String typeName = type.getClassifier().toString();

        // Get the real type name
        String[] parts = typeName.split(" ");
        if (parts[1].startsWith("(")) {
            typeName = parts[0];
        } else {
            typeName = parts[1];
        }

        // Get the last part of the type name
        parts = typeName.split("\\.");
        typeName = parts[parts.length - 1];
        typeName = typeName.toLowerCase();

        return typeName;
    }

    public static void install() {
        JitsiConfig.Companion.useDebugNewConfig(new CustomIceConfigSource());
    }
}
