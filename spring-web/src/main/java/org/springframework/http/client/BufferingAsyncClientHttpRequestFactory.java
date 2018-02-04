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

import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

/**
 * A buffered {@link AsyncClientHttpRequestFactory} adding buffering of the request and response streams.
 * Configuring the request factory allows for multiple reads of the response body stream.
 *
 * @author Jakub Narloch
 * @see AsyncClientHttpRequestFactory
 */
public class BufferingAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

    private AsyncClientHttpRequestFactory delegate;

    /**
     * Creates new instance of {@link BufferingAsyncClientHttpRequestFactory} class.
     *
     * @param delegate the delegates request factory
     */
    public BufferingAsyncClientHttpRequestFactory(AsyncClientHttpRequestFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
        AsyncClientHttpRequest request = delegate.createAsyncRequest(uri, httpMethod);
        if(!shouldBuffer(uri, httpMethod)) {
            return request;
        }
        return new BufferingAsyncClientHttpRequestWrapper(request);
    }

    protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
        return true;
    }
}
