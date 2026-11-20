/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Rob Winch
 * @since 4.2
 */
@Controller
public class ForwardController {

	@RequestMapping("/forward")
	public String forward() {
		return "forward:/a";
	}

	@RequestMapping("/infiniteForward")
	public String infiniteForward() {
		return "forward:/infiniteForward";
	}

}
