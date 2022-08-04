/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Result handler that prints {@link MvcResult} details to a given output
 * stream &mdash; for example: {@code System.out}, {@code System.err}, a
 * custom {@code java.io.PrintWriter}, etc.
 *
 * <p>An instance of this class is typically accessed via one of the
 * {@link MockMvcResultHandlers#print print} or {@link MockMvcResultHandlers#log log}
 * methods in {@link MockMvcResultHandlers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class PrintingResultHandler implements ResultHandler {

	private static final String MISSING_CHARACTER_ENCODING = "<no character encoding set>";

	private final ResultValuePrinter printer;


	/**
	 * Protected constructor.
	 * @param printer a {@link ResultValuePrinter} to do the actual writing
	 */
	protected PrintingResultHandler(ResultValuePrinter printer) {
		this.printer = printer;
	}

	/**
	 * Return the result value printer.
	 * @return the printer
	 */
	protected ResultValuePrinter getPrinter() {
		return this.printer;
	}

	/**
	 * Print {@link MvcResult} details.
	 */
	@Override
	public final void handle(MvcResult result) throws Exception {
		this.printer.printHeading("MockHttpServletRequest");
		printRequest(result.getRequest());

		this.printer.printHeading("Handler");
		printHandler(result.getHandler(), result.getInterceptors());

		this.printer.printHeading("Async");
		printAsyncResult(result);

		this.printer.printHeading("Resolved Exception");
		printResolvedException(result.getResolvedException());

		this.printer.printHeading("ModelAndView");
		printModelAndView(result.getModelAndView());

		this.printer.printHeading("FlashMap");
		printFlashMap(RequestContextUtils.getOutputFlashMap(result.getRequest()));

		this.printer.printHeading("MockHttpServletResponse");
		printResponse(result.getResponse());
	}

	/**
	 * Print the request.
	 */
	protected void printRequest(MockHttpServletRequest request) throws Exception {
		String body = (request.getCharacterEncoding() != null ?
				request.getContentAsString() : MISSING_CHARACTER_ENCODING);

		this.printer.printValue("HTTP Method", request.getMethod());
		this.printer.printValue("Request URI", request.getRequestURI());
		this.printer.printValue("Parameters", getParamsMultiValueMap(request));
		this.printer.printValue("Headers", getRequestHeaders(request));
		this.printer.printValue("Body", body);
		this.printer.printValue("Session Attrs", getSessionAttributes(request));
	}

	protected final HttpHeaders getRequestHeaders(MockHttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			headers.put(name, Collections.list(request.getHeaders(name)));
		}
		return headers;
	}

	protected final MultiValueMap<String, String> getParamsMultiValueMap(MockHttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
		params.forEach((name, values) -> {
			if (params.get(name) != null) {
				for (String value : values) {
					multiValueMap.add(name, value);
				}
			}
		});
		return multiValueMap;
	}

	protected final Map<String, Object> getSessionAttributes(MockHttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Enumeration<String> attrNames = session.getAttributeNames();
			if (attrNames != null) {
				return Collections.list(attrNames).stream().
						collect(Collectors.toMap(n -> n, session::getAttribute));
			}
		}
		return Collections.emptyMap();
	}

	protected void printAsyncResult(MvcResult result) throws Exception {
		HttpServletRequest request = result.getRequest();
		this.printer.printValue("Async started", request.isAsyncStarted());
		Object asyncResult = null;
		try {
			asyncResult = result.getAsyncResult(0);
		}
		catch (IllegalStateException ex) {
			// Not set
		}
		this.printer.printValue("Async result", asyncResult);
	}

	/**
	 * Print the handler.
	 */
	protected void printHandler(@Nullable Object handler, @Nullable HandlerInterceptor[] interceptors)
			throws Exception {

		if (handler == null) {
			this.printer.printValue("Type", null);
		}
		else {
			if (handler instanceof HandlerMethod) {
				HandlerMethod handlerMethod = (HandlerMethod) handler;
				this.printer.printValue("Type", handlerMethod.getBeanType().getName());
				this.printer.printValue("Method", handlerMethod);
			}
			else {
				this.printer.printValue("Type", handler.getClass().getName());
			}
		}
	}

	/**
	 * Print exceptions resolved through a HandlerExceptionResolver.
	 */
	protected void printResolvedException(@Nullable Exception resolvedException) throws Exception {
		if (resolvedException == null) {
			this.printer.printValue("Type", null);
		}
		else {
			this.printer.printValue("Type", resolvedException.getClass().getName());
		}
	}

	/**
	 * Print the ModelAndView.
	 */
	protected void printModelAndView(@Nullable ModelAndView mav) throws Exception {
		this.printer.printValue("View name", (mav != null) ? mav.getViewName() : null);
		this.printer.printValue("View", (mav != null) ? mav.getView() : null);
		if (mav == null || mav.getModel().size() == 0) {
			this.printer.printValue("Model", null);
		}
		else {
			for (String name : mav.getModel().keySet()) {
				if (!name.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
					Object value = mav.getModel().get(name);
					this.printer.printValue("Attribute", name);
					this.printer.printValue("value", value);
					Errors errors = (Errors) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
					if (errors != null) {
						this.printer.printValue("errors", errors.getAllErrors());
					}
				}
			}
		}
	}

	/**
	 * Print "output" flash attributes.
	 */
	protected void printFlashMap(FlashMap flashMap) throws Exception {
		if (ObjectUtils.isEmpty(flashMap)) {
			this.printer.printValue("Attributes", null);
		}
		else {
			flashMap.forEach((name, value) -> {
				this.printer.printValue("Attribute", name);
				this.printer.printValue("value", value);
			});
		}
	}

	/**
	 * Print the response.
	 */
	protected void printResponse(MockHttpServletResponse response) throws Exception {
		this.printer.printValue("Status", response.getStatus());
		this.printer.printValue("Error message", response.getErrorMessage());
		this.printer.printValue("Headers", getResponseHeaders(response));
		this.printer.printValue("Content type", response.getContentType());
		String body = (MediaType.APPLICATION_JSON_VALUE.equals(response.getContentType()) ?
				response.getContentAsString(StandardCharsets.UTF_8) : response.getContentAsString());
		this.printer.printValue("Body", body);
		this.printer.printValue("Forwarded URL", response.getForwardedUrl());
		this.printer.printValue("Redirected URL", response.getRedirectedUrl());
		printCookies(response.getCookies());
	}

	/**
	 * Print the supplied cookies in a human-readable form, assuming the
	 * {@link Cookie} implementation does not provide its own {@code toString()}.
	 * @since 4.2
	 */
	private void printCookies(Cookie[] cookies) {
		String[] cookieStrings = new String[cookies.length];
		for (int i = 0; i < cookies.length; i++) {
			Cookie cookie = cookies[i];
			cookieStrings[i] = new ToStringCreator(cookie)
				.append("name", cookie.getName())
				.append("value", cookie.getValue())
				.append("comment", cookie.getComment())
				.append("domain", cookie.getDomain())
				.append("maxAge", cookie.getMaxAge())
				.append("path", cookie.getPath())
				.append("secure", cookie.getSecure())
				.append("version", cookie.getVersion())
				.append("httpOnly", cookie.isHttpOnly())
				.toString();
		}
		this.printer.printValue("Cookies", cookieStrings);
	}

	protected final HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : response.getHeaderNames()) {
			headers.put(name, response.getHeaders(name));
		}
		return headers;
	}


	/**
	 * A contract for how to actually write result information.
	 */
	protected interface ResultValuePrinter {

		void printHeading(String heading);

		void printValue(String label, @Nullable Object value);
	}

}
