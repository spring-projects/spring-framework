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

package org.springframework.web.server;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;

/**
 * Subclass of {@link ErrorResponseException} that accepts a "reason", and by
 * default maps that to the {@link ErrorResponseException#setDetail(String) "detail"}
 * of the {@code ProblemDetail}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ResponseStatusException extends ErrorResponseException {

	private final @Nullable String reason;


	/**
	 * Constructor with a response status.
	 * @param status the HTTP status (required)
	 */
	public ResponseStatusException(HttpStatusCode status) {
		this(status, null);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation.
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 */
	public ResponseStatusException(HttpStatusCode status, @Nullable String reason) {
		this(status, reason, null);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation, as well as a nested exception.
	 * @param rawStatusCode the HTTP status code value
	 * @param reason the associated reason (optional)
	 * @param cause a nested exception (optional)
	 * @since 5.3
	 */
	public ResponseStatusException(int rawStatusCode, @Nullable String reason, @Nullable Throwable cause) {
		this(HttpStatusCode.valueOf(rawStatusCode), reason, cause);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation, as well as a nested exception.
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 * @param cause a nested exception (optional)
	 */
	public ResponseStatusException(HttpStatusCode status, @Nullable String reason, @Nullable Throwable cause) {
		this(status, reason, cause, null, null);
	}

	/**
	 * Constructor with a message code and arguments for resolving the error
	 * "detail" via {@link org.springframework.context.MessageSource}.
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 * @param cause a nested exception (optional)
	 * @since 6.0
	 */
	protected ResponseStatusException(
			HttpStatusCode status, @Nullable String reason, @Nullable Throwable cause,
			@Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

		super(status, new ResponseStatusProblemDetail(status, reason, messageDetailCode == null), cause,
				messageDetailCode, messageDetailArguments);
		this.reason = reason;
	}


	/**
	 * The reason explaining the exception (potentially {@code null} or empty).
	 */
	public @Nullable String getReason() {
		return this.reason;
	}

	/**
	 * Return headers to add to the error response, for example, "Allow", "Accept", etc.
	 */
	@Override
	public HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}

	@Override
	public ProblemDetail updateAndGetBody(@Nullable MessageSource messageSource, Locale locale) {
		ResponseStatusProblemDetail body = (ResponseStatusProblemDetail) getBody();
		boolean reasonLocalizationAllowed = body.isReasonLocalizationAllowed();
		super.updateAndGetBody(messageSource, locale);

		// The reason may be a code (consistent with ResponseStatusExceptionResolver)

		String reason = getReason();
		if (reasonLocalizationAllowed && messageSource != null && reason != null &&
				isDefaultDetailMessageCode()) {
			Object[] arguments = getDetailMessageArguments(messageSource, locale);
			String resolved = messageSource.getMessage(reason, arguments, null, locale);
			if (resolved != null) {
				body.setDetailFromReason(resolved);
			}
		}
		body.setReasonLocalizationAllowed(reasonLocalizationAllowed);

		return getBody();
	}

	private boolean isDefaultDetailMessageCode() {
		return getDetailMessageCode().equals(ErrorResponse.getDefaultDetailMessageCode(getClass(), null));
	}

	@Override
	public String getMessage() {
		return getStatusCode() + (this.reason != null ? " \"" + this.reason + "\"" : "");
	}


	private static final class ResponseStatusProblemDetail extends ProblemDetail {

		private static final long serialVersionUID = 1L;

		private boolean reasonLocalizationAllowed;


		ResponseStatusProblemDetail(
				HttpStatusCode status, @Nullable String reason, boolean reasonLocalizationAllowed) {

			super(ProblemDetail.forStatus(status));
			this.reasonLocalizationAllowed = reasonLocalizationAllowed;
			super.setDetail(reason);
		}

		@Override
		public void setDetail(@Nullable String detail) {
			super.setDetail(detail);
			this.reasonLocalizationAllowed = false;
		}

		boolean isReasonLocalizationAllowed() {
			return this.reasonLocalizationAllowed;
		}

		void setDetailFromReason(String detail) {
			super.setDetail(detail);
		}

		void setReasonLocalizationAllowed(boolean allowed) {
			this.reasonLocalizationAllowed = allowed;
		}

	}

}
