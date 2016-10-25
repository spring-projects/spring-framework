/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.core.convert.support;

import java.util.concurrent.CompletableFuture;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 * @author Rossen Stoyanchev
 */
public class ReactiveAdapterRegistryTests {

	private ReactiveAdapterRegistry adapterRegistry;

	@Before
	public void setUp() throws Exception {
		this.adapterRegistry = new ReactiveAdapterRegistry();
	}

	@Test
	public void getDefaultAdapters() throws Exception {
		testMonoAdapter(Mono.class);
		testFluxAdapter(Flux.class);
		testFluxAdapter(Publisher.class);
		testMonoAdapter(CompletableFuture.class);
		testFluxAdapter(Observable.class);
		testMonoAdapter(Single.class);
		testMonoAdapter(Completable.class);
		testFluxAdapter(Flowable.class);
		testFluxAdapter(io.reactivex.Observable.class);
		testMonoAdapter(io.reactivex.Single.class);
		testMonoAdapter(Maybe.class);
		testMonoAdapter(io.reactivex.Completable.class);
	}

	private void testFluxAdapter(Class<?> adapteeType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapterFrom(adapteeType);
		assertNotNull(adapter);
		assertTrue(adapter.getDescriptor().isMultiValue());

		adapter = this.adapterRegistry.getAdapterTo(adapteeType);
		assertNotNull(adapter);
		assertTrue(adapter.getDescriptor().isMultiValue());
	}

	private void testMonoAdapter(Class<?> adapteeType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapterFrom(adapteeType);
		assertNotNull(adapter);
		assertFalse(adapter.getDescriptor().isMultiValue());

		adapter = this.adapterRegistry.getAdapterTo(adapteeType);
		assertNotNull(adapter);
		assertFalse(adapter.getDescriptor().isMultiValue());
	}

}
