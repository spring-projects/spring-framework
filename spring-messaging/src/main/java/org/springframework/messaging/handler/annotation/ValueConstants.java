/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation;

/**
 * Common annotation value constants.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ValueConstants {

	/**
	 * Constant defining a value for no default - as a replacement for {@code null} which
	 * we cannot use in annotation attributes.
	 * <p>This is an artificial arrangement of 16 unicode characters, with its sole purpose
	 * being to never match user-declared values.
	 * @see Header#defaultValue()
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
