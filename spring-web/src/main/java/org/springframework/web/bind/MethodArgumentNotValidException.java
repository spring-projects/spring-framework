/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.util.BindErrorUtils;

/**
 * {@link BindException} to be thrown when validation on an argument annotated
 * with {@code @Valid} fails.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends BindException implements ErrorResponse {

	private final MethodParameter parameter;

	private final ProblemDetail body;


	/**
	 * Constructor for {@link MethodArgumentNotValidException}.
	 * @param parameter the parameter that failed validation
	 * @param bindingResult the results of the validation
	 */
	public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
		super(bindingResult);
		this.parameter = parameter;
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), "Invalid request content.");
	}


	/**
	 * Return the method parameter that failed validation.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public Object[] getDetailMessageArguments(MessageSource source, Locale locale) {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors(), source, locale),
				BindErrorUtils.resolveAndJoin(getFieldErrors(), source, locale)};
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors()),
				BindErrorUtils.resolveAndJoin(getFieldErrors())};
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Validation failed for argument [")
				.append(this.parameter.getParameterIndex()).append("] in ")
				.append(this.parameter.getExecutable().toGenericString());
		BindingResult bindingResult = getBindingResult();
		if (bindingResult.getErrorCount() > 1) {
			sb.append(" with ").append(bindingResult.getErrorCount()).append(" errors");
		}
		sb.append(": ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append('[').append(error).append("] ");
		}
		return sb.toString();
	}

}
