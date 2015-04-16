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

import com.squareup.okhttp.Response;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Luciano Leggieri
 */
public final class OkHttpClientHttpResponse extends AbstractClientHttpResponse {

    private final Response response;
    private final HttpHeaders httpHeaders;

    public OkHttpClientHttpResponse(Response r) {
        response = r;
        httpHeaders = new HttpHeaders();
        for (String key : response.headers().names()) {
            for (String value : response.headers(key)) {
                httpHeaders.add(key, value);
            }
        }
    }

    @Override
    public int getRawStatusCode() {
        return response.code();
    }

    @Override
    public String getStatusText() {
        return response.message();
    }

    @Override
    public void close() {
        try {
            response.body().close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public InputStream getBody() {
        return response.body().byteStream();
    }

    @Override
    public HttpHeaders getHeaders() {
        return httpHeaders;
    }
}
