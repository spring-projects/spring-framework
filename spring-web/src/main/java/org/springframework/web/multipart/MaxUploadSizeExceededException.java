/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
			ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Maximum upload size exceeded");

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
		return HttpStatus.CONTENT_TOO_LARGE;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

}
