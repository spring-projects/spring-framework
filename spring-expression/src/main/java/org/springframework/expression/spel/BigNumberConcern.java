/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

/**
 * Marker of "big number" ({@link java.math.BigInteger} and {@link java.math.BigDecimal}) concern extension for SpEL engine.
 * Solution extends related elements similar to how L/l suffix support is done.
 * It covers: HEX, non-HEX(including exponent) literals.
 * Requirement: <a href="https://github.com/spring-projects/spring-framework/issues/21758">Suffix support for BigInteger in SPEL [SPR-17225] #21758</a>
 *
 * @author Oleksandr Markushyn
 */
public @interface BigNumberConcern {
}
