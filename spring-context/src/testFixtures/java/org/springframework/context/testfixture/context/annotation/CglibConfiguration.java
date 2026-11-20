/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CglibConfiguration {

	private static final AtomicInteger counter = new AtomicInteger();

	@Bean
	public String prefix() {
		return getPrefix() + counter.getAndIncrement();
	}

	@Bean
	public String text() {
		return prefix() + " World";
	}

	protected String getPrefix() {
		return "Hello";
	}

}
