/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.failures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ApplicationContextFailureProcessor;

/**
 * Demo {@link ApplicationContextFailureProcessor} for tests that tracks
 * {@linkplain LoadFailure load failures} that can be queried via
 * {@link #loadFailures}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public class TrackingApplicationContextFailureProcessor implements ApplicationContextFailureProcessor {

	public static final List<LoadFailure> loadFailures = Collections.synchronizedList(new ArrayList<>());


	@Override
	public void processLoadFailure(ApplicationContext context, @Nullable Throwable exception) {
		loadFailures.add(new LoadFailure(context, exception));
	}

	public record LoadFailure(ApplicationContext context, @Nullable Throwable exception) {}

}
