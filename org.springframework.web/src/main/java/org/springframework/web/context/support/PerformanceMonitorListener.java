/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ResponseTimeMonitorImpl;

/**
 * Listener that logs the response times of web requests.
 * To be registered as bean in a WebApplicationContext.
 *
 * <p>Logs performance statistics using Commons Logging at "trace" level.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 21, 2001
 * @see RequestHandledEvent
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0.
 * Use a custom ApplicationListener specific to your needs instead.
 */
public class PerformanceMonitorListener implements ApplicationListener {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final ResponseTimeMonitorImpl responseTimeMonitor = new ResponseTimeMonitorImpl();


	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof RequestHandledEvent) {
			RequestHandledEvent rhe = (RequestHandledEvent) event;
			this.responseTimeMonitor.recordResponseTime(rhe.getProcessingTimeMillis());
			if (logger.isTraceEnabled()) {
				logger.trace("PerformanceMonitorListener: last=[" + rhe.getProcessingTimeMillis() + "ms]; " +
						this.responseTimeMonitor + "; " + rhe.getShortDescription());
			}
		}
	}

}
