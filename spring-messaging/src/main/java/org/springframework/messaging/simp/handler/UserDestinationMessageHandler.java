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

package org.springframework.messaging.simp.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 *
 * Supports destinations prefixed with "/user/{username}" and resolves them into a
 * destination to which the user is currently subscribed by appending the user session id.
 * For example a destination such as "/user/john/queue/trade-confirmation" would resolve
 * to "/trade-confirmation/i9oqdfzo" if "i9oqdfzo" is the user's session id.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UserDestinationMessageHandler implements MessageHandler {

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private String prefix = "/user/";

	private UserSessionResolver userSessionResolver = new InMemoryUserSessionResolver();


	public UserDestinationMessageHandler(MessageSendingOperations<String> messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	/**
	 * <p>The default prefix is "/user".
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String prefix) {
		Assert.hasText(prefix, "prefix is required");
		this.prefix = prefix.endsWith("/") ? prefix : prefix + "/";
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * @param userSessionResolver the userSessionResolver to set
	 */
	public void setUserSessionResolver(UserSessionResolver userSessionResolver) {
		this.userSessionResolver = userSessionResolver;
	}

	/**
	 * @return the userSessionResolver
	 */
	public UserSessionResolver getUserSessionResolver() {
		return this.userSessionResolver;
	}

	/**
	 * @return the messagingTemplate
	 */
	public MessageSendingOperations<String> getMessagingTemplate() {
		return this.messagingTemplate;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		if (!shouldHandle(message)) {
			return;
		}

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		String destination = headers.getDestination();

		if (logger.isTraceEnabled()) {
			logger.trace("Processing message to destination " + destination);
		}

		UserDestinationParser destinationParser = new UserDestinationParser(destination);
		String user = destinationParser.getUser();

		if (user == null) {
			if (logger.isErrorEnabled()) {
				logger.error("Ignoring message, expected destination \"" + this.prefix
						+ "{userId}/**\": " + destination);
			}
			return;
		}

		for (String sessionId : this.userSessionResolver.resolveUserSessionIds(user)) {

			String targetDestination = destinationParser.getTargetDestination(sessionId);
			headers.setDestination(targetDestination);
			message = MessageBuilder.fromMessage(message).copyHeaders(headers.toMap()).build();

			if (logger.isTraceEnabled()) {
				logger.trace("Sending message to resolved target destination " + targetDestination);
			}
			this.messagingTemplate.send(targetDestination, message);
		}
	}

	protected boolean shouldHandle(Message<?> message) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();
		String destination = headers.getDestination();

		if (!SimpMessageType.MESSAGE.equals(messageType)) {
			return false;
		}

		if (!StringUtils.hasText(destination)) {
			if (logger.isErrorEnabled()) {
				logger.error("Ignoring message, no destination: " + headers);
			}
			return false;
		}
		else if (!destination.startsWith(this.prefix)) {
			return false;
		}

		return true;
	}


	private class UserDestinationParser {

		private final String user;

		private final String targetDestination;


		public UserDestinationParser(String destination) {

			int userStartIndex = prefix.length();
			int userEndIndex = destination.indexOf('/', userStartIndex);

			if (userEndIndex > 0) {
				this.user = destination.substring(userStartIndex, userEndIndex);
				this.targetDestination = destination.substring(userEndIndex);
			}
			else {
				this.user = null;
				this.targetDestination = null;
			}
		}

		public String getUser() {
			return this.user;
		}

		public String getTargetDestination(String sessionId) {
			return (this.targetDestination != null) ? this.targetDestination + "/" + sessionId : null;
		}
	}

}
