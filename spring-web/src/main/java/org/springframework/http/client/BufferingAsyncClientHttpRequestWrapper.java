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
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;

/**
 * A buffering {@link AsyncClientHttpRequest} allowing to buffer the request output stream.
 *
 * @author Jakub Narloch
 * @see BufferingAsyncClientHttpRequestFactory
 */
public class BufferingAsyncClientHttpRequestWrapper extends AbstractBufferingAsyncClientHttpRequest {

    private final AsyncClientHttpRequest request;

    public BufferingAsyncClientHttpRequestWrapper(AsyncClientHttpRequest request) {
        this.request = request;
    }

    @Override
    public HttpMethod getMethod() {
        return request.getMethod();
    }

    @Override
    public URI getURI() {
        return request.getURI();
    }

    @Override
    protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        request.getHeaders().putAll(headers);
        if(bufferedOutput.length > 0) {
            StreamUtils.copy(bufferedOutput, request.getBody());
        }
        return new BufferingAsyncClientHttpResponseFutureAdapter(request.executeAsync());
    }
}
