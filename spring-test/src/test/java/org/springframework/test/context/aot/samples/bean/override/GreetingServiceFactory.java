/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.bean.override;

import org.springframework.test.context.aot.samples.common.GreetingService;

/**
 * @author Sam Brannen
 * @since 6.2
 */
public class GreetingServiceFactory {

	public static GreetingService createEnigmaGreetingService() {
		return () -> "enigma";
	}

}
