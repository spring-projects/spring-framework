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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleUserSessionResolver implements MutableUserSessionResolver {

	// userId -> sessionId's
	private final ConcurrentMap<String, Set<String>> userSessionIds = new ConcurrentHashMap<String, Set<String>>();


	@Override
	public void addUserSessionId(String user, String sessionId) {
		Set<String> sessionIds = this.userSessionIds.get(user);
		if (sessionIds == null) {
			sessionIds = new CopyOnWriteArraySet<String>();
			Set<String> value = this.userSessionIds.putIfAbsent(user, sessionIds);
			if (value != null) {
				sessionIds = value;
			}
		}
		sessionIds.add(sessionId);
	}

	@Override
	public void removeUserSessionId(String user, String sessionId) {
		Set<String> sessionIds = this.userSessionIds.get(user);
		if (sessionIds != null) {
			if (sessionIds.remove(sessionId)) {
				this.userSessionIds.remove(user, Collections.<String>emptySet());
			}
		}
	}

	@Override
	public Set<String> resolveUserSessionIds(String user) {
		Set<String> sessionIds = this.userSessionIds.get(user);
		return (sessionIds != null) ? sessionIds : Collections.<String>emptySet();
	}

}
