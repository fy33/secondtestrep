package org.springframework.aop.support.annotation;

import jdk.nashorn.api.scripting.ClassFilter;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

public class AnnotationClassFilter implements ClassFilter {

    private final Class<? extends Annotation> annotationType;

    private final boolean checkInherited;

    public AnnotationClassFilter(Class<? extends Annotation> annotationType)
    {
        this(annotationType,false);
    }

    public AnnotationClassFilter(Class<?extends Annotation> annotationType,boolean checkInherited )
    {
        Assert.notNull(annotationType,"Annotation type must not be null");
        this.annotationType=annotationType;
        this.checkInherited=checkInherited;
    }

    public boolean equals(Object other)
    {
        if(this==other)
        {
            return true;
        }
        else if(!(other instanceof AnnotationClassFilter))
        {
            return false;
        }else {
            AnnotationClassFilter otherCf=(AnnotationClassFilter)other;
            return this.annotationType.equals(otherCf.annotationType)&& this.checkInherited;
        }
    }

    public int hashCode()
    {
        return this.annotationType.hashCode();
    }

    public String toString()
    {
        return this.getClass().getName()+":"+this.annotationType;
    }
}
