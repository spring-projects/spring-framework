/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stephane Nicoll
 */
public class JmsListenerContainerTestFactory implements JmsListenerContainerFactory<MessageListenerTestContainer> {

	private boolean autoStartup = true;

	private final Map<String, MessageListenerTestContainer> listenerContainers = new LinkedHashMap<>();


	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public List<MessageListenerTestContainer> getListenerContainers() {
		return new ArrayList<>(this.listenerContainers.values());
	}

	public MessageListenerTestContainer getListenerContainer(String id) {
		return this.listenerContainers.get(id);
	}

	@Override
	public MessageListenerTestContainer createListenerContainer(JmsListenerEndpoint endpoint) {
		MessageListenerTestContainer container = new MessageListenerTestContainer(endpoint);
		container.setAutoStartup(this.autoStartup);
		this.listenerContainers.put(endpoint.getId(), container);
		return container;
	}

}
