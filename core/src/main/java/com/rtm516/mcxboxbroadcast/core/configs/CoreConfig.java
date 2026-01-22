package com.rtm516.mcxboxbroadcast.core.configs;

import com.rtm516.mcxboxbroadcast.core.Constants;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultNumeric;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.interfaces.meta.range.NumericRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public interface CoreConfig {
    @Comment("Core session settings")
    SessionConfig session();

    @Comment("Friend/follower list sync settings")
    FriendSyncConfig friendSync();

    @Comment("Notification settings (e.g., Slack/Discord webhook)")
    NotificationConfig notifications();

    @Comment("Enable debug logging")
    @ExcludePlatform(platforms = {"Extension"})
    @DefaultBoolean(false)
    boolean debugMode();

    @Comment("Suppresses \"Updated session!\" log into debug")
    @ExcludePlatform(platforms = {"Extension"})
    @DefaultBoolean(false)
    boolean suppressSessionUpdateMessage();

    @Comment("Do not change!")
    @SuppressWarnings("unused")
    default int configVersion() {
        return Constants.CONFIG_VERSION;
    }

    @ConfigSerializable
    interface SessionConfig {
        @Comment("""
            The IP address to broadcast, you likely want to change this to
            your servers public IP""")
        @ExcludePlatform(platforms = {"Standalone"})
        @DefaultString("auto")
        String remoteAddress();

        @Comment("""
            The port to broadcast, this should be left as auto unless your
            manipulating the port using network rules or reverse proxies""")
        @ExcludePlatform(platforms = {"Standalone"})
        @DefaultString("auto")
        String remotePort();

        @Comment("""
            The amount of time in seconds to update session information
            Warning: This can be no lower than 20 due to Xbox rate limits""")
        @DefaultNumeric(30)
        @NumericRange(from = 20, to = Integer.MAX_VALUE)
        int updateInterval();

        @Comment("""
            The amount of time in seconds to wait between initializing sub-sessions
            Warning: This can be no lower than 20 due to Xbox rate limits""")
        @DefaultNumeric(20)
        @NumericRange(from = 20, to = Integer.MAX_VALUE)
        int subSessionInitDelaySeconds();

        @Comment("Should we query the bedrock server to sync the session information")
        @ExcludePlatform(platforms = {"Extension"})
        @DefaultBoolean(true)
        boolean queryServer();

        @Comment("""
            This uses checker.geysermc.org for querying if the native ping fails
            This can be useful in the case of docker networks or routing problems causing the native ping to fail""")
        @ExcludePlatform(platforms = {"Extension"})
        @DefaultBoolean(false)
        boolean webQueryFallback();

        @Comment("Fallback to config values if all other server query methods fail")
        @ExcludePlatform(platforms = {"Extension"})
        @DefaultBoolean(false)
        boolean configFallback();

        @Comment("Extension-only overrides for the session data broadcast to Xbox Live")
        @ExcludePlatform(platforms = {"Standalone"})
        ExtensionOverride extensionOverride();

        @Comment("The data to broadcast over xbox live, this is the default if querying is enabled")
        @ExcludePlatform(platforms = {"Extension"})
        SessionInfo sessionInfo();

        @ConfigSerializable
        interface SessionInfo {
            @Comment("The host name to broadcast")
            @DefaultString("Geyser Test Server")
            String hostName();

            @Comment("The world name to broadcast")
            @DefaultString("GeyserMC Demo & Test Server")
            String worldName();

            @Comment("The current number of players")
            @DefaultNumeric(0)
            int players();

            @Comment("The maximum number of players")
            @DefaultNumeric(20)
            int maxPlayers();

            @Comment("The IP address of the server")
            @DefaultString("test.geysermc.org")
            String ip();

            @Comment("The port of the server")
            @DefaultNumeric(19132)
            @NumericRange(from = 1, to = 65535)
            int port();
        }

        @ConfigSerializable
        interface ExtensionOverride {
            @Comment("Enable overriding session info values in the extension")
            @DefaultBoolean(false)
            boolean enabled();

            @Comment("Override the host name to broadcast (leave empty to keep passthrough)")
            @DefaultString("")
            String hostName();

            @Comment("Override the world name to broadcast (leave empty to keep passthrough)")
            @DefaultString("")
            String worldName();

            @Comment("Override the current number of players (0 keeps passthrough)")
            @DefaultNumeric(0)
            int players();

            @Comment("Override the maximum number of players (0 keeps passthrough)")
            @DefaultNumeric(0)
            int maxPlayers();
        }
    }

    @ConfigSerializable
    interface FriendSyncConfig {
        @Comment("""
            The amount of time in seconds to update session information
            Warning: This can be no lower than 20 due to Xbox rate limits""")
        @DefaultNumeric(60)
        @NumericRange(from = 20, to = Integer.MAX_VALUE)
        int updateInterval();

        @Comment("Should we automatically follow people that follow us")
        @DefaultBoolean(true)
        boolean autoFollow();

        @Comment("Should we automatically unfollow people that no longer follow us")
        @DefaultBoolean(true)
        boolean autoUnfollow();

        @Comment("Should we automatically send an invite when a friend is added")
        @DefaultBoolean(true)
        boolean initialInvite();

        @Comment("Invite loop settings")
        InviteLoopConfig inviteLoop();

        @Comment("Friend expiry settings")
        ExpiryConfig expiry();

        @ConfigSerializable
        interface InviteLoopConfig {
            @Comment("Should we continuously invite everyone we follow")
            @DefaultBoolean(false)
            boolean enabled();

            @Comment("Delay in seconds between invites")
            @DefaultNumeric(60)
            @NumericRange(from = 1, to = Integer.MAX_VALUE)
            int delaySeconds();
        }

        @ConfigSerializable
        interface ExpiryConfig {
            @Comment("Should we unfriend people that haven't joined the server in a while")
            @DefaultBoolean(true)
            boolean enabled();

            @Comment("The amount of time in days before a friend is considered expired")
            @DefaultNumeric(15)
            @NumericRange(from = 1, to = Integer.MAX_VALUE)
            int days();

            @Comment("How often to check in seconds for expired friends")
            @DefaultNumeric(1800)
            @NumericRange(from = 1, to = Integer.MAX_VALUE)
            int check();
        }
    }

    @ConfigSerializable
    interface NotificationConfig {
        @Comment("Should we send a message to a slack webhook when the session is updated")
        @DefaultBoolean(false)
        boolean enabled();

        @Comment("""
            The webhook url to send the message to
            If you are using discord add "/slack" to the end of the webhook url""")
        @DefaultString("")
        String webhookUrl();

        @Comment("The message to send when the session is expired and needs to be updated")
        @DefaultString("""
            <!here> Xbox Session expired, sign in again to update it.
            
            Use the following link to sign in: %s
            Enter the code: %s""")
        String sessionExpiredMessage();

        @Comment("The message to send when a friend has restrictions in place that prevent them from being friends with our account")
        @DefaultString("""
            %s (%s) has restrictions in place that prevent them from being friends with our account.""")
        String friendRestrictionMessage();
    }
}
