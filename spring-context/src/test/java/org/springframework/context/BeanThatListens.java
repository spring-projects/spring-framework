/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context;

import java.util.Map;

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
