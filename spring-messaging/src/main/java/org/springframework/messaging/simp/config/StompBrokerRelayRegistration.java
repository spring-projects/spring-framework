/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.config;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Registration class for configuring a {@link StompBrokerRelayMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompBrokerRelayRegistration extends AbstractBrokerRegistration {

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	@Nullable
	private Long systemHeartbeatSendInterval;

	@Nullable
	private Long systemHeartbeatReceiveInterval;

	@Nullable
	private String virtualHost;

	@Nullable
	private TcpOperations<byte[]> tcpClient;

	@Nullable
	private TaskScheduler taskScheduler;

	private boolean autoStartup = true;

	@Nullable
	private String userDestinationBroadcast;

	@Nullable
	private String userRegistryBroadcast;


	public StompBrokerRelayRegistration(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, String[] destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, destinationPrefixes);
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
	 * Set the login to use when creating connections to the STOMP broker on
	 * behalf of connected clients.
	 * <p>By default this is set to "guest".
	 */
	public StompBrokerRelayRegistration setClientLogin(String login) {
		Assert.hasText(login, "clientLogin must not be empty");
		this.clientLogin = login;
		return this;
	}

	/**
	 * Set the passcode to use when creating connections to the STOMP broker on
	 * behalf of connected clients.
	 * <p>By default this is set to "guest".
	 */
	public StompBrokerRelayRegistration setClientPasscode(String passcode) {
		Assert.hasText(passcode, "clientPasscode must not be empty");
		this.clientPasscode = passcode;
		return this;
	}

	/**
	 * Set the login for the shared "system" connection used to send messages to
	 * the STOMP broker from within the application, i.e. messages not associated
	 * with a specific client session (e.g. REST/HTTP request handling method).
	 * <p>By default this is set to "guest".
	 */
	public StompBrokerRelayRegistration setSystemLogin(String login) {
		Assert.hasText(login, "systemLogin must not be empty");
		this.systemLogin = login;
		return this;
	}

	/**
	 * Set the passcode for the shared "system" connection used to send messages to
	 * the STOMP broker from within the application, i.e. messages not associated
	 * with a specific client session (e.g. REST/HTTP request handling method).
	 * <p>By default this is set to "guest".
	 */
	public StompBrokerRelayRegistration setSystemPasscode(String passcode) {
		Assert.hasText(passcode, "systemPasscode must not be empty");
		this.systemPasscode = passcode;
		return this;
	}

	/**
	 * Set the interval, in milliseconds, at which the "system" relay session will,
	 * in the absence of any other data being sent, send a heartbeat to the STOMP broker.
	 * A value of zero will prevent heartbeats from being sent to the broker.
	 * <p>The default value is 10000.
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
	 * <p>The default value is 10000.
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
		return this;
	}

	/**
	 * Set the value of the "host" header to use in STOMP CONNECT frames. When this
	 * property is configured, a "host" header will be added to every STOMP frame sent to
	 * the STOMP broker. This may be useful for example in a cloud environment where the
	 * actual host to which the TCP connection is established is different from the host
	 * providing the cloud-based STOMP service.
	 * <p>By default this property is not set.
	 */
	public StompBrokerRelayRegistration setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
		return this;
	}

	/**
	 * Configure a TCP client for managing TCP connections to the STOMP broker.
	 * <p>By default {@code ReactorNettyTcpClient} is used.
	 * <p><strong>Note:</strong> when this property is used, any
	 * {@link #setRelayHost(String) host} or {@link #setRelayPort(int) port}
	 * specified are effectively ignored.
	 * @since 4.3.15
	 */
	public StompBrokerRelayRegistration setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
		return this;
	}

	/**
	 * Some STOMP clients (e.g. stomp-js) always send heartbeats at a fixed rate
	 * but others (Spring STOMP client) do so only when no other messages are
	 * sent. However messages with a non-broker {@link #getDestinationPrefixes()
	 * destination prefix} aren't forwarded and as a result the broker may deem
	 * the connection inactive.
	 * <p>When this {@link TaskScheduler} is set, it is used to reset a count of
	 * the number of messages sent from client to broker since the beginning of
	 * the current heartbeat period. This is then used to decide whether to send
	 * a heartbeat to the broker when ignoring a message with a non-broker
	 * destination prefix.
	 * @since 5.3
	 */
	public StompBrokerRelayRegistration setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	/**
	 * Configure whether the {@link StompBrokerRelayMessageHandler} should start
	 * automatically when the Spring ApplicationContext is refreshed.
	 * <p>The default setting is {@code true}.
	 */
	public StompBrokerRelayRegistration setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		return this;
	}

	/**
	 * Set a destination to broadcast messages to user destinations that remain
	 * unresolved because the user appears not to be connected. In a
	 * multi-application server scenario this gives other application servers
	 * a chance to try.
	 * <p>By default this is not set.
	 * @param destination the destination to broadcast unresolved messages to,
	 * e.g. "/topic/unresolved-user-destination"
	 */
	public StompBrokerRelayRegistration setUserDestinationBroadcast(String destination) {
		this.userDestinationBroadcast = destination;
		return this;
	}

	@Nullable
	protected String getUserDestinationBroadcast() {
		return this.userDestinationBroadcast;
	}

	/**
	 * Set a destination to broadcast the content of the local user registry to
	 * and to listen for such broadcasts from other servers. In a multi-application
	 * server scenarios this allows each server's user registry to be aware of
	 * users connected to other servers.
	 * <p>By default this is not set.
	 * @param destination the destination for broadcasting user registry details,
	 * e.g. "/topic/simp-user-registry".
	 */
	public StompBrokerRelayRegistration setUserRegistryBroadcast(String destination) {
		this.userRegistryBroadcast = destination;
		return this;
	}

	@Nullable
	protected String getUserRegistryBroadcast() {
		return this.userRegistryBroadcast;
	}


	@Override
	protected StompBrokerRelayMessageHandler getMessageHandler(SubscribableChannel brokerChannel) {
		StompBrokerRelayMessageHandler handler = new StompBrokerRelayMessageHandler(
				getClientInboundChannel(), getClientOutboundChannel(),
				brokerChannel, getDestinationPrefixes());

		handler.setRelayHost(this.relayHost);
		handler.setRelayPort(this.relayPort);

		handler.setClientLogin(this.clientLogin);
		handler.setClientPasscode(this.clientPasscode);

		handler.setSystemLogin(this.systemLogin);
		handler.setSystemPasscode(this.systemPasscode);

		if (this.systemHeartbeatSendInterval != null) {
			handler.setSystemHeartbeatSendInterval(this.systemHeartbeatSendInterval);
		}
		if (this.systemHeartbeatReceiveInterval != null) {
			handler.setSystemHeartbeatReceiveInterval(this.systemHeartbeatReceiveInterval);
		}
		if (this.virtualHost != null) {
			handler.setVirtualHost(this.virtualHost);
		}
		if (this.tcpClient != null) {
			handler.setTcpClient(this.tcpClient);
		}
		if (this.taskScheduler != null) {
			handler.setTaskScheduler(this.taskScheduler);
		}

		handler.setAutoStartup(this.autoStartup);

		return handler;
	}

}
