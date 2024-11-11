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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.FilterTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FilterTests {

	@Test
	public void whenFiltersCompleteMvcProcessesRequest() throws Exception {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filters(new ContinueFilter())
				.build();

		EntityExchangeResult<Void> exchangeResult = client.post().uri("/persons?name=Andy")
				.exchange()
				.expectStatus().isFound()
				.expectHeader().location("/person/1")
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("id"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void filtersProcessRequest() {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filters(new ContinueFilter(), new RedirectFilter())
				.build();

		client.post().uri("/persons?name=Andy")
				.exchange()
				.expectStatus().isFound()
				.expectHeader().location("/login");
	}

	@Test
	public void filterMappedBySuffix() {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filter(new RedirectFilter(), "*.html")
				.build();

		client.post().uri("/persons.html?name=Andy")
				.exchange()
				.expectStatus().isFound()
				.expectHeader().location("/login");
	}

	@Test
	public void filterWithExactMapping() {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filter(new RedirectFilter(), "/p", "/persons")
				.build();

		client.post().uri("/persons?name=Andy")
				.exchange()
				.expectStatus().isFound()
				.expectHeader().location("/login");
	}

	@Test
	public void filterSkipped() throws Exception {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filter(new RedirectFilter(), "/p", "/person")
				.build();

		EntityExchangeResult<Void> exchangeResult =
				client.post().uri("/persons?name=Andy")
						.exchange()
						.expectStatus().isFound()
						.expectHeader().location("/person/1")
						.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("id"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void filterWrapsRequestResponse() throws Exception {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filter(new WrappingRequestResponseFilter())
				.build();

		EntityExchangeResult<Void> exchangeResult =
				client.post().uri("/user").exchange().expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("principal", WrappingRequestResponseFilter.PRINCIPAL_NAME));
	}

	@Test
	public void filterWrapsRequestResponseAndPerformsAsyncDispatch() {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
				.filters(new WrappingRequestResponseFilter(), new ShallowEtagHeaderFilter())
				.build();

		client.get().uri("/persons/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentLength(53)
				.expectHeader().valueEquals("ETag", "\"0e37becb4f0c90709cb2e1efcc61eaa00\"")
				.expectBody().json("{\"name\":\"Lukas\",\"someDouble\":0.0,\"someBoolean\":false}");
	}


	@Controller
	private static class PersonController {

		@PostMapping(path="/persons")
		public String save(@Valid Person person, Errors errors, RedirectAttributes redirectAttrs) {
			if (errors.hasErrors()) {
				return "person/add";
			}
			redirectAttrs.addAttribute("id", "1");
			redirectAttrs.addFlashAttribute("message", "success!");
			return "redirect:/person/{id}";
		}

		@PostMapping("/user")
		public ModelAndView user(Principal principal) {
			return new ModelAndView("user/view", "principal", principal.getName());
		}

		@GetMapping("/forward")
		public String forward() {
			return "forward:/persons";
		}

		@GetMapping("persons/{id}")
		@ResponseBody
		public CompletableFuture<Person> getPerson() {
			return CompletableFuture.completedFuture(new Person("Lukas"));
		}
	}

	private static class ContinueFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request,
				HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(request, response);
		}
	}

	private static class WrappingRequestResponseFilter extends OncePerRequestFilter {

		public static final String PRINCIPAL_NAME = "WrapRequestResponseFilterPrincipal";


		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(new HttpServletRequestWrapper(request) {

				@Override
				public Principal getUserPrincipal() {
					return () -> PRINCIPAL_NAME;
				}

				// Like Spring Security does in HttpServlet3RequestFactory..

				@Override
				public AsyncContext getAsyncContext() {
					return super.getAsyncContext() != null ?
							new AsyncContextWrapper(super.getAsyncContext()) : null;
				}

			}, new HttpServletResponseWrapper(response));
		}
	}

	private static class RedirectFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException {

			response.sendRedirect("/login");
		}
	}


	private static class AsyncContextWrapper implements AsyncContext {

		private final AsyncContext delegate;

		public AsyncContextWrapper(AsyncContext delegate) {
			this.delegate = delegate;
		}

		@Override
		public ServletRequest getRequest() {
			return this.delegate.getRequest();
		}

		@Override
		public ServletResponse getResponse() {
			return this.delegate.getResponse();
		}

		@Override
		public boolean hasOriginalRequestAndResponse() {
			return this.delegate.hasOriginalRequestAndResponse();
		}

		@Override
		public void dispatch() {
			this.delegate.dispatch();
		}

		@Override
		public void dispatch(String path) {
			this.delegate.dispatch(path);
		}

		@Override
		public void dispatch(ServletContext context, String path) {
			this.delegate.dispatch(context, path);
		}

		@Override
		public void complete() {
			this.delegate.complete();
		}

		@Override
		public void start(Runnable run) {
			this.delegate.start(run);
		}

		@Override
		public void addListener(AsyncListener listener) {
			this.delegate.addListener(listener);
		}

		@Override
		public void addListener(AsyncListener listener, ServletRequest req, ServletResponse res) {
			this.delegate.addListener(listener, req, res);
		}

		@Override
		public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
			return this.delegate.createListener(clazz);
		}

		@Override
		public void setTimeout(long timeout) {
			this.delegate.setTimeout(timeout);
		}

		@Override
		public long getTimeout() {
			return this.delegate.getTimeout();
		}
	}
}
