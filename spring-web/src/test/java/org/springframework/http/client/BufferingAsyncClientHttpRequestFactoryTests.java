package org.springframework.http.client;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the {@link BufferingAsyncClientHttpRequestFactory} class.
 *
 * @author Jakub Narloch
 */
public class BufferingAsyncClientHttpRequestFactoryTests extends AbstractJettyServerTestCase {

    private AsyncClientHttpRequestFactory requestFactory;

    @Before
    public void setUp() throws Exception {

        requestFactory = new BufferingAsyncClientHttpRequestFactory(new HttpComponentsAsyncClientHttpRequestFactory());
    }

    @Test
    public void testResponseBuffering() throws Exception {

        byte[] message = "Async buffering works".getBytes("UTF-8");
        ResponseBodyExtractor originalResponse = new ResponseBodyExtractor();
        ResponseBodyExtractor responseCopy = new ResponseBodyExtractor();
        AsyncClientHttpRequest request = requestFactory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
        request.getHeaders().setContentLength(message.length);
        FileCopyUtils.copy(message, request.getBody());

        ListenableFuture<ClientHttpResponse> future = request.executeAsync();
        future.addCallback(originalResponse, ex -> fail(ex.getMessage()));
        future.addCallback(responseCopy, ex -> fail(ex.getMessage()));
        future.get();

        assertArrayEquals(originalResponse.body, responseCopy.body);
        assertArrayEquals(message, originalResponse.body);
    }

    public class ResponseBodyExtractor implements SuccessCallback<ClientHttpResponse> {

        private byte[] body;

        @Override
        public void onSuccess(ClientHttpResponse result) {
            try {
                body = StreamUtils.copyToByteArray(result.getBody());
            } catch (IOException e) {
                // ignores
            }
        }
    }
}