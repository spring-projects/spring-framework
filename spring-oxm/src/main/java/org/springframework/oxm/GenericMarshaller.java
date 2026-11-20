/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm;

import java.lang.reflect.Type;

/**
 * Subinterface of {@link Marshaller} that has support for generics.
 *
 * @author Arjen Poutsma
 * @since 3.0.1
 */
public interface GenericMarshaller extends Marshaller {

	/**
	 * Indicates whether this marshaller can marshal instances of the supplied generic type.
	 * @param genericType the type that this marshaller is being asked if it can marshal
	 * @return {@code true} if this marshaller can indeed marshal instances of the supplied type;
	 * {@code false} otherwise
	 */
	boolean supports(Type genericType);

}
