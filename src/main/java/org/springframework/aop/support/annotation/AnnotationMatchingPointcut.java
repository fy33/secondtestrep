package org.springframework.aop.support.annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;

public class AnnotationMatchingPointcut {
    private final ClassFilter classFilter;

    private final MethodMatcher methodMatcher;

    public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType) {
        this.classFilter = new AnnotationClassFilter(classAnnotationType);
        this.methodMatcher = MethodMatcher.TRUE;
    }

    public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotatioonType, boolean checkInherited) {
        this.classFilter = new AnnotationClassFilter(classAnnotatioonType, checkInherited);
        this.methodMatcher = MethodMatcher.TRUE;
    }

    public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType, Class<? extends Annotation> methodAnnotationType) {
        Assert.isTrue(classAnnotationType != null || methodAnnotationType != null,
                "Either Class annotation type or Method annotation type needs to specified (or both)");
        if (classAnnotationType != null) {
            this.classFilter = new AnnotationClassFilter(classAnnotationType);
        } else {
            this.classFilter = ClassFilter.TRUE;
        }
        if (methodAnnotationType != null) {
            this.methodMatcher = new AnnotationMethodMatcher(methodAnnotationType);
        } else {
            this.methodMatcher = MethodMatcher.TRUE;
        }
    }

    public ClassFilter getClassFilter() {
        return this.classFilter;
    }

    public MethodMatcher getMethodMatcher() {
        return this.methodMatcher;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof AnnotationMatchingPointcut)) {
            return false;
        } else {
            AnnotationMatchingPointcut that = (AnnotationMatchingPointcut) other;
            return ObjectUtils.nullSafeEquals(that.classFilter, this.classFilter) &&
                    ObjectUtils.nullSafeEquals(that.methodMatcher, this.methodMatcher);
        }
    }

    public int hashCode() {
        int code = 17;
        if (this.classFilter != null) {
            code = 37 * code + this.classFilter.hashCode();
        }
        if (this.methodMatcher != null) {
            code = 37 * code + this.methodMatcher.hashCode();
        }
        return code;
    }

    public String toString()
    {
        return "AnnotationMatchingPointcut:"+this.classFilter+","+this.methodMatcher;
    }

    public static AnnotationMatchingPointcut forClassAnnotation(Class<? extends Annotation> annotationType)
    {
        Assert.notNull(annotationType,"Annotation type must be null");
        return new AnnotationMatchingPointcut(annotationType);
    }

    public static AnnotationMatchingPointcut forMethodAnnotation(Class<? extends Annotation> annotationType)
    {
        Assert.notNull(annotationType,"Annotation type must not be null");
        return new AnnotationMatchingPointcut((Class)null,annotationType);
    }
}
