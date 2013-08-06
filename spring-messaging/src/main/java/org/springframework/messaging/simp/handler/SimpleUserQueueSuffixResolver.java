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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleUserQueueSuffixResolver implements MutableUserQueueSuffixResolver {

	// userId -> [sessionId -> queueSuffix]
	private final ConcurrentMap<String, Map<String, String>> cache = new ConcurrentHashMap<String, Map<String, String>>();


	@Override
	public void addQueueSuffix(String user, String sessionId, String suffix) {
		Map<String, String> suffixes = this.cache.get(user);
		if (suffixes == null) {
			suffixes = new ConcurrentHashMap<String, String>();
			Map<String, String> prevSuffixes = this.cache.putIfAbsent(user, suffixes);
			if (prevSuffixes != null) {
				suffixes = prevSuffixes;
			}
		}
		suffixes.put(sessionId, suffix);
	}

	@Override
	public void removeQueueSuffix(String user, String sessionId) {
		Map<String, String> suffixes = this.cache.get(user);
		if (suffixes != null) {
			if (suffixes.remove(sessionId) != null) {
				this.cache.remove(user, Collections.emptyMap());
			}
		}
	}

	@Override
	public Set<String> getUserQueueSuffixes(String user) {
		Map<String, String> suffixes = this.cache.get(user);
		return (suffixes != null) ? new HashSet<String>(suffixes.values()) : Collections.<String>emptySet();
	}

	@Override
	public String getUserQueueSuffix(String user, String sessionId) {
		Map<String, String> suffixes = this.cache.get(user);
		if (suffixes != null) {
			return suffixes.get(sessionId);
		}
		return null;
	}

}
