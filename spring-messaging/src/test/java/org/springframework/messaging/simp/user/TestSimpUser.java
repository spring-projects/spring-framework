/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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
	public @Nullable Principal getPrincipal() {
		return null;
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
	public boolean equals(@Nullable Object other) {
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
