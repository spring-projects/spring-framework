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

import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Luciano Leggieri
 */
final class OkHttpRequestHelper {
    public OkHttpRequestHelper() {}

    static Request buildRequest(HttpMethod method, URI uri, HttpHeaders headers, RequestBody body)
            throws MalformedURLException {
        Request.Builder builder = new Request.Builder().method(method.name(), body);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                builder.addHeader(entry.getKey(), value);
            }
        }
        return builder.url(uri.toURL()).build();
    }

    static OkHttpRequestBody getRequestBody(OkHttpRequestBody body) {
        return body.isEmpty()?null:body;
    }
}
