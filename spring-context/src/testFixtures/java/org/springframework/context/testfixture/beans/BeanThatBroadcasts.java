/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Juergen Hoeller
 */
public class BeanThatBroadcasts implements ApplicationContextAware {

	public ApplicationContext applicationContext;

	public int receivedCount;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext.getDisplayName().contains("listener")) {
			applicationContext.getBean("listener");
		}
	}

}
