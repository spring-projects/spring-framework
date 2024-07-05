/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link View} that enables rendering of a collection of fragments, each with
 * its own view and model, also inheriting common attributes from the top-level model.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public class FragmentsView implements SmartView {

	private final Collection<ModelAndView> modelAndViews;


	/**
	 * Protected constructor to allow extension.
	 */
	protected FragmentsView(Collection<ModelAndView> modelAndViews) {
		this.modelAndViews = modelAndViews;
	}


	@Override
	public boolean isRedirectView() {
		return false;
	}

	@Override
	public void resolveNestedViews(ViewResolver resolver, Locale locale) throws Exception {
		for (ModelAndView mv : this.modelAndViews) {
			View view = resolveView(resolver, locale, mv);
			mv.setView(view);
		}
	}

	private static View resolveView(ViewResolver viewResolver, Locale locale, ModelAndView mv) throws Exception {
		String viewName = mv.getViewName();
		View view = (viewName != null ? viewResolver.resolveViewName(viewName, locale) : mv.getView());
		if (view == null) {
			throw new ServletException("Could not resolve view in " + mv);
		}
		return view;
	}

	@Override
	public void render(
			@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (model != null) {
			model.forEach((key, value) ->
					this.modelAndViews.forEach(mv -> mv.getModel().putIfAbsent(key, value)));
		}

		HttpServletResponse nonClosingResponse = new NonClosingHttpServletResponse(response);
		for (ModelAndView mv : this.modelAndViews) {
			Assert.state(mv.getView() != null, "Expected View");
			mv.getView().render(mv.getModel(), request, nonClosingResponse);
			response.flushBuffer();
		}
	}


	@Override
	public String toString() {
		return "FragmentsView " + this.modelAndViews;
	}


	/**
	 * Factory method to create an instance with the given fragments.
	 * @param modelAndViews the {@link ModelAndView} to use
	 * @return the created {@code FragmentsView} instance
	 */
	public static FragmentsView create(Collection<ModelAndView> modelAndViews) {
		return new FragmentsView(modelAndViews);
	}


	/**
	 * Response wrapper that in turn applies {@link NonClosingServletOutputStream}.
	 */
	private static final class NonClosingHttpServletResponse extends HttpServletResponseWrapper {

		@Nullable
		private ServletOutputStream os;

		public NonClosingHttpServletResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (this.os == null) {
				this.os = new NonClosingServletOutputStream(getResponse().getOutputStream());
			}
			return this.os;
		}
	}


	/**
	 * {@code OutputStream} that leaves the response open, ignoring calls to close it.
	 */
	private static final class NonClosingServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream os;

		public NonClosingServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			this.os.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.os.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException();
		}
	}

}
