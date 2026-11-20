/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

/**
 * @author Juergen Hoeller
 * @since 14.01.2005
 */
class MessageTagOutsideDispatcherServletTests extends MessageTagTests {

	@Override
	protected boolean inDispatcherServlet() {
		return false;
	}

}
