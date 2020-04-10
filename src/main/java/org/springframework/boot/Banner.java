package org.springframework.boot;

import org.springframework.core.env.Environment;

import java.io.PrintStream;

@FunctionalInterface
public interface Banner {
   void printBanner(Environment environment, Class<?> sourceClass, PrintStream out);
   enum Mode{
       OFF,
       CONSOLE,
       LOG,
   }
}
