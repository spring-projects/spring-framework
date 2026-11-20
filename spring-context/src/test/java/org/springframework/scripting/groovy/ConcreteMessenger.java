/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import org.springframework.scripting.ConfigurableMessenger;

/**
 * @author Dave Syer
 *
 */
public class ConcreteMessenger implements ConfigurableMessenger {

	private String message;

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void setMessage(String message) {
		this.message = message;
	}

}
