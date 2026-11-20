/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LogAccessor}.
 *
 * @author Sam Brannen
 * @since 7.0.10
 */
class LogAccessorTests {

	private final Log log = mock();

	private final LogAccessor logAccessor = new LogAccessor(this.log);


	@BeforeEach
	void renderMessagesWhenDelegatingToLog() {
		// Simulate a Log implementation (such as commons-logging's SLF4J bridge) that renders the
		// message unconditionally, in order to verify that LogAccessor itself guards against that.
		Answer<Void> render = invocation -> {
			invocation.getArgument(0).toString();
			return null;
		};
		doAnswer(render).when(this.log).fatal(any());
		doAnswer(render).when(this.log).fatal(any(), any());
		doAnswer(render).when(this.log).error(any());
		doAnswer(render).when(this.log).error(any(), any());
		doAnswer(render).when(this.log).warn(any());
		doAnswer(render).when(this.log).warn(any(), any());
		doAnswer(render).when(this.log).info(any());
		doAnswer(render).when(this.log).info(any(), any());
		doAnswer(render).when(this.log).debug(any());
		doAnswer(render).when(this.log).debug(any(), any());
		doAnswer(render).when(this.log).trace(any());
		doAnswer(render).when(this.log).trace(any(), any());
	}

	@ParameterizedTest
	@FieldSource("levels")
	void messageIsNotRenderedWhenLevelIsDisabled(LevelEnabler enabler, LogInvoker invoker) {
		enabler.enable(this.log, false);

		AtomicBoolean rendered = new AtomicBoolean();
		invoker.invoke(this.logAccessor, lazyMessage(rendered));

		assertThat(rendered).isFalse();
	}

	@ParameterizedTest
	@FieldSource("levels")
	void messageIsRenderedWhenLevelIsEnabled(LevelEnabler enabler, LogInvoker invoker) {
		enabler.enable(this.log, true);

		AtomicBoolean rendered = new AtomicBoolean();
		invoker.invoke(this.logAccessor, lazyMessage(rendered));

		assertThat(rendered).isTrue();
	}

	@ParameterizedTest
	@FieldSource("levelsWithCause")
	void messageWithCauseIsNotRenderedWhenLevelIsDisabled(LevelEnabler enabler, CauseLogInvoker invoker) {
		enabler.enable(this.log, false);

		AtomicBoolean rendered = new AtomicBoolean();
		invoker.invoke(this.logAccessor, new RuntimeException(), lazyMessage(rendered));

		assertThat(rendered).isFalse();
	}

	@ParameterizedTest
	@FieldSource("levelsWithCause")
	void messageWithCauseIsRenderedWhenLevelIsEnabled(LevelEnabler enabler, CauseLogInvoker invoker) {
		enabler.enable(this.log, true);

		AtomicBoolean rendered = new AtomicBoolean();
		invoker.invoke(this.logAccessor, new RuntimeException(), lazyMessage(rendered));

		assertThat(rendered).isTrue();
	}


	private static LogMessage lazyMessage(AtomicBoolean rendered) {
		return LogMessage.of(() -> {
			rendered.set(true);
			return "message";
		});
	}

	@SuppressWarnings("unused")
	private static List<Arguments> levels = List.of(
			argumentSet("fatal", (LevelEnabler) (log, enabled) -> when(log.isFatalEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::fatal),
			argumentSet("error", (LevelEnabler) (log, enabled) -> when(log.isErrorEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::error),
			argumentSet("warn", (LevelEnabler) (log, enabled) -> when(log.isWarnEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::warn),
			argumentSet("info", (LevelEnabler) (log, enabled) -> when(log.isInfoEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::info),
			argumentSet("debug", (LevelEnabler) (log, enabled) -> when(log.isDebugEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::debug),
			argumentSet("trace", (LevelEnabler) (log, enabled) -> when(log.isTraceEnabled()).thenReturn(enabled),
					(LogInvoker) LogAccessor::trace)
		);

	@SuppressWarnings("unused")
	private static List<Arguments> levelsWithCause = List.of(
			argumentSet("fatal", (LevelEnabler) (log, enabled) -> when(log.isFatalEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::fatal),
			argumentSet("error", (LevelEnabler) (log, enabled) -> when(log.isErrorEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::error),
			argumentSet("warn", (LevelEnabler) (log, enabled) -> when(log.isWarnEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::warn),
			argumentSet("info", (LevelEnabler) (log, enabled) -> when(log.isInfoEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::info),
			argumentSet("debug", (LevelEnabler) (log, enabled) -> when(log.isDebugEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::debug),
			argumentSet("trace", (LevelEnabler) (log, enabled) -> when(log.isTraceEnabled()).thenReturn(enabled),
					(CauseLogInvoker) LogAccessor::trace)
		);


	@FunctionalInterface
	private interface LevelEnabler {

		void enable(Log log, boolean enabled);
	}

	@FunctionalInterface
	private interface LogInvoker {

		void invoke(LogAccessor logAccessor, CharSequence message);
	}

	@FunctionalInterface
	private interface CauseLogInvoker {

		void invoke(LogAccessor logAccessor, Throwable cause, CharSequence message);
	}

}
