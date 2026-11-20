/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

/**
 * Used by {@link SpringFactoriesLoaderTests}.
 *
 * @author Andy Wilkinson
 */
class ConstructorArgsDummyFactory implements DummyFactory {

	private final String string;

	public ConstructorArgsDummyFactory(String string) {
		this(string, 0);
	}

	private ConstructorArgsDummyFactory(String string, int reasonCode) {
		this.string = string;
	}

	@Override
	public String getString() {
		return this.string;
	}

}
