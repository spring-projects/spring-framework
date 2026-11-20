/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * {@link ExampleGenericService} implementation for tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 */
public class IntegerExampleGenericService implements ExampleGenericService<Integer> {

	@Override
	public Integer greeting() {
		return 123;
	}

}
