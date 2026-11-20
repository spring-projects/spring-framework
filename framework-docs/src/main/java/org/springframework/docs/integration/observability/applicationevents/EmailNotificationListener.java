/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.applicationevents;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationListener {

	private final Log logger = LogFactory.getLog(EmailNotificationListener.class);

	@EventListener(EmailReceivedEvent.class)
	@Async("propagatingContextExecutor")
	public void emailReceived(EmailReceivedEvent event) {
		// asynchronously process the received event
		// this logging statement will contain the expected MDC entries from the propagated context
		logger.info("email has been received");
	}

}
