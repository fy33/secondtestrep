package org.springframework.core.env;

public interface Environment extends PropertyResolver {
    String[] getActiveProfiles();

    String[] getDefaultProfiles();

    @Deprecated
    boolean acceptsProfiles(String... profiles);

    boolean acceptsProfiles(Profiles profiles);

}
