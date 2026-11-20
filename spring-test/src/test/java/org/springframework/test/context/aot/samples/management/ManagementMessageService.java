/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.management;

import org.springframework.test.context.aot.samples.common.MessageService;

/**
 * {@link MessageService} implemented in a different package.
 *
 * <p>See <a href="https://github.com/spring-projects/spring-framework/issues/30861">gh-30861</a>.
 *
 * @author Sam Brannen
 * @since 6.0.12
 */
class ManagementMessageService implements MessageService {

	@Override
	public String generateMessage() {
		return "Hello, Management!";
	}

}
