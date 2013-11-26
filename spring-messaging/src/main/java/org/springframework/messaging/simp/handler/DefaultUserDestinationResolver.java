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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A default implementation of {@link UserDestinationResolver}.
 *
 * <p>Resolves messages sent to destination patterns "/user/{user-name}/**" as well as
 * subscriptions to destinations "/user/queue/**" where the "/user/" prefix used to
 * recognize such destinations is customizable via
 * {@link #setUserDestinationPrefix(String)}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultUserDestinationResolver implements UserDestinationResolver {

	private static final Log logger = LogFactory.getLog(DefaultUserDestinationResolver.class);


	private final UserSessionRegistry userSessionRegistry;

	private String destinationPrefix = "/user/";

	private String subscriptionDestinationPrefix = "/user/queue/";


	/**
	 * Create an instance that will access user session id information through
	 * the provided registry.
	 * @param userSessionRegistry the registry, never {@code null}
	 */
	public DefaultUserDestinationResolver(UserSessionRegistry userSessionRegistry) {
		Assert.notNull(userSessionRegistry, "'userSessionRegistry' must not be null");
		this.userSessionRegistry = userSessionRegistry;
	}

	/**
	 * The prefix used to identify user destinations. Any destinations that do not
	 * start with the given prefix are not be resolved.
	 * <p>The default value is "/user/".
	 * @param prefix the prefix to use
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "prefix must not be empty");
		this.destinationPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		this.subscriptionDestinationPrefix = this.destinationPrefix + "queue/";
	}

	/**
	 * Return the prefix used to identify user destinations. Any destinations that do not
	 * start with the given prefix are not be resolved.
	 * <p>By default "/user/queue/".
	 */
	public String getDestinationPrefix() {
		return this.destinationPrefix;
	}

	/**
	 * Return the prefix used to identify user destinations for (un)subscribe messages.
	 * <p>By default "/user/queue/".
	 */
	public String getSubscriptionDestinationPrefix() {
		return this.subscriptionDestinationPrefix;
	}

	@Override
	public Set<String> resolveDestination(Message<?> message) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		UserDestinationInfo info = getUserDestinationInfo(headers);
		if (info == null) {
			return Collections.emptySet();
		}

		Set<String> set = new HashSet<String>();
		for (String sessionId : this.userSessionRegistry.getSessionIds(info.getUser())) {
			set.add(getTargetDestination(headers.getDestination(), info.getDestination(), sessionId, info.getUser()));
		}
		return set;
	}

	protected String getTargetDestination(String originalDestination, String targetDestination,
			String sessionId, String user) {

		return targetDestination + "-user" + sessionId;
	}

	private UserDestinationInfo getUserDestinationInfo(SimpMessageHeaderAccessor headers) {

		String destination = headers.getDestination();

		String targetUser;
		String targetDestination;

		Principal user = headers.getUser();
		SimpMessageType messageType = headers.getMessageType();

		if (SimpMessageType.SUBSCRIBE.equals(messageType) || SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			if (!checkDestination(destination, this.subscriptionDestinationPrefix)) {
				return null;
			}
			if (user == null) {
				logger.warn("Ignoring message, no user information");
				return null;
			}
			targetUser = user.getName();
			targetDestination = destination.substring(this.destinationPrefix.length()-1);
		}
		else if (SimpMessageType.MESSAGE.equals(messageType)) {
			if (!checkDestination(destination, this.destinationPrefix)) {
				return null;
			}
			int startIndex = this.destinationPrefix.length();
			int endIndex = destination.indexOf('/', startIndex);
			Assert.isTrue(endIndex > 0, "Expected destination pattern \"/user/{userId}/**\"");
			targetUser = destination.substring(startIndex, endIndex);
			targetDestination = destination.substring(endIndex);

		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring " + messageType + " message");
			}
			return null;
		}

		return new UserDestinationInfo(targetUser, targetDestination);
	}

	protected boolean checkDestination(String destination, String requiredPrefix) {
		if (destination == null) {
			logger.trace("Ignoring message, no destination");
			return false;
		}
		if (!destination.startsWith(requiredPrefix)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring message to " + destination + ", not a \"user\" destination");
			}
			return false;
		}
		return true;
	}


	private static class UserDestinationInfo {

		private final String user;

		private final String destination;

		private UserDestinationInfo(String user, String destination) {
			this.user = user;
			this.destination = destination;
		}

		private String getUser() {
			return this.user;
		}

		private String getDestination() {
			return this.destination;
		}
	}

}
