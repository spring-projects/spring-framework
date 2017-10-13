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

package org.springframework.cache.rest;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheManager implemented on top of unirest expection a RestEndpoint
 *
 * @author Pablo Verdugo Huerta @pablocloud
 * @since 4.3.X
 */
public class RestCacheManager implements CacheManager {

	private ConcurrentMap<String, RestCache> map = new ConcurrentHashMap<>(16);
	private final String urlRoot;
	private final Map<String, Class> classMapping;

	public RestCacheManager(String urlRoot, Map<String, Class> classMapping) {
		this.urlRoot = urlRoot;
		this.classMapping = classMapping;
	}

	@Override
	public Cache getCache(String name) {
		if (!map.containsKey(name)) {
			map.put(name, new RestCache(urlRoot.concat(name), classMapping.get(name)));
		}
		return map.get(name);
	}

	@Override
	public Collection<String> getCacheNames() {
		return map.keySet();
	}

}
