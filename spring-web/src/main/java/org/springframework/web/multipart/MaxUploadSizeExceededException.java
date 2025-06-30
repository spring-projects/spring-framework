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

package org.springframework.web.multipart;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

/**
 * MultipartException subclass thrown when an upload exceeds the
 * maximum upload size allowed.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class MaxUploadSizeExceededException extends MultipartException implements ErrorResponse {

	private final ProblemDetail body =
			ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum upload size exceeded");

	private final long maxUploadSize;


	/**
	 * Constructor for MaxUploadSizeExceededException.
	 * @param maxUploadSize the maximum upload size allowed,
	 * or -1 if the size limit isn't known
	 */
	public MaxUploadSizeExceededException(long maxUploadSize) {
		this(maxUploadSize, null);
	}

	/**
	 * Constructor for MaxUploadSizeExceededException.
	 * @param maxUploadSize the maximum upload size allowed,
	 * or -1 if the size limit isn't known
	 * @param ex root cause from multipart parsing API in use
	 */
	public MaxUploadSizeExceededException(long maxUploadSize, @Nullable Throwable ex) {
		super("Maximum upload size " + (maxUploadSize >= 0 ? "of " + maxUploadSize + " bytes " : "") + "exceeded", ex);
		this.maxUploadSize = maxUploadSize;
	}


	/**
	 * Return the maximum upload size allowed,
	 * or -1 if the size limit isn't known.
	 */
	public long getMaxUploadSize() {
		return this.maxUploadSize;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.PAYLOAD_TOO_LARGE;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

}
