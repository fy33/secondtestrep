package org.springframework.boot.admin;

public interface SpringApplicationMXBean {
    boolean isReady();
    boolean isEmbeddedWebApplication();
    String getProperty(String key);
    void shutdown();

}
