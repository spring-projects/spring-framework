/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.server.session;

import org.junit.Test;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.Assert.assertTrue;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class DefaultWebSessionTests {
	private static final Clock CLOCK = Clock.system(ZoneId.of("GMT"));

	private static final IdGenerator idGenerator = new JdkIdGenerator();

	@Test
	public void constructorWhenImplicitStartCopiedThenCopyIsStarted() {
		DefaultWebSession original = createDefaultWebSession();
		original.getAttributes().put("foo", "bar");

		DefaultWebSession copy = new DefaultWebSession(original, CLOCK.instant());

		assertTrue(copy.isStarted());
	}

	@Test
	public void constructorWhenExplicitStartCopiedThenCopyIsStarted() {
		DefaultWebSession original = createDefaultWebSession();
		original.start();

		DefaultWebSession copy = new DefaultWebSession(original, CLOCK.instant());

		assertTrue(copy.isStarted());
	}

	@Test
	public void startsSessionExplicitly() {
		DefaultWebSession session = createDefaultWebSession();
		session.start();
		assertTrue(session.isStarted());
	}

	@Test
	public void startsSessionImplicitly() {
		DefaultWebSession session = createDefaultWebSession();
		session.getAttributes().put("foo", "bar");
		assertTrue(session.isStarted());
	}

	private DefaultWebSession createDefaultWebSession() {
		return new DefaultWebSession(idGenerator, CLOCK, (s, session) -> Mono.empty(), s -> Mono.empty());
	}
}