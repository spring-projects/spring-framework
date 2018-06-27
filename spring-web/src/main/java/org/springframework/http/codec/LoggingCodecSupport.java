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
import org.apache.commons.logging.LogFactory;

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

	protected final Log logger = LogFactory.getLog(getClass());

	/** Do not log potentially sensitive information (params at DEBUG and headers at TRACE). */
	private boolean disableLoggingRequestDetails = false;


	/**
	 * Whether to disable any logging of request details by this codec.
	 * By default values being encoded or decoded are logged at DEBUG and TRACE
	 * level under {@code "org.springframework.http.codec"} which may show
	 * sensitive data for form and multipart data. Typically that's not an issue
	 * since DEBUG and TRACE are intended for development, but this property may
	 * be used to explicitly disable any logging of such information regardless
	 * of the log level.
	 * <p>By default this is set to {@code false} in which case values encoded
	 * or decoded are logged at DEBUG level. When set to {@code true} values
	 * will not be logged at any level.
	 * @param disableLoggingRequestDetails whether to disable
	 */
	public void setDisableLoggingRequestDetails(boolean disableLoggingRequestDetails) {
		this.disableLoggingRequestDetails = disableLoggingRequestDetails;
	}

	/**
	 * Whether any logging of values being encoded or decoded is explicitly
	 * disabled regardless of log level.
	 */
	public boolean isDisableLoggingRequestDetails() {
		return this.disableLoggingRequestDetails;
	}

	/**
	 * Returns "true" if logger is at DEBUG level and the logging of values
	 * being encoded or decoded is not explicitly disabled.
	 */
	protected boolean shouldLogRequestDetails() {
		return !this.disableLoggingRequestDetails && logger.isDebugEnabled();
	}

}
