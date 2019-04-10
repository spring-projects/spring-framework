/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.List;

/**
 * A {@link org.springframework.messaging.MessageChannel MessageChannel} that
 * maintains a list {@link org.springframework.messaging.support.ChannelInterceptor
 * ChannelInterceptors} and allows interception of message sending.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface InterceptableChannel {

	/**
	 * Set the list of channel interceptors clearing any existing interceptors.
	 */
	void setInterceptors(List<ChannelInterceptor> interceptors);

	/**
	 * Add a channel interceptor to the end of the list.
	 */
	void addInterceptor(ChannelInterceptor interceptor);

	/**
	 * Add a channel interceptor at the specified index.
	 */
	void addInterceptor(int index, ChannelInterceptor interceptor);

	/**
	 * Return the list of configured interceptors.
	 */
	List<ChannelInterceptor> getInterceptors();

	/**
	 * Remove the given interceptor.
	 */
	boolean removeInterceptor(ChannelInterceptor interceptor);

	/**
	 * Remove the interceptor at the given index.
	 */
	ChannelInterceptor removeInterceptor(int index);

}
