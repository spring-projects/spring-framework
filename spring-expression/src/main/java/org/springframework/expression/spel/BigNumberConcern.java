package org.springframework.expression.spel;

/**
 * Marker of "big number" ({@link java.math.BigInteger} and {@link java.math.BigDecimal}) concern extension for SpEL engine.
 * Solution extends related elements similar to how L/l suffix support is done.
 * It covers: HEX, non-HEX(including exponent) literals.
 * Requirement: <a href="https://github.com/spring-projects/spring-framework/issues/21758">Suffix support for BigInteger in SPEL [SPR-17225] #21758</a>
 */
public @interface BigNumberConcern {
}