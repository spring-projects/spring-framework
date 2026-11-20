/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.naming;

/**
 * @author Rob Harrop
 */
class KeyNamingStrategyTests extends AbstractNamingStrategyTests {

	private static final String OBJECT_NAME = "spring:name=test";


	@Override
	protected ObjectNamingStrategy getStrategy() {
		return new KeyNamingStrategy();
	}

	@Override
	protected Object getManagedResource() {
		return new Object();
	}

	@Override
	protected String getKey() {
		return OBJECT_NAME;
	}

	@Override
	protected String getCorrectObjectName() {
		return OBJECT_NAME;
	}

}
