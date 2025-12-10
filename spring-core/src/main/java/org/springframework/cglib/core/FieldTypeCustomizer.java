package org.springframework.cglib.core;

import org.springframework.asm.Type;

/**
 * Customizes key types for {@link KeyFactory} right in constructor.
 */
public interface FieldTypeCustomizer extends KeyFactoryCustomizer {
    /**
     * Customizes {@code this.FIELD_0 = ?} assignment in key constructor
     * @param e code emitter
     * @param index parameter index
     * @param type parameter type
     */
    void customize(CodeEmitter e, int index, Type type);

    /**
     * Computes type of field for storing given parameter
     * @param index parameter index
     * @param type parameter type
     */
    Type getOutType(int index, Type type);
}
