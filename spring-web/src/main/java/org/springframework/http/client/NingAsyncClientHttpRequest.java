package org.springframework.http.client;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

    private final URI uri;
    private final HttpMethod httpMethod;
    private final AsyncHttpClient asyncHttpClient;

    public NingAsyncClientHttpRequest(URI uri, HttpMethod httpMethod, AsyncHttpClient asyncHttpClient) {
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.asyncHttpClient = asyncHttpClient;
    }

    @Override
    public HttpMethod getMethod() {
        return this.httpMethod;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        AsyncHttpClient.BoundRequestBuilder boundRequestBuilder;
        String suri = uri.toString();
        switch (this.httpMethod) {
            case GET:
                boundRequestBuilder = this.asyncHttpClient.prepareGet(suri);
                break;
            case POST:
                boundRequestBuilder = this.asyncHttpClient.preparePost(suri);
                break;
            case HEAD:
                boundRequestBuilder = this.asyncHttpClient.prepareHead(suri);
                break;
            case OPTIONS:
                boundRequestBuilder = this.asyncHttpClient.prepareOptions(suri);
                break;
            case PUT:
                boundRequestBuilder = this.asyncHttpClient.preparePut(suri);
                break;
            case DELETE:
                boundRequestBuilder = this.asyncHttpClient.prepareDelete(suri);
                break;
            case PATCH:
                boundRequestBuilder = this.asyncHttpClient.preparePatch(suri);
                break;
            default:
                throw new IOException("Unsupported HttpMethod: " + this.httpMethod);
        }
        Map<String, Collection<String>> headersMap = new HashMap<String, Collection<String>>(headers.size());
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            headersMap.put(entry.getKey(), entry.getValue());
        }
        boundRequestBuilder = boundRequestBuilder.setHeaders(headersMap);

        //boundRequestBuilder = boundRequestBuilder.addBodyPart(new ByteArrayPart("body", "body", bufferedOutput, null, null));
        boundRequestBuilder = boundRequestBuilder.setBody(bufferedOutput);
        org.asynchttpclient.ListenableFuture<Response> ningResponse = boundRequestBuilder.execute();
        ListenableFuture<Response> springResponse =
                new NingListenableFutureToSpringListenableFuture<Response>(ningResponse);
        return new NingListenableFutureAdapter(springResponse);
    }

    public static final class NingListenableFutureAdapter extends ListenableFutureAdapter<ClientHttpResponse, Response> {

        public NingListenableFutureAdapter(ListenableFuture<Response> adaptee) {
            super(adaptee);
        }

        @Override
        protected ClientHttpResponse adapt(Response response) throws ExecutionException {
            return new NingAsyncClientHttpResponse(response);
        }

    }

    public static final class NingListenableFutureToSpringListenableFuture<T> implements ListenableFuture<T> {

        private final org.asynchttpclient.ListenableFuture<T> future;

        public NingListenableFutureToSpringListenableFuture(org.asynchttpclient.ListenableFuture<T> future) {
            this.future = future;
        }

        @Override
        public void addCallback(final ListenableFutureCallback<? super T> callback) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    try {
                        T result = future.get();
                        callback.onSuccess(result);
                    } catch (ExecutionException e) {
                        callback.onFailure(e.getCause());
                    } catch (InterruptedException e) {
                        callback.onFailure(e);
                    }

                }
            };
            this.future.addListener(runnable, ImmediateExecutor.INSTANCE);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return this.future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return this.future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return this.future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return this.future.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return this.future.get(timeout, unit);
        }
    }

    /**
     * {@link Executor} which execute tasks in the callers thread.
     */
    private static final class ImmediateExecutor implements Executor {
        public static final ImmediateExecutor INSTANCE = new ImmediateExecutor();

        private  ImmediateExecutor() {
            // use static instance
        }

        @Override
        public void execute(Runnable command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            command.run();
        }
    }
}
