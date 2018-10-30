/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.lang.Nullable;

/**
 * Common configuration object for activating a JMS message endpoint.
 * Gets converted into a provider-specific JCA 1.5 ActivationSpec
 * object for activating the endpoint.
 *
 * <p>Typically used in combination with {@link JmsMessageEndpointManager},
 * but not tied to it.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 * @see JmsActivationSpecFactory
 * @see JmsMessageEndpointManager#setActivationSpecConfig
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 */
public class JmsActivationSpecConfig {

	/** Constants instance for {@code javax.jms.Session}. */
	private static final Constants sessionConstants = new Constants(Session.class);


	@Nullable
	private String destinationName;

	private boolean pubSubDomain = false;

	@Nullable
	private Boolean replyPubSubDomain;

	@Nullable
	private QosSettings replyQosSettings;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	@Nullable
	private String subscriptionName;

	@Nullable
	private String clientId;

	@Nullable
	private String messageSelector;

	private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private int maxConcurrency = -1;

	private int prefetchSize = -1;

	@Nullable
	private MessageConverter messageConverter;


	public void setDestinationName(@Nullable String destinationName) {
		this.destinationName = destinationName;
	}

	@Nullable
	public String getDestinationName() {
		return this.destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return this.replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	public void setReplyQosSettings(@Nullable QosSettings replyQosSettings) {
		this.replyQosSettings = replyQosSettings;
	}

	@Nullable
	public QosSettings getReplyQosSettings() {
		return this.replyQosSettings;
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	public void setSubscriptionName(@Nullable String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	@Nullable
	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setDurableSubscriptionName(@Nullable String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = (durableSubscriptionName != null);
	}

	@Nullable
	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	@Nullable
	public String getClientId() {
		return this.clientId;
	}

	public void setMessageSelector(@Nullable String messageSelector) {
		this.messageSelector = messageSelector;
	}

	@Nullable
	public String getMessageSelector() {
		return this.messageSelector;
	}

	/**
	 * Set the JMS acknowledgement mode by the name of the corresponding constant
	 * in the JMS {@link Session} interface, e.g. "CLIENT_ACKNOWLEDGE".
	 * <p>Note that JCA resource adapters generally only support auto and dups-ok
	 * (see Spring's {@link StandardJmsActivationSpecFactory}). ActiveMQ also
	 * supports "SESSION_TRANSACTED" in the form of RA-managed transactions
	 * (automatically translated by Spring's {@link DefaultJmsActivationSpecFactory}.
	 * @param constantName the name of the {@link Session} acknowledge mode constant
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 * @see StandardJmsActivationSpecFactory
	 * @see DefaultJmsActivationSpecFactory
	 */
	public void setAcknowledgeModeName(String constantName) {
		setAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * Set the JMS acknowledgement mode to use.
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 */
	public void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	/**
	 * Return the JMS acknowledgement mode to use.
	 */
	public int getAcknowledgeMode() {
		return this.acknowledgeMode;
	}

	/**
	 * Specify concurrency limits via a "lower-upper" String, e.g. "5-10", or a simple
	 * upper limit String, e.g. "10".
	 * <p>JCA listener containers will always scale from zero to the given upper limit.
	 * A specified lower limit will effectively be ignored.
	 * <p>This property is primarily supported for configuration compatibility with
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}.
	 * For this activation config, generally use {@link #setMaxConcurrency} instead.
	 */
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setMaxConcurrency(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setMaxConcurrency(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that JmsActivationSpecConfig will effectively ignore the minimum value and " +
					"scale from zero up to the number of consumers according to the maximum value.");
		}
	}

	/**
	 * Specify the maximum number of consumers/sessions to use, effectively
	 * controlling the number of concurrent invocations on the target listener.
	 */
	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	/**
	 * Return the maximum number of consumers/sessions to use.
	 */
	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	/**
	 * Specify the maximum number of messages to load into a session
	 * (a kind of batch size).
	 */
	public void setPrefetchSize(int prefetchSize) {
		this.prefetchSize = prefetchSize;
	}

	/**
	 * Return the maximum number of messages to load into a session.
	 */
	public int getPrefetchSize() {
		return this.prefetchSize;
	}

	/**
	 * Set the {@link MessageConverter} strategy for converting JMS Messages.
	 * @param messageConverter the message converter to use
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the {@link MessageConverter} to use, if any.
	 */
	@Nullable
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

}
