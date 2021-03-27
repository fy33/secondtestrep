package org.springframework.aop.scope;

import org.springframework.aop.RawTargetAccess;

public class ScopedObject extends RawTargetAccess {

    Object getTargetObject();

    void removeFromScope();

}
