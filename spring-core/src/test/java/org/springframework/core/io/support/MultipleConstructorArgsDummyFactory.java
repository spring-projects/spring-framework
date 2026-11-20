/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

/**
 * Used by {@link SpringFactoriesLoaderTests}.
 *
 * @author Madhura Bhave
 */
class MultipleConstructorArgsDummyFactory implements DummyFactory {

	private final String string;

	private final Integer age;

	MultipleConstructorArgsDummyFactory(String string) {
		this(string, null);
	}

	MultipleConstructorArgsDummyFactory(String string, Integer age) {
		this.string = string;
		this.age = age;
	}


	@Override
	public String getString() {
		return this.string + this.age;
	}

}
