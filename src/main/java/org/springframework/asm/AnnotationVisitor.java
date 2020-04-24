package org.springframework.asm;

public abstract class AnnotationVisitor {
    protected final int api;


    protected AnnotationVisitor av;

    public AnnotationVisitor(final int api) {
        this(api, null);
    }


    @SuppressWarnings("deprecation")
    public AnnotationVisitor(final int api, final AnnotationVisitor annotationVisitor) {
        if (api != Opcodes.ASM7
                && api != Opcodes.ASM6
                && api != Opcodes.ASM5
                && api != Opcodes.ASM4
                && api != Opcodes.ASM8_EXPERIMENTAL) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        // SPRING PATCH: no preview mode check for ASM 8 experimental
        this.api = api;
        this.av = annotationVisitor;
    }


    public void visit(final String name, final Object value) {
        if (av != null) {
            av.visit(name, value);
        }
    }


    public void visitEnum(final String name, final String descriptor, final String value) {
        if (av != null) {
            av.visitEnum(name, descriptor, value);
        }
    }


    public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
        if (av != null) {
            return av.visitAnnotation(name, descriptor);
        }
        return null;
    }

    public AnnotationVisitor visitArray(final String name) {
        if (av != null) {
            return av.visitArray(name);
        }
        return null;
    }


    public void visitEnd() {
        if (av != null) {
            av.visitEnd();
        }
    }





}
