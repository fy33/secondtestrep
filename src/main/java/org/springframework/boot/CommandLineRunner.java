package org.springframework.boot;

@FunctionalInterface
public interface CommandLineRunner {
    void run(String... args) throws Exception;
}
