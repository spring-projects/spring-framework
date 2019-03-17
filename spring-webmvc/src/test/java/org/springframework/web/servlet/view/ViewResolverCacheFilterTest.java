/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.Test;
import org.springframework.web.servlet.View;

public class ViewResolverCacheFilterTest {

	private interface ViewLoader {
		View load(String viewName);
	}

	private static class TestViewResolver extends AbstractCachingViewResolver {

		private final ViewLoader viewLoader;

		private TestViewResolver(ViewLoader viewLoader) {
			this.viewLoader = viewLoader;
		}

		@Override
		protected View loadView(String viewName, Locale locale) {
			return viewLoader.load(viewName);
		}
	}

	private final static String VIEW_NAME = "name";
	private final ViewLoader viewLoader = mock(ViewLoader.class);
	private final TestViewResolver resolver = new TestViewResolver(viewLoader);

	@Test
	public void viewWillBePlacedInCache() throws Exception {
		resolver.setViewCacheFilter((n, v, l) -> true);

		resolver.resolveViewName(VIEW_NAME, Locale.ENGLISH);
		resolver.resolveViewName(VIEW_NAME, Locale.ENGLISH);

		verify(viewLoader, times(1)).load(any());
	}

	@Test
	public void viewWillNotBePlacedInCached() throws Exception {
		resolver.setViewCacheFilter((n, v, l) -> false);

		resolver.resolveViewName(VIEW_NAME, Locale.ENGLISH);
		resolver.resolveViewName(VIEW_NAME, Locale.ENGLISH);

		verify(viewLoader, times(2)).load(any());
	}

	@Test
	public void verifyPassedParamsToFilter() throws Exception {
		View view = mock(View.class);
		when(viewLoader.load(any())).thenReturn(view);

		ViewCacheFilter filter = mock(ViewCacheFilter.class);
		resolver.setViewCacheFilter(filter);

		resolver.resolveViewName(VIEW_NAME, Locale.ENGLISH);

		verify(filter, times(1)).filter(eq(VIEW_NAME), eq(view), eq(Locale.ENGLISH));
	}
}
