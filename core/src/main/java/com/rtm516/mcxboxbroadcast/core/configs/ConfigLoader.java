package com.rtm516.mcxboxbroadcast.core.configs;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.objectmapping.meta.Processor;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.Arrays;

import static org.spongepowered.configurate.NodePath.path;
import static org.spongepowered.configurate.transformation.TransformAction.rename;

public class ConfigLoader {
    private static final ConfigurationTransformation.Versioned TRANSFORMER = ConfigurationTransformation.versionedBuilder()
        .versionKey("config-version")
        .addVersion(2, ConfigurationTransformation.builder()
            // Extension only settings
            .addAction(path("remote-address"), moveTo("session"))
            .addAction(path("remote-port"), moveTo("session"))
            .addAction(path("update-interval"), moveTo("session"))

            // Standalone only settings
            .addAction(path("suppress-session-update-info"), rename("suppress-session-update-message"))
            .addAction(path("debug-log"), rename("debug-mode"))

            // Shared settings
            .addAction(path("slack-webhook"), rename("notifications"))
            .addAction(path("friend-sync", "should-expire"), renameAndMove("friend-sync", "expiry", "enabled"))
            .addAction(path("friend-sync", "expire-days"), renameAndMove("friend-sync", "expiry", "days"))
            .addAction(path("friend-sync", "expire-check"), renameAndMove("friend-sync", "expiry", "check"))

            .build())
        .build();

    public static CoreConfig loadConfig(File configFile, String platformName) throws ConfigurateException {
        YamlConfigurationLoader loader = createLoader(configFile, platformName);

        CommentedConfigurationNode node = loader.load();
        boolean originallyEmpty = !configFile.exists() || node.isNull();

        int currentVersion = TRANSFORMER.version(node);
        TRANSFORMER.apply(node);
        int newVersion = TRANSFORMER.version(node);

        CoreConfig config = node.get(CoreConfig.class);

        // Keep ordering
        CommentedConfigurationNode newRoot = CommentedConfigurationNode.root(loader.defaultOptions());
        newRoot.set(config);

        if (originallyEmpty || currentVersion != newVersion) {
            loader.save(newRoot);
        }

        return config;
    }

    private static YamlConfigurationLoader createLoader(File configFile, String platformName) {
        return YamlConfigurationLoader.builder()
            .file(configFile)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(options -> InterfaceDefaultOptions.addTo(options, builder -> {
                builder.addProcessor(ExcludePlatform.class, excludePlatform(platformName));
            }))
            .build();
    }

    private static Processor.Factory<ExcludePlatform, Object> excludePlatform(String thisPlatform) {
        return (data, fieldType) -> (value, destination) -> {
            for (String platform : data.platforms()) {
                if (thisPlatform.equals(platform)) {
                    //noinspection DataFlowIssue
                    destination.parent().removeChild(destination.key());
                    break;
                }
            }
        };
    }

    private static TransformAction renameAndMove(String... newPath) {
        return ((path, value) -> Arrays.stream(newPath).toArray());
    }

    private static TransformAction moveTo(String... newPath) {
        return (path, value) -> {
            Object[] arr = path.array();
            if (arr.length == 0) {
                throw new ConfigurateException(value, "The root node cannot be renamed!");
            } else {
                // create a new array with space for newPath segments + the original last segment
                Object[] result = new Object[newPath.length + 1];
                System.arraycopy(newPath, 0, result, 0, newPath.length);
                result[newPath.length] = arr[arr.length - 1];
                return result;
            }
        };
    }
}
