package com.rtm516.mcxboxbroadcast.core.player;

public interface Player {

    long getLastLogOff();

    void setLastLogOff(long lastLogOff);

    int getJoinTimes();

    void setJoinTimes(int joinTimes);
}