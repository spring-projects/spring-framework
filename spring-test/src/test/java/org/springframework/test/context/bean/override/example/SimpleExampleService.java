/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Example service implementation for tests.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public class SimpleExampleService extends RealExampleService {

	public SimpleExampleService() {
		super("simple");
	}

}
