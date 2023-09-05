package com.rtm516.mcxboxbroadcast.bootstrap.geyser.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.player.Player;

public class PlayerImpl implements Player {

    @JsonProperty("joinTimes") private int joinTimes;
    @JsonProperty("lastLogOff") private long lastLogOff;

    @Override
    public long getLastLogOff() {
        return this.lastLogOff;
    }

    @Override
    public void setLastLogOff(long lastLogOff) {
        this.lastLogOff = lastLogOff;
    }

    @Override
    public int getJoinTimes() {
        return this.joinTimes;
    }

    @Override
    public void setJoinTimes(int joinTimes) {
        this.joinTimes = joinTimes;
    }
}