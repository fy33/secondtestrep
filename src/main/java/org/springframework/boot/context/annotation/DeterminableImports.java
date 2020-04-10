package org.springframework.boot.context.annotation;

import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

@FunctionalInterface
public interface DeterminableImports {
    Set<Object> determineImports(AnnotationMetadata metadata);
}
