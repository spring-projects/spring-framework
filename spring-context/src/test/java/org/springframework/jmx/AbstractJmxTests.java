/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base JMX test class that pre-loads an ApplicationContext from a user-configurable file. Override the
 * {@link #getApplicationContextPath()} method to control the configuration file location.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractJmxTests extends AbstractMBeanServerTests {

	private ConfigurableApplicationContext ctx;


	@Override
	protected final void onSetUp() {
		ctx = loadContext(getApplicationContextPath());
	}

	@Override
	protected final void onTearDown() {
		if (ctx != null) {
			ctx.close();
		}
	}

	protected String getApplicationContextPath() {
		return "org/springframework/jmx/applicationContext.xml";
	}

	protected ApplicationContext getContext() {
		return this.ctx;
	}
}
