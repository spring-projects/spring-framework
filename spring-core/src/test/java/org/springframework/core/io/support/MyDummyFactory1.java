/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import org.springframework.core.annotation.Order;

/**
 * Used by {@link SpringFactoriesLoaderTests}
 *
 * @author Arjen Poutsma
 */
@Order(1)
public class MyDummyFactory1 implements DummyFactory {

	@Override
	public String getString() {
		return "Foo";
	}
}
