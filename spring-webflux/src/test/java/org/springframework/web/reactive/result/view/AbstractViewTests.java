package org.springframework.web.reactive.result.view;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.Model;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link AbstractView}.
 *
 * @author Sebastien Deleuze
 */
public class AbstractViewTests {

    private MockServerWebExchange exchange;

    @Before
    public void setup() {
        this.exchange = MockServerHttpRequest.get("/").toExchange();
    }

    @Test
    public void resolveAsyncAttributes() {

        TestBean testBean1 = new TestBean("Bean1");
        TestBean testBean2 = new TestBean("Bean2");
        Map<String, Object> attributes = new HashMap();
        attributes.put("attr1", Mono.just(testBean1));
        attributes.put("attr2", Flux.just(testBean1, testBean2));
        attributes.put("attr3", Single.just(testBean2));
        attributes.put("attr4", Observable.just(testBean1, testBean2));
        attributes.put("attr5", Mono.empty());

        TestView view = new TestView();
        StepVerifier.create(view.render(attributes, null, this.exchange)).verifyComplete();

        assertEquals(testBean1, view.attributes.get("attr1"));
        assertArrayEquals(new TestBean[] {testBean1, testBean2}, ((List<TestBean>)view.attributes.get("attr2")).toArray());
        assertEquals(testBean2, view.attributes.get("attr3"));
        assertArrayEquals(new TestBean[] {testBean1, testBean2}, ((List<TestBean>)view.attributes.get("attr4")).toArray());
        assertNull(view.attributes.get("attr5"));
    }


    private static class TestView extends AbstractView {

        private Map<String, Object> attributes;

        @Override
        protected Mono<Void> renderInternal(Map<String, Object> renderAttributes, MediaType contentType, ServerWebExchange exchange) {
            this.attributes = renderAttributes;
            return Mono.empty();
        }

        public Map<String, Object> getAttributes() {
            return this.attributes;
        }
    }
}
