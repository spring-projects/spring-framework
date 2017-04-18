/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * @author Stephane Nicoll
 */
public class MessageListenerTestContainer implements MessageListenerContainer, InitializingBean, DisposableBean {

	private final JmsListenerEndpoint endpoint;

	private boolean autoStartup = true;

	private boolean startInvoked;

	private boolean initializationInvoked;

	private boolean stopInvoked;

	private boolean destroyInvoked;


	MessageListenerTestContainer(JmsListenerEndpoint endpoint) {
		this.endpoint = endpoint;
	}


	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public JmsListenerEndpoint getEndpoint() {
		return endpoint;
	}

	public boolean isStarted() {
		return startInvoked && initializationInvoked;
	}

	public boolean isStopped() {
		return stopInvoked && destroyInvoked;
	}

	@Override
	public void start() throws JmsException {
		if (!initializationInvoked) {
			throw new IllegalStateException("afterPropertiesSet should have been invoked before start on " + this);
		}
		if (startInvoked) {
			throw new IllegalStateException("Start already invoked on " + this);
		}
		startInvoked = true;
	}

	@Override
	public void stop() throws JmsException {
		if (stopInvoked) {
			throw new IllegalStateException("Stop already invoked on " + this);
		}
		stopInvoked = true;
	}

	@Override
	public boolean isRunning() {
		return startInvoked && !stopInvoked;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		stopInvoked = true;
		callback.run();
	}

	@Override
	public void setupMessageListener(Object messageListener) {
	}

	@Override
	public MessageConverter getMessageConverter() {
		return null;
	}

	@Override
	public DestinationResolver getDestinationResolver() {
		return null;
	}

	@Override
	public boolean isPubSubDomain() {
		return true;
	}

	@Override
	public boolean isReplyPubSubDomain() {
		return isPubSubDomain();
	}

	@Override
	public QosSettings getReplyQosSettings() {
		return null;
	}

	@Override
	public void afterPropertiesSet() {
		initializationInvoked = true;
	}

	@Override
	public void destroy() {
		if (!stopInvoked) {
			throw new IllegalStateException("Stop should have been invoked before " + "destroy on " + this);
		}
		destroyInvoked = true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TestContainer{");
		sb.append("endpoint=").append(endpoint);
		sb.append(", startInvoked=").append(startInvoked);
		sb.append(", initializationInvoked=").append(initializationInvoked);
		sb.append(", stopInvoked=").append(stopInvoked);
		sb.append(", destroyInvoked=").append(destroyInvoked);
		sb.append('}');
		return sb.toString();
	}

}
