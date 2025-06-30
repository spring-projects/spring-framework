/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.SessionCookieConfig;
import org.jspecify.annotations.Nullable;

/**
 * Mock implementation of the {@link jakarta.servlet.SessionCookieConfig} interface.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see jakarta.servlet.ServletContext#getSessionCookieConfig()
 */
public class MockSessionCookieConfig implements SessionCookieConfig {

	private @Nullable String name;

	private @Nullable String domain;

	private @Nullable String path;

	private @Nullable String comment;

	private boolean httpOnly;

	private boolean secure;

	private int maxAge = -1;

	private Map<String, String> attributes = new LinkedHashMap<>();


	@Override
	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Override
	public @Nullable String getName() {
		return this.name;
	}

	@Override
	public void setDomain(@Nullable String domain) {
		this.domain = domain;
	}

	@Override
	public @Nullable String getDomain() {
		return this.domain;
	}

	@Override
	public void setPath(@Nullable String path) {
		this.path = path;
	}

	@Override
	public @Nullable String getPath() {
		return this.path;
	}

	@SuppressWarnings("removal")
	@Override
	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@SuppressWarnings("removal")
	@Override
	public @Nullable String getComment() {
		return this.comment;
	}

	@Override
	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	@Override
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	@Override
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	@Override
	public int getMaxAge() {
		return this.maxAge;
	}

	@Override
	public void setAttribute(String name, String value) {
		this.attributes.put(name, value);
	}

	@Override
	public @Nullable String getAttribute(String name) {
		return this.attributes.get(name);
	}

	@Override
	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(this.attributes);
	}

}
