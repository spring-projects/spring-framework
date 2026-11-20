/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

/**
 * Implementors of this interface are expected to be able to locate types.
 *
 * <p>They may use a custom {@link ClassLoader} and/or deal with common package
 * prefixes (for example, {@code java.lang}) however they wish.
 *
 * <p>See {@link org.springframework.expression.spel.support.StandardTypeLocator}
 * for an example implementation.
 *
 * @author Andy Clement
 * @since 3.0
 */
@FunctionalInterface
public interface TypeLocator {

	/**
	 * Find a type by name.
	 * <p>The name may or may not be fully qualified &mdash; for example,
	 * {@code String} or {@code java.lang.String}.
	 * @param typeName the type to be located
	 * @return the {@code Class} object representing that type
	 * @throws EvaluationException if there is a problem finding the type
	 */
	Class<?> findType(String typeName) throws EvaluationException;

}
