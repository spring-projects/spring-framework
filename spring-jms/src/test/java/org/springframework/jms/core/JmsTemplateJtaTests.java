/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.core;

/**
 * @author Juergen Hoeller
 * @since 06.01.2005
 */
class JmsTemplateJtaTests extends JmsTemplateTests {

	@Override
	protected boolean useTransactedSession() {
		return true;
	}

	@Override
	protected boolean useTransactedTemplate() {
		return false;
	}

}
