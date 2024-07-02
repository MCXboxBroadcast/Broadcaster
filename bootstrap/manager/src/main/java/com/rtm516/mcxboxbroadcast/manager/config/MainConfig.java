package com.rtm516.mcxboxbroadcast.manager.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcxb")
public record MainConfig (
    boolean auth,
    UpdateTime updateTime,
    @Min(1)
    int workerThreads
) {
    @ConstructorBinding
    public MainConfig {
    }

    public record UpdateTime(
        @Min(5)
        int server,
        @Min(30)
        int session,
        @Min(30)
        int stats,
        @Min(20)
        int friend
    ) {
    }
}
