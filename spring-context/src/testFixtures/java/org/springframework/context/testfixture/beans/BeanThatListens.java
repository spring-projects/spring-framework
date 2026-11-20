/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans;

import java.util.Map;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * A stub {@link ApplicationListener}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class BeanThatListens implements ApplicationListener<ApplicationEvent> {

	private BeanThatBroadcasts beanThatBroadcasts;

	private int eventCount;


	public BeanThatListens() {
	}

	public BeanThatListens(BeanThatBroadcasts beanThatBroadcasts) {
		this.beanThatBroadcasts = beanThatBroadcasts;
		Map<?, BeanThatListens> beans = beanThatBroadcasts.applicationContext.getBeansOfType(BeanThatListens.class);
		if (!beans.isEmpty()) {
			throw new IllegalStateException("Shouldn't have found any BeanThatListens instances");
		}
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		eventCount++;
		if (beanThatBroadcasts != null) {
			beanThatBroadcasts.receivedCount++;
		}
	}

	public int getEventCount() {
		return eventCount;
	}

	public void zero() {
		eventCount = 0;
	}

}
