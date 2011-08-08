/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet;

import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;

/**
 * Stores attributes that need to be made available in the next request.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class FlashMap extends ModelMap {

	private static final long serialVersionUID = 1L;

	private final String key;

	private final String keyParameterName;

	private long expirationStartTime;
	
	private int timeToLive;

	/**
	 * Create a FlashMap with a unique key.
	 */
	public FlashMap(String key, String keyParameterName) {
		Assert.notNull("The key is required", key);
		Assert.notNull("The key parameter name is required", keyParameterName);
		this.key = key;
		this.keyParameterName = keyParameterName;
	}

	/**
	 * Create a FlashMap without a key. 
	 */
	public FlashMap() {
		this.key = null;
		this.keyParameterName = null;
	}

	/**
	 * Return the key assigned to this FlashMap instance; 
	 * or {@code null} if a unique key has not been assigned.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Return the name of the request parameter to use when appending the flash 
	 * key to a redirect URL. 
	 */
	public String getKeyParameterName() {
		return keyParameterName;
	}

	/**
	 * Start the expiration period for this instance. After the given number of 
	 * seconds calls to {@link #isExpired()} will return "true".
	 * @param timeToLive the number of seconds before flash map expires
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationStartTime = System.currentTimeMillis();
		this.timeToLive = timeToLive;
	}

	/**
	 * Whether the flash map has expired depending on the number of seconds 
	 * elapsed since the call to {@link #startExpirationPeriod}.
	 */
	public boolean isExpired() {
		if (this.expirationStartTime != 0) {
			return (System.currentTimeMillis() - this.expirationStartTime) > this.timeToLive * 1000;
		}
		else {
			return false;
		}
	}

}
