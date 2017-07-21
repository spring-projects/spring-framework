/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;

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

    @SuppressWarnings("unchecked")
    @Test
    public void resolveAsyncAttributes() {

        TestBean testBean1 = new TestBean("Bean1");
        TestBean testBean2 = new TestBean("Bean2");
        Map<String, Object> attributes = new HashMap<>();
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
