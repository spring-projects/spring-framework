/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.util.Assert;


/**
 * A helper class for configuring a relay to an external STOMP message broker.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompBrokerRelayRegistration extends AbstractBrokerRegistration {

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String applicationLogin = "guest";

	private String applicationPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private boolean autoStartup = true;


	public StompBrokerRelayRegistration(MessageChannel webSocketReplyChannel, String[] destinationPrefixes) {
		super(webSocketReplyChannel, destinationPrefixes);
	}


	/**
	 * Set the STOMP message broker host.
	 */
	public StompBrokerRelayRegistration setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
		return this;
	}

	/**
	 * Set the STOMP message broker port.
	 */
	public StompBrokerRelayRegistration setRelayPort(int relayPort) {
		this.relayPort = relayPort;
		return this;
	}

	/**
	 * Set the login for the "system" relay session used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 */
	public StompBrokerRelayRegistration setApplicationLogin(String login) {
		Assert.hasText(login, "applicationLogin must not be empty");
		this.applicationLogin = login;
		return this;
	}

	/**
	 * Set the passcode for the "system" relay session used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 */
	public StompBrokerRelayRegistration setApplicationPasscode(String passcode) {
		Assert.hasText(passcode, "applicationPasscode must not be empty");
		this.applicationPasscode = passcode;
		return this;
	}

	/**
	 * Set the interval, in milliseconds, at which the "system" relay session will,
	 * in the absence of any other data being sent, send a heartbeat to the STOMP broker.
	 * A value of zero will prevent heartbeats from being sent to the broker.
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
		return this;
	}

	/**
	 * Set the maximum interval, in milliseconds, at which the "system" relay session
	 * expects, in the absence of any other data, to receive a heartbeat from the STOMP
	 * broker. A value of zero will configure the relay session to expect not to receive
	 * heartbeats from the broker.
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
		return this;
	}

	/**
	 * Configure whether the {@link StompBrokerRelayMessageHandler} should start
	 * automatically when the Spring ApplicationContext is refreshed.
	 * <p>
	 * The default setting is {@code true}.
	 */
	public StompBrokerRelayRegistration setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		return this;
	}


	protected StompBrokerRelayMessageHandler getMessageHandler() {
		StompBrokerRelayMessageHandler handler =
				new StompBrokerRelayMessageHandler(getWebSocketReplyChannel(), getDestinationPrefixes());
		handler.setRelayHost(this.relayHost);
		handler.setRelayPort(this.relayPort);
		handler.setSystemLogin(this.applicationLogin);
		handler.setSystemPasscode(this.applicationPasscode);
		handler.setSystemHeartbeatSendInterval(this.systemHeartbeatSendInterval);
		handler.setSystemHeartbeatReceiveInterval(this.systemHeartbeatReceiveInterval);
		handler.setAutoStartup(this.autoStartup);
		return handler;
	}

}
