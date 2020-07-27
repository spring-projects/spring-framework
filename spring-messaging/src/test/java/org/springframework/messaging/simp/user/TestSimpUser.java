/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Rossen Stoyanchev
 */
public class TestSimpUser implements SimpUser {

	private final String name;

	private final Map<String, SimpSession> sessions = new HashMap<>();


	public TestSimpUser(String name) {
		this.name = name;
	}


	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<SimpSession> getSessions() {
		return new HashSet<>(this.sessions.values());
	}

	@Override
	public boolean hasSessions() {
		return !this.sessions.isEmpty();
	}

	@Override
	public SimpSession getSession(String sessionId) {
		return this.sessions.get(sessionId);
	}

	public void addSessions(TestSimpSession... sessions) {
		for (TestSimpSession session : sessions) {
			session.setUser(this);
			this.sessions.put(session.getId(), session);
		}
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof SimpUser && this.name.equals(((SimpUser) other).getName())));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return "name=" + this.name + ", sessions=" + this.sessions;
	}

}
