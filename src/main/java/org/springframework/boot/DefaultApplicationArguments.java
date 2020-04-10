package org.springframework.boot;

import java.util.List;
import java.util.Set;

public class DefaultApplicationArguments implements ApplicationArguments {

    @Override
    public String[] getSourceArgs() {
        return new String[0];
    }

    @Override
    public Set<String> getOptionNames() {
        return null;
    }

    @Override
    public boolean containsOption(String name) {
        return false;
    }

    @Override
    public List<String> getOptionValues(String name) {
        return null;
    }

    @Override
    public List<String> getNonOptionArgs() {
        return null;
    }
}
