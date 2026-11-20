/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Example bean that has dependencies on parameterized {@link ExampleGenericService}
 * collaborators.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 */
public record ExampleGenericServiceCaller(ExampleGenericService<Integer> integerService,
		ExampleGenericService<String> stringService) {

	public String sayGreeting() {
		return "I say " + this.stringService.greeting() + " " + this.integerService.greeting();
	}

}
