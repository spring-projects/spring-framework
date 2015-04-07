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

import com.squareup.okhttp.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.net.URI;

/**
 * @author Luciano Leggieri
 */
public final class OkHttpClientHttpRequestFactory implements
        ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {

    private final OkHttpClient client;
    private final boolean locallyBuilt;

    public OkHttpClientHttpRequestFactory() {
        client = new OkHttpClient();
        locallyBuilt = true;
    }

    public OkHttpClientHttpRequestFactory(OkHttpClient okHttpClient) {
        Assert.notNull(okHttpClient, "'okHttpClient' must not be null");
        client = okHttpClient;
        locallyBuilt = false;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
        return new OkHttpClientHttpRequest(client, uri, httpMethod);
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
        return new OkHttpAsyncClientHttpRequest(client, uri, httpMethod);
    }

    @Override
    public void destroy() throws Exception {
        if (locallyBuilt) {
            if (this.client.getCache() != null) {
                this.client.getCache().close();
            }
            this.client.getDispatcher().getExecutorService().shutdown();
        }
    }
}
