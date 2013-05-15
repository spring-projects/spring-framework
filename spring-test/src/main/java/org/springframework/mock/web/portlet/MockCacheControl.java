/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.mock.web.portlet;

import javax.portlet.CacheControl;

/**
 * Mock implementation of the {@link javax.portlet.CacheControl} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MockCacheControl implements CacheControl {

	private int expirationTime = 0;

	private boolean publicScope = false;

	private String etag;

	private boolean useCachedContent = false;


	@Override
	public int getExpirationTime() {
		return this.expirationTime;
	}

	@Override
	public void setExpirationTime(int time) {
		this.expirationTime = time;
	}

	@Override
	public boolean isPublicScope() {
		return this.publicScope;
	}

	@Override
	public void setPublicScope(boolean publicScope) {
		this.publicScope = publicScope;
	}

	@Override
	public String getETag() {
		return this.etag;
	}

	@Override
	public void setETag(String token) {
		this.etag = token;
	}

	@Override
	public boolean useCachedContent() {
		return this.useCachedContent;
	}

	@Override
	public void setUseCachedContent(boolean useCachedContent) {
		this.useCachedContent = useCachedContent;
	}

}
