/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A default implementation of {@code UserDestinationResolver} that relies
 * on a {@link SimpUserRegistry} to find active sessions for a user.
 *
 * <p>When a user attempts to subscribe, e.g. to "/user/queue/position-updates",
 * the "/user" prefix is removed and a unique suffix added based on the session
 * id, e.g. "/queue/position-updates-useri9oqdfzo" to ensure different users can
 * subscribe to the same logical destination without colliding.
 *
 * <p>When sending to a user, e.g. "/user/{username}/queue/position-updates", the
 * "/user/{username}" prefix is removed and a suffix based on active session id's
 * is added, e.g. "/queue/position-updates-useri9oqdfzo".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class DefaultUserDestinationResolver implements UserDestinationResolver {

	private static final Log logger = SimpLogging.forLogName(DefaultUserDestinationResolver.class);


	private final SimpUserRegistry userRegistry;

	private String prefix = "/user/";

	private boolean removeLeadingSlash = false;


	/**
	 * Create an instance that will access user session id information through
	 * the provided registry.
	 * @param userRegistry the registry, never {@code null}
	 */
	public DefaultUserDestinationResolver(SimpUserRegistry userRegistry) {
		Assert.notNull(userRegistry, "SimpUserRegistry must not be null");
		this.userRegistry = userRegistry;
	}


	/**
	 * Return the configured {@link SimpUserRegistry}.
	 */
	public SimpUserRegistry getSimpUserRegistry() {
		return this.userRegistry;
	}

	/**
	 * The prefix used to identify user destinations. Any destinations that do not
	 * start with the given prefix are not be resolved.
	 * <p>The default prefix is "/user/".
	 * @param prefix the prefix to use
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "Prefix must not be empty");
		this.prefix = (prefix.endsWith("/") ? prefix : prefix + "/");
	}

	/**
	 * Return the configured prefix for user destinations.
	 */
	public String getDestinationPrefix() {
		return this.prefix;
	}

	/**
	 * Use this property to indicate whether the leading slash from translated
	 * user destinations should be removed or not. This depends on the
	 * destination prefixes the message broker is configured with.
	 * <p>By default this is set to {@code false}, i.e.
	 * "do not change the target destination", although
	 * {@link org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration
	 * AbstractMessageBrokerConfiguration} may change that to {@code true}
	 * if the configured destinations do not have a leading slash.
	 * @param remove whether to remove the leading slash
	 * @since 4.3.14
	 */
	public void setRemoveLeadingSlash(boolean remove) {
		this.removeLeadingSlash = remove;
	}

	/**
	 * Whether to remove the leading slash from target destinations.
	 * @since 4.3.14
	 */
	public boolean isRemoveLeadingSlash() {
		return this.removeLeadingSlash;
	}


	@Override
	@Nullable
	public UserDestinationResult resolveDestination(Message<?> message) {
		ParseResult parseResult = parse(message);
		if (parseResult == null) {
			return null;
		}
		String user = parseResult.getUser();
		String sourceDestination = parseResult.getSourceDestination();
		Set<String> targetSet = new HashSet<>();
		for (String sessionId : parseResult.getSessionIds()) {
			String actualDestination = parseResult.getActualDestination();
			String targetDestination = getTargetDestination(
					sourceDestination, actualDestination, sessionId, user);
			if (targetDestination != null) {
				targetSet.add(targetDestination);
			}
		}
		String subscribeDestination = parseResult.getSubscribeDestination();
		return new UserDestinationResult(sourceDestination, targetSet, subscribeDestination, user);
	}

	@Nullable
	private ParseResult parse(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String sourceDestination = SimpMessageHeaderAccessor.getDestination(headers);
		if (sourceDestination == null || !checkDestination(sourceDestination, this.prefix)) {
			return null;
		}
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (messageType != null) {
			switch (messageType) {
				case SUBSCRIBE:
				case UNSUBSCRIBE:
					return parseSubscriptionMessage(message, sourceDestination);
				case MESSAGE:
					return parseMessage(headers, sourceDestination);
			}
		}
		return null;
	}

	@Nullable
	private ParseResult parseSubscriptionMessage(Message<?> message, String sourceDestination) {
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			logger.error("No session id. Ignoring " + message);
			return null;
		}
		int prefixEnd = this.prefix.length() - 1;
		String actualDestination = sourceDestination.substring(prefixEnd);
		if (isRemoveLeadingSlash()) {
			actualDestination = actualDestination.substring(1);
		}
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		String user = (principal != null ? principal.getName() : null);
		Assert.isTrue(user == null || !user.contains("%2F"), "Invalid sequence \"%2F\" in user name: " + user);
		Set<String> sessionIds = Collections.singleton(sessionId);
		return new ParseResult(sourceDestination, actualDestination, sourceDestination, sessionIds, user);
	}

	private ParseResult parseMessage(MessageHeaders headers, String sourceDest) {
		int prefixEnd = this.prefix.length();
		int userEnd = sourceDest.indexOf('/', prefixEnd);
		Assert.isTrue(userEnd > 0, "Expected destination pattern \"/user/{userId}/**\"");
		String actualDest = sourceDest.substring(userEnd);
		String subscribeDest = this.prefix.substring(0, prefixEnd - 1) + actualDest;
		String userName = sourceDest.substring(prefixEnd, userEnd);
		userName = StringUtils.replace(userName, "%2F", "/");

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Set<String> sessionIds;
		if (userName.equals(sessionId)) {
			userName = null;
			sessionIds = Collections.singleton(sessionId);
		}
		else {
			sessionIds = getSessionIdsByUser(userName, sessionId);
		}

		if (isRemoveLeadingSlash()) {
			actualDest = actualDest.substring(1);
		}
		return new ParseResult(sourceDest, actualDest, subscribeDest, sessionIds, userName);
	}

	private Set<String> getSessionIdsByUser(String userName, @Nullable String sessionId) {
		Set<String> sessionIds;
		SimpUser user = this.userRegistry.getUser(userName);
		if (user != null) {
			if (sessionId != null && user.getSession(sessionId) != null) {
				sessionIds = Collections.singleton(sessionId);
			}
			else {
				Set<SimpSession> sessions = user.getSessions();
				sessionIds = new HashSet<>(sessions.size());
				for (SimpSession session : sessions) {
					sessionIds.add(session.getId());
				}
			}
		}
		else {
			sessionIds = Collections.emptySet();
		}
		return sessionIds;
	}

	protected boolean checkDestination(String destination, String requiredPrefix) {
		return destination.startsWith(requiredPrefix);
	}

	/**
	 * This method determines how to translate the source "user" destination to an
	 * actual target destination for the given active user session.
	 * @param sourceDestination the source destination from the input message.
	 * @param actualDestination a subset of the destination without any user prefix.
	 * @param sessionId the id of an active user session, never {@code null}.
	 * @param user the target user, possibly {@code null}, e.g if not authenticated.
	 * @return a target destination, or {@code null} if none
	 */
	@SuppressWarnings("unused")
	@Nullable
	protected String getTargetDestination(String sourceDestination, String actualDestination,
			String sessionId, @Nullable String user) {

		return actualDestination + "-user" + sessionId;
	}

	@Override
	public String toString() {
		return "DefaultUserDestinationResolver[prefix=" + this.prefix + "]";
	}


	/**
	 * A temporary placeholder for a parsed source "user" destination.
	 */
	private static class ParseResult {

		private final String sourceDestination;

		private final String actualDestination;

		private final String subscribeDestination;

		private final Set<String> sessionIds;

		@Nullable
		private final String user;

		public ParseResult(String sourceDest, String actualDest, String subscribeDest,
				Set<String> sessionIds, @Nullable String user) {

			this.sourceDestination = sourceDest;
			this.actualDestination = actualDest;
			this.subscribeDestination = subscribeDest;
			this.sessionIds = sessionIds;
			this.user = user;
		}

		public String getSourceDestination() {
			return this.sourceDestination;
		}

		public String getActualDestination() {
			return this.actualDestination;
		}

		public String getSubscribeDestination() {
			return this.subscribeDestination;
		}

		public Set<String> getSessionIds() {
			return this.sessionIds;
		}

		@Nullable
		public String getUser() {
			return this.user;
		}
	}

}
