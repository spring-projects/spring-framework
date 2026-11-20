/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
