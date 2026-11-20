/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.factory;

import java.io.Serializable;

/**
 * A sample object with a generic type.
 *
 * @param <T> the number type
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public class NumberHolder<T extends Number> implements Serializable {

	@SuppressWarnings("unused")
	private final T number;

	public NumberHolder(T number) {
		this.number = number;
	}

}
