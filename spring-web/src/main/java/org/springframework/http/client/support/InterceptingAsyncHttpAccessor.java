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

package org.springframework.http.client.support;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingAsyncClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The HTTP accessor that extends the base {@link AsyncHttpAccessor} with request intercepting functionality.
 *
 * @author Jakub Narloch
 */
public abstract class InterceptingAsyncHttpAccessor extends AsyncHttpAccessor {

    private List<AsyncClientHttpRequestInterceptor> interceptors = new ArrayList<AsyncClientHttpRequestInterceptor>();

    /**
     * Retrieves the list of interceptors.
     *
     * @return the list of interceptors
     */
    public List<AsyncClientHttpRequestInterceptor> getInterceptors() {
        return interceptors;
    }

    /**
     * Sets the list of interceptors.
     *
     * @param interceptors the list of interceptors
     */
    public void setInterceptors(List<AsyncClientHttpRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public AsyncClientHttpRequestFactory getAsyncRequestFactory() {
        AsyncClientHttpRequestFactory asyncRequestFactory = super.getAsyncRequestFactory();
        if(interceptors.isEmpty()) {
            return asyncRequestFactory;
        }
        return new InterceptingAsyncClientHttpRequestFactory(asyncRequestFactory, getInterceptors());
    }
}
