package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

/**
 * An example extension of Spring's {@link DefaultLifecycleProcessor}.
 * </p>
 * All Lifecycle beans of a same phase start _at once_ rather than sequentially.
 * Phases still only complete after every bean has returned from start().
 * </p>
 * A start timeout can be specified for each phase; the default timeout is 10 seconds.
 * If any timeout is exceeded, an exception is thrown and the application context refresh() fails.
 * </p>
 * @author Francis Lalonde
 */
public class ConcurrentLifecycleProcessor extends DefaultLifecycleProcessor {

	private final Map<Integer, Long> timeoutsForStartPhases = new ConcurrentHashMap<>();

	private final long timeoutPerStartPhase = 10000;

	public void setTimeoutsForStartPhases(Map<Integer, Long> timeoutsForShutdownPhases) {
		this.timeoutsForStartPhases.putAll(timeoutsForShutdownPhases);
	}

	protected long determineStartTimeout(int phase) {
		Long timeout = this.timeoutsForStartPhases.get(phase);
		return (timeout != null ? timeout : this.timeoutPerStartPhase);
	}

	@Override
	protected void startBeans(boolean autoStartupOnly) {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, ConcurentLifecycleGroup> phases = new TreeMap<>();

		lifecycleBeans.forEach((beanName, bean) -> {
			if (!autoStartupOnly || isAutoStartupCandidate(beanName, bean)) {
				int startupPhase = getPhase(bean);
				phases.computeIfAbsent(startupPhase,
						phase -> new ConcurentLifecycleGroup(phase, determineStartTimeout(phase), determineStopTimeout(phase), lifecycleBeans, autoStartupOnly)
				).add(beanName, bean);
			}
		});

		if (!phases.isEmpty()) {
			phases.values().forEach(ConcurentLifecycleGroup::start);
		}
	}

	protected class ConcurentLifecycleGroup extends LifecycleGroup {

		private final Log logger = LogFactory.getLog(getClass());

		private final long startTimeout;

		public ConcurentLifecycleGroup(int phase, long startTimeout, long stopTimeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {
			super(phase, stopTimeout, lifecycleBeans, autoStartupOnly);
			this.startTimeout = startTimeout;
		}

		@Override
		public void start() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Starting beans in phase " + phase);
			}

			List<CompletableFuture<Void>> starting = members.stream()
					.map(member -> CompletableFuture.runAsync(() -> doStart(lifecycleBeans, member.name(), autoStartupOnly)))
					.toList();

			try {
				CompletableFuture.allOf(starting.toArray(CompletableFuture<?>[]::new))
						.get(startTimeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new IllegalStateException("Timeout exceeded starting beans in phase " + this.phase, e);
			} catch (InterruptedException | ExecutionException e) {
				throw new IllegalStateException("Error starting beans in phase " + this.phase, e);
			}
		}
	}
}
