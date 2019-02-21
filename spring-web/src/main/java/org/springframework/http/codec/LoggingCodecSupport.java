/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec;

import org.apache.commons.logging.Log;

import org.springframework.http.HttpLogging;

/**
 * Base class for {@link org.springframework.core.codec.Encoder},
 * {@link org.springframework.core.codec.Decoder}, {@link HttpMessageReader}, or
 * {@link HttpMessageWriter} that uses a logger and shows potentially sensitive
 * request data.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class LoggingCodecSupport {

	protected final Log logger = HttpLogging.forLogName(getClass());

	/** Whether to log potentially sensitive info (form data at DEBUG and headers at TRACE). */
	private boolean enableLoggingRequestDetails = false;


	/**
	 * Whether to log form data at DEBUG level, and headers at TRACE level.
	 * Both may contain sensitive information.
	 * <p>By default set to {@code false} so that request details are not shown.
	 * @param enable whether to enable or not
	 */
	public void setEnableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	/**
	 * Whether any logging of values being encoded or decoded is explicitly
	 * disabled regardless of log level.
	 */
	public boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

}
