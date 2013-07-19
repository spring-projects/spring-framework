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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link SimpleUserSessionResolver}
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleUserSessionResolverTests {

	private static final String user = "joe";
	private static final List<String> sessionIds = Arrays.asList("sess01", "sess02", "sess03");


	@Test
	public void addOneSessionId() {

		SimpleUserSessionResolver resolver = new SimpleUserSessionResolver();
		resolver.addUserSessionId(user, sessionIds.get(0));

		assertEquals(Collections.singleton(sessionIds.get(0)), resolver.resolveUserSessionIds(user));
		assertSame(Collections.emptySet(), resolver.resolveUserSessionIds("jane"));
	}

	@Test
	public void addMultipleSessionIds() {

		SimpleUserSessionResolver resolver = new SimpleUserSessionResolver();
		for (String sessionId : sessionIds) {
			resolver.addUserSessionId(user, sessionId);
		}

		assertEquals(new LinkedHashSet<>(sessionIds), resolver.resolveUserSessionIds(user));
		assertEquals(Collections.emptySet(), resolver.resolveUserSessionIds("jane"));
	}


	@Test
	public void removeSessionIds() {

		SimpleUserSessionResolver resolver = new SimpleUserSessionResolver();
		for (String sessionId : sessionIds) {
			resolver.addUserSessionId(user, sessionId);
		}

		assertEquals(new LinkedHashSet<>(sessionIds), resolver.resolveUserSessionIds(user));

		resolver.removeUserSessionId(user, sessionIds.get(1));
		resolver.removeUserSessionId(user, sessionIds.get(2));
		assertEquals(Collections.singleton(sessionIds.get(0)), resolver.resolveUserSessionIds(user));

		resolver.removeUserSessionId(user, sessionIds.get(0));
		assertSame(Collections.emptySet(), resolver.resolveUserSessionIds(user));
	}

}
