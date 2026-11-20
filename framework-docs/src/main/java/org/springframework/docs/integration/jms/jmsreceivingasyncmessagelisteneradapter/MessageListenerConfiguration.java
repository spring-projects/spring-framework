/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

@Configuration
public class MessageListenerConfiguration {

	// tag::snippet[]
	@Bean
	MessageListenerAdapter messageListener(DefaultTextMessageDelegate messageDelegate) {
		MessageListenerAdapter messageListener = new MessageListenerAdapter(messageDelegate);
		messageListener.setDefaultListenerMethod("receive");
		// We don't want automatic message context extraction
		messageListener.setMessageConverter(null);
		return messageListener;
	}
	// end::snippet[]
}
