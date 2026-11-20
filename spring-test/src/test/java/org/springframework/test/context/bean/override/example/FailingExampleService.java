/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

import org.springframework.stereotype.Service;

/**
 * An {@link ExampleService} that always throws an exception.
 *
 * @author Phillip Webb
 */
@Service
public class FailingExampleService implements ExampleService {

	@Override
	public String greeting() {
		throw new IllegalStateException("Failed");
	}

}
