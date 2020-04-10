package org.springframework.boot;

import java.util.List;
import java.util.Set;

public interface ApplcationArguments {
    String[] getSourceArgs();
    Set<String> getOptionNames();
    boolean containsOption(String name);
    List<String> getOptionValues(String name);
    List<String> getNonOptionArgs();
}
