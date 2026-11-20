/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
