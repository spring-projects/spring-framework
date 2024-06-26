/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.testfixture;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.transaction.reactive.ReactiveAdapterRegistryProvider;

/**
 * @author Bohdan Pryshedko
 */
public class DelegatingReactiveAdapterRegistryProvider implements ReactiveAdapterRegistryProvider {
	@Override
	public ReactiveAdapterRegistry get() {
		return InstanceHolder.INSTANCE;
	}

	private static final class InstanceHolder {
		private static final ReactiveAdapterRegistry INSTANCE = new DelegatingReactiveAdapterRegistry();
	}

	private static final class DelegatingReactiveAdapterRegistry extends ReactiveAdapterRegistry {
		@Override
		public boolean hasAdapters() {
			return super.hasAdapters() || ReactiveAdapterRegistry.getSharedInstance().hasAdapters();
		}

		@Override
		public ReactiveAdapter getAdapter(Class<?> reactiveType) {
			var result = ReactiveAdapterRegistry.getSharedInstance().getAdapter(reactiveType);
			if (result == null) {
				return super.getAdapter(reactiveType);
			}
			return result;
		}

		@Override
		public ReactiveAdapter getAdapter(Class<?> reactiveType, Object source) {
			var result = ReactiveAdapterRegistry.getSharedInstance().getAdapter(reactiveType, source);
			if (result == null) {
				return super.getAdapter(reactiveType, source);
			}
			return result;
		}
	}
}
