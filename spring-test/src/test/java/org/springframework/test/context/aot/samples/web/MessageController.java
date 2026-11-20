/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@RestController
class MessageController {

	@GetMapping("/hello")
	String hello() {
		return "Hello, AOT!";
	}

}
