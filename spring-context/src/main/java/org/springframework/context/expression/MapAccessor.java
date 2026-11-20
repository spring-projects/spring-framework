/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import org.springframework.expression.PropertyAccessor;

/**
 * SpEL {@link PropertyAccessor} that knows how to access the keys of a standard
 * {@link java.util.Map}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 * @deprecated as of Spring Framework 7.0 in favor of {@link org.springframework.expression.spel.support.MapAccessor}.
 */
@Deprecated(since = "7.0", forRemoval = true)
public class MapAccessor extends org.springframework.expression.spel.support.MapAccessor {

	/**
	 * Create a new {@code MapAccessor} for reading as well as writing.
	 * @see #MapAccessor(boolean)
	 */
	public MapAccessor() {
		this(true);
	}

	/**
	 * Create a new {@code MapAccessor} for reading and possibly also writing.
	 * @param allowWrite whether to allow write operations on a target instance
	 * @since 6.2
	 * @see #canWrite
	 */
	public MapAccessor(boolean allowWrite) {
		super(allowWrite);
	}

}
