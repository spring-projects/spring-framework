/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	private final Map<String, MessageListenerTestContainer> listenerContainers =
			new LinkedHashMap<>();


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
