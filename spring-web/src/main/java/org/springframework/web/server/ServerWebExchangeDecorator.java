/*
 * Copyright 2002-2019 the original author or authors.
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

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * A convenient base class for classes that need to wrap another
 * {@link ServerWebExchange}. Pre-implements all methods by delegating to the
 * wrapped instance.
 *
 * <p><strong>Note:</strong> if the purpose for using a decorator is to override
 * properties like {@link #getPrincipal()}, consider using
 * {@link ServerWebExchange#mutate()} instead.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 *
 * @see ServerWebExchange#mutate()
 */
public class ServerWebExchangeDecorator implements ServerWebExchange {

	private final ServerWebExchange delegate;


	protected ServerWebExchangeDecorator(ServerWebExchange delegate) {
		Assert.notNull(delegate, "ServerWebExchange 'delegate' is required.");
		this.delegate = delegate;
	}


	public ServerWebExchange getDelegate() {
		return this.delegate;
	}

	// ServerWebExchange delegation methods...

	@Override
	public ServerHttpRequest getRequest() {
		return getDelegate().getRequest();
	}

	@Override
	public ServerHttpResponse getResponse() {
		return getDelegate().getResponse();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return getDelegate().getAttributes();
	}

	@Override
	public Mono<WebSession> getSession() {
		return getDelegate().getSession();
	}

	@Override
	public <T extends Principal> Mono<T> getPrincipal() {
		return getDelegate().getPrincipal();
	}

	@Override
	public LocaleContext getLocaleContext() {
		return getDelegate().getLocaleContext();
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return getDelegate().getApplicationContext();
	}

	@Override
	public Mono<MultiValueMap<String, String>> getFormData() {
		return getDelegate().getFormData();
	}

	@Override
	public Mono<MultiValueMap<String, Part>> getMultipartData() {
		return getDelegate().getMultipartData();
	}

	@Override
	public boolean isNotModified() {
		return getDelegate().isNotModified();
	}

	@Override
	public boolean checkNotModified(Instant lastModified) {
		return getDelegate().checkNotModified(lastModified);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return getDelegate().checkNotModified(etag);
	}

	@Override
	public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
		return getDelegate().checkNotModified(etag, lastModified);
	}

	@Override
	public String transformUrl(String url) {
		return getDelegate().transformUrl(url);
	}

	@Override
	public void addUrlTransformer(Function<String, String> transformer) {
		getDelegate().addUrlTransformer(transformer);
	}

	@Override
	public String getLogPrefix() {
		return getDelegate().getLogPrefix();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
