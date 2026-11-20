/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Example service implementation for tests.
 *
 * @author Phillip Webb
 */
public class RealExampleService implements ExampleService {

	private final String greeting;

	public RealExampleService(String greeting) {
		this.greeting = greeting;
	}

	@Override
	public String greeting() {
		return this.greeting;
	}

}
