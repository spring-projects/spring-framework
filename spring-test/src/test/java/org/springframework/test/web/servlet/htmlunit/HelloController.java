/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Rob Winch
 * @since 4.2
 */
@RestController
public class HelloController {

	@RequestMapping("/a")
	public String header(HttpServletRequest request) {
		return "hello";
	}

}
