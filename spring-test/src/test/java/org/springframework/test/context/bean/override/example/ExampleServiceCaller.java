/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Example bean for tests that call the {@link ExampleService}.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public class ExampleServiceCaller {

	private final ExampleService service;

	public ExampleServiceCaller(ExampleService service) {
		this.service = service;
	}

	public ExampleService getService() {
		return this.service;
	}

	public String sayGreeting() {
		return "I say " + this.service.greeting();
	}

}
