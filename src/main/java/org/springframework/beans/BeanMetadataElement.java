package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

public interface BeanMetadataElement {

    @Nullable
    default Object getSource() {
        return null;
    }
}
