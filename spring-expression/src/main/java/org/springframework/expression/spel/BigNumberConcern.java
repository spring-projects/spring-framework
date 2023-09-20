package org.springframework.expression.spel;

/**
 * Marker of BigInteger and BigDecimal concern extension of SpEL engine.
 * Solution extends related elements similar to how L/l suffix support is done.
 * It covers 2 cases: HEX and non-HEX literals.
 * Requirement: <a href="https://github.com/spring-projects/spring-framework/issues/21758">Suffix support for BigInteger in SPEL [SPR-17225] #21758</a>
 */
public @interface BigNumberConcern {
}