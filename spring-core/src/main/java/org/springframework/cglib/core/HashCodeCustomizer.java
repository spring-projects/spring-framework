package org.springframework.cglib.core;

import org.springframework.asm.Type;

public interface HashCodeCustomizer extends KeyFactoryCustomizer {
    /**
     * Customizes calculation of hashcode
     * @param e code emitter
     * @param type parameter type
     */
    boolean customize(CodeEmitter e, Type type);
}
