/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.springframework.context.MessageSource;

/**
 * @author Juergen Hoeller
 */
public class AutowiredService {

	private MessageSource messageSource;

	public void setMessageSource(MessageSource messageSource) {
		if (this.messageSource != null) {
			throw new IllegalArgumentException("MessageSource should not be set twice");
		}
		this.messageSource = messageSource;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

}
