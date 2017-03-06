/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
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

	private static final Log logger = LogFactory.getLog(DefaultUserDestinationResolver.class);


	private final SimpUserRegistry userRegistry;

	private String prefix = "/user/";

	private boolean keepLeadingSlash = true;


	/**
	 * Create an instance that will access user session id information through
	 * the provided registry.
	 * @param userRegistry the registry, never {@code null}
	 */
	public DefaultUserDestinationResolver(SimpUserRegistry userRegistry) {
		Assert.notNull(userRegistry, "'userRegistry' must not be null");
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
		Assert.hasText(prefix, "prefix must not be empty");
		this.prefix = prefix.endsWith("/") ? prefix : prefix + "/";
	}

	/**
	 * Return the configured prefix for user destinations.
	 */
	public String getDestinationPrefix() {
		return this.prefix;
	}

	/**
	 * Provide the {@code PathMatcher} in use for working with destinations
	 * which in turn helps to determine whether the leading slash should be
	 * kept in actual destinations after removing the
	 * {@link #setUserDestinationPrefix userDestinationPrefix}.
	 * <p>By default actual destinations have a leading slash, e.g.
	 * {@code /queue/position-updates} which makes sense with brokers that
	 * support destinations with slash as separator. When a {@code PathMatcher}
	 * is provided that supports an alternative separator, then resulting
	 * destinations won't have a leading slash, e.g. {@code
	 * jms.queue.position-updates}.
	 * @param pathMatcher the PathMatcher used to work with destinations
	 * @since 4.3
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		if (pathMatcher != null) {
			this.keepLeadingSlash = pathMatcher.combine("1", "2").equals("1/2");
		}
	}


	@Override
	public UserDestinationResult resolveDestination(Message<?> message) {
		String sourceDestination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		ParseResult parseResult = parse(message);
		if (parseResult == null) {
			return null;
		}
		String user = parseResult.getUser();
		Set<String> targetSet = new HashSet<>();
		for (String sessionId : parseResult.getSessionIds()) {
			String actualDestination = parseResult.getActualDestination();
			String targetDestination = getTargetDestination(sourceDestination, actualDestination, sessionId, user);
			if (targetDestination != null) {
				targetSet.add(targetDestination);
			}
		}
		String subscribeDestination = parseResult.getSubscribeDestination();
		return new UserDestinationResult(sourceDestination, targetSet, subscribeDestination, user);
	}

	private ParseResult parse(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null || !checkDestination(destination, this.prefix)) {
			return null;
		}
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (SimpMessageType.SUBSCRIBE.equals(messageType) || SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			if (sessionId == null) {
				logger.error("No session id. Ignoring " + message);
				return null;
			}
			int prefixEnd = this.prefix.length() - 1;
			String actualDestination = destination.substring(prefixEnd);
			if (!this.keepLeadingSlash) {
				actualDestination = actualDestination.substring(1);
			}
			String user = (principal != null ? principal.getName() : null);
			return new ParseResult(actualDestination, destination, Collections.singleton(sessionId), user);
		}
		else if (SimpMessageType.MESSAGE.equals(messageType)) {
			int prefixEnd = this.prefix.length();
			int userEnd = destination.indexOf('/', prefixEnd);
			Assert.isTrue(userEnd > 0, "Expected destination pattern \"/user/{userId}/**\"");
			String actualDestination = destination.substring(userEnd);
			String subscribeDestination = this.prefix.substring(0, prefixEnd - 1) + actualDestination;
			String userName = destination.substring(prefixEnd, userEnd);
			userName = StringUtils.replace(userName, "%2F", "/");
			Set<String> sessionIds;
			if (userName.equals(sessionId)) {
				userName = null;
				sessionIds = Collections.singleton(sessionId);
			}
			else {
				SimpUser user = this.userRegistry.getUser(userName);
				if (user != null) {
					if (user.getSession(sessionId) != null) {
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
			}
			if (!this.keepLeadingSlash) {
				actualDestination = actualDestination.substring(1);
			}
			return new ParseResult(actualDestination, subscribeDestination, sessionIds, userName);
		}
		else {
			return null;
		}
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
	protected String getTargetDestination(String sourceDestination, String actualDestination,
			String sessionId, String user) {

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

		private final String actualDestination;

		private final String subscribeDestination;

		private final Set<String> sessionIds;

		private final String user;


		public ParseResult(String actualDest, String subscribeDest, Set<String> sessionIds, String user) {
			this.actualDestination = actualDest;
			this.subscribeDestination = subscribeDest;
			this.sessionIds = sessionIds;
			this.user = user;
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

		public String getUser() {
			return this.user;
		}
	}

}
