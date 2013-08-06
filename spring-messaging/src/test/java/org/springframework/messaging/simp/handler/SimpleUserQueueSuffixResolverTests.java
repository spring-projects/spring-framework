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
 * Test fixture for {@link SimpleUserQueueSuffixResolver}
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleUserQueueSuffixResolverTests {

	private static final String user = "joe";
	private static final List<String> sessionIds = Arrays.asList("sess01", "sess02", "sess03");


	@Test
	public void addOneSessionId() {

		SimpleUserQueueSuffixResolver resolver = new SimpleUserQueueSuffixResolver();
		resolver.addQueueSuffix(user, sessionIds.get(0), sessionIds.get(0));

		assertEquals(Collections.singleton(sessionIds.get(0)), resolver.getUserQueueSuffixes(user));
		assertSame(Collections.emptySet(), resolver.getUserQueueSuffixes("jane"));
	}

	@Test
	public void addMultipleSessionIds() {

		SimpleUserQueueSuffixResolver resolver = new SimpleUserQueueSuffixResolver();
		for (String sessionId : sessionIds) {
			resolver.addQueueSuffix(user, sessionId, sessionId);
		}

		assertEquals(new LinkedHashSet<>(sessionIds), resolver.getUserQueueSuffixes(user));
		assertEquals(Collections.emptySet(), resolver.getUserQueueSuffixes("jane"));
	}


	@Test
	public void removeSessionIds() {

		SimpleUserQueueSuffixResolver resolver = new SimpleUserQueueSuffixResolver();
		for (String sessionId : sessionIds) {
			resolver.addQueueSuffix(user, sessionId, sessionId);
		}

		assertEquals(new LinkedHashSet<>(sessionIds), resolver.getUserQueueSuffixes(user));

		resolver.removeQueueSuffix(user, sessionIds.get(1));
		resolver.removeQueueSuffix(user, sessionIds.get(2));
		assertEquals(Collections.singleton(sessionIds.get(0)), resolver.getUserQueueSuffixes(user));

		resolver.removeQueueSuffix(user, sessionIds.get(0));
		assertSame(Collections.emptySet(), resolver.getUserQueueSuffixes(user));
	}

}
