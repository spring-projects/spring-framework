/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans;

import java.util.Locale;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.NoSuchMessageException;

public class ACATester implements ApplicationContextAware {

	private ApplicationContext ac;

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws ApplicationContextException {
		// check re-initialization
		if (this.ac != null) {
			throw new IllegalStateException("Already initialized");
		}

		// check message source availability
		if (ctx != null) {
			try {
				ctx.getMessage("code1", null, Locale.getDefault());
			}
			catch (NoSuchMessageException ex) {
				// expected
			}
		}

		this.ac = ctx;
	}

	public ApplicationContext getApplicationContext() {
		return ac;
	}

}
