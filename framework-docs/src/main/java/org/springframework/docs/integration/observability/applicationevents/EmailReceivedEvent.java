/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.applicationevents;

import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class EmailReceivedEvent extends ApplicationEvent {

	public EmailReceivedEvent(Object source) {
		super(source);
	}

}
