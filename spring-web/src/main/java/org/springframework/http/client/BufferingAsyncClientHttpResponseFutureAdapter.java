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

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

import java.util.concurrent.ExecutionException;

/**
 * A {@link ListenableFutureAdapter} that returns buffered {@link ClientHttpResponse} that buffers the response
 * input stream.
 *
 * @author Jakub Narloch
 * @see BufferingAsyncClientHttpRequestFactory
 */
public class BufferingAsyncClientHttpResponseFutureAdapter extends ListenableFutureAdapter<ClientHttpResponse, ClientHttpResponse> {

    /**
     * Creates new instance of {@link BufferingAsyncClientHttpResponseFutureAdapter} with delegated listenable future.
     *
     * @param adaptee the delegated future
     */
    public BufferingAsyncClientHttpResponseFutureAdapter(ListenableFuture<ClientHttpResponse> adaptee) {
        super(adaptee);
    }

    @Override
    protected ClientHttpResponse adapt(ClientHttpResponse adapteeResult) throws ExecutionException {
        return new BufferingClientHttpResponseWrapper(adapteeResult);
    }
}
