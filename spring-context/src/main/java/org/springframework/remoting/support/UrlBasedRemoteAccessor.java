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

package org.springframework.remoting.support;

import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract base class for classes that access remote services via URLs.
 * Provides a "serviceUrl" bean property, which is considered as required.
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 */
public abstract class UrlBasedRemoteAccessor extends RemoteAccessor implements InitializingBean {

	private String serviceUrl;


	/**
	 * Set the URL of this remote accessor's target service.
	 * The URL must be compatible with the rules of the particular remoting provider.
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * Return the URL of this remote accessor's target service.
	 */
	public String getServiceUrl() {
		return this.serviceUrl;
	}


	@Override
	public void afterPropertiesSet() {
		if (getServiceUrl() == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}
	}

}
