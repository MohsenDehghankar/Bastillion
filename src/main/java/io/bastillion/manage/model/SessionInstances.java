package io.bastillion.manage.model;

import io.bastillion.manage.util.CommandLine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionInstances {
    Map<Integer, CommandLine> instancesCommandLine = new ConcurrentHashMap<>();

    public Map<Integer, CommandLine> getInstancesCommandLine() {
        return instancesCommandLine;
    }

    public void setInstancesCommandLine(Map<Integer, CommandLine> instancesCommandLine) {
        this.instancesCommandLine = instancesCommandLine;
    }
}
