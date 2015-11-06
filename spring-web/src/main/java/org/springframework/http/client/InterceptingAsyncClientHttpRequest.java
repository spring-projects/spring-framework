/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A {@link AsyncClientHttpRequest} wrapper that enriches it proceeds the actual request execution with calling
 * the registered interceptors.
 *
 * @author Jakub Narloch
 * @see InterceptingAsyncClientHttpRequestFactory
 */
class InterceptingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

    private AsyncClientHttpRequestFactory requestFactory;

    private List<AsyncClientHttpRequestInterceptor> interceptors;

    private URI uri;

    private HttpMethod httpMethod;

    /**
     * Creates new instance of {@link InterceptingAsyncClientHttpRequest}.
     *
     * @param requestFactory the async request factory
     * @param interceptors   the list of interceptors
     * @param uri            the request URI
     * @param httpMethod     the HTTP method
     */
    public InterceptingAsyncClientHttpRequest(AsyncClientHttpRequestFactory requestFactory,
                                              List<AsyncClientHttpRequestInterceptor> interceptors, URI uri,
                                              HttpMethod httpMethod) {

        this.requestFactory = requestFactory;
        this.interceptors = interceptors;
        this.uri = uri;
        this.httpMethod = httpMethod;
    }

    @Override
    protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] body) throws IOException {
        return new AsyncRequestExecution().executeAsync(this, body);
    }

    @Override
    public HttpMethod getMethod() {
        return httpMethod;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    private class AsyncRequestExecution implements AsyncClientHttpRequestExecution {

        private Iterator<AsyncClientHttpRequestInterceptor> nextInterceptor = interceptors.iterator();

        @Override
        public ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body) throws IOException {
            if (nextInterceptor.hasNext()) {
                AsyncClientHttpRequestInterceptor interceptor = nextInterceptor.next();
                ListenableFuture<ClientHttpResponse> future = interceptor.interceptRequest(request, body, this);
                return new IdentityListenableFutureAdapter<ClientHttpResponse>(future);
            }
            else {
                AsyncClientHttpRequest req = requestFactory.createAsyncRequest(uri, httpMethod);
                req.getHeaders().putAll(getHeaders());
                if (body.length > 0) {
                    StreamUtils.copy(body, req.getBody());
                }
                return req.executeAsync();
            }
        }
    }

    private static class IdentityListenableFutureAdapter<T> extends ListenableFutureAdapter<T, T> {

        protected IdentityListenableFutureAdapter(ListenableFuture<T> adaptee) {
            super(adaptee);
        }

        @Override
        protected T adapt(T adapteeResult) throws ExecutionException {
            return adapteeResult;
        }
    }
}
