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
import java.util.Collections;
import java.util.List;

/**
 * The intercepting request factory.
 *
 * @author Jakub Narloch
 * @see InterceptingAsyncClientHttpRequest
 */
public class InterceptingAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

    private AsyncClientHttpRequestFactory delegate;

    private List<AsyncClientHttpRequestInterceptor> interceptors;

    /**
     * Creates new instance of {@link InterceptingAsyncClientHttpRequestFactory} with delegated request factory and
     * list of interceptors.
     *
     * @param delegate     the delegated request factory
     * @param interceptors the list of interceptors.
     */
    public InterceptingAsyncClientHttpRequestFactory(AsyncClientHttpRequestFactory delegate, List<AsyncClientHttpRequestInterceptor> interceptors) {

        this.delegate = delegate;
        this.interceptors = interceptors != null ? interceptors : Collections.<AsyncClientHttpRequestInterceptor>emptyList();
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {

        return new InterceptingAsyncClientHttpRequest(delegate, interceptors, uri, httpMethod);
    }
}
