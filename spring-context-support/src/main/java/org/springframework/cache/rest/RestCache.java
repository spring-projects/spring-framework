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

import com.mashape.unirest.http.Unirest;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Implementation on top of Unirest.
 *
 * @author Pablo Verdugo Huerta @pablocloud
 * @since 4.3.X
 */
public class RestCache extends AbstractValueAdaptingCache {

	private final String baseUrl;
	private final Class type;

	public RestCache(String baseUrl, Class type){
		super(false);
		this.baseUrl = baseUrl;
		this.type = type;
	}

	@Override
	protected Object lookup(Object key) {
		Object result = null;
		try {
			result = Unirest.get(baseUrl.concat("/").concat(key.toString())).asObjectAsync(type).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return baseUrl;
	}

	//TODO: Check what we should return here.
	@Override
	public Object getNativeCache() {
		return baseUrl;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) lookup(key);
	}

	@Override
	public void put(Object key, Object value) {
		try {
			Unirest.post(baseUrl.concat("/").concat(key.toString())).body(value).asStringAsync().get().getBody();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		return new SimpleValueWrapper(lookup(key));
	}

	@Override
	public void evict(Object key) {
		try {
			Unirest.delete(baseUrl.concat("/").concat(key.toString())).asStringAsync().get().getBody();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void clear() {
		try {
			Unirest.delete(baseUrl.concat("/")).asStringAsync().get().getBody();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
