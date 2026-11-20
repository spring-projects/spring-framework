/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.support.CronTrigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Juergen Hoeller
 * @since 6.1.3
 */
@EnabledForTestGroups(LONG_RUNNING)
class CronTriggerExecutionTests {

	ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

	AtomicInteger count = new AtomicInteger();

	Runnable quick = count::incrementAndGet;

	Runnable slow = () -> {
		count.incrementAndGet();
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	};


	@BeforeEach
	void initialize() {
		scheduler.initialize();
	}

	@AfterEach
	void shutdown() {
		scheduler.shutdown();
	}


	@Test
	void forLenientExecutionQuick() throws Exception {
		scheduler.schedule(quick, CronTrigger.forLenientExecution("*/1 * * * * *"));
		Thread.sleep(2000);
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	void forLenientExecutionSlow() throws Exception {
		scheduler.schedule(slow, CronTrigger.forLenientExecution("*/1 * * * * *"));
		Thread.sleep(2000);
		assertThat(count.get()).isEqualTo(1);
	}

	@Test
	void forFixedExecutionQuick() throws Exception {
		scheduler.schedule(quick, CronTrigger.forFixedExecution("*/1 * * * * *"));
		Thread.sleep(2000);
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	void forFixedExecutionSlow() throws Exception {
		scheduler.schedule(slow, CronTrigger.forFixedExecution("*/1 * * * * *"));
		Thread.sleep(2000);
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	void resumeLenientExecution() throws Exception {
		scheduler.schedule(quick, CronTrigger.resumeLenientExecution("*/1 * * * * *",
				Clock.systemDefaultZone().instant().minusSeconds(2)));
		Thread.sleep(1000);
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	void resumeFixedExecution() throws Exception {
		scheduler.schedule(quick, CronTrigger.resumeFixedExecution("*/1 * * * * *",
				Clock.systemDefaultZone().instant().minusSeconds(2)));
		Thread.sleep(1000);
		assertThat(count.get()).isEqualTo(3);
	}

}
