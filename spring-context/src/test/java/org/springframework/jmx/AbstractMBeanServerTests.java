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

package org.springframework.jmx;

import java.net.BindException;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.AbstractMBeanServerTests.BindExceptionHandler;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.MBeanTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>If you run into the <em>"Unsupported protocol: jmxmp"</em> error, you will need to
 * download the <a href="https://www.oracle.com/technetwork/java/javase/tech/download-jsp-141676.html">JMX
 * Remote API 1.0.1_04 Reference Implementation</a> from Oracle and extract
 * {@code jmxremote_optional.jar} into your classpath, for example in the {@code lib/ext}
 * folder of your JVM.
 *
 * <p>See also: <a href="https://jira.spring.io/browse/SPR-8093">SPR-8093</a>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Stephane Nicoll
 */
@ExtendWith(BindExceptionHandler.class)
public abstract class AbstractMBeanServerTests {

	protected MBeanServer server;


	@BeforeEach
	public final void setUp() throws Exception {
		this.server = MBeanServerFactory.createMBeanServer();
		try {
			onSetUp();
		}
		catch (Exception ex) {
			releaseServer();
			throw ex;
		}
	}

	@AfterEach
	protected void tearDown() throws Exception {
		releaseServer();
		onTearDown();
	}

	private void releaseServer() {
		try {
			MBeanServerFactory.releaseMBeanServer(getServer());
		}
		catch (IllegalArgumentException ex) {
			if (!ex.getMessage().contains("not in list")) {
				throw ex;
			}
		}
		MBeanTestUtils.resetMBeanServers();
	}

	protected final ConfigurableApplicationContext loadContext(String configLocation) {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(configLocation);
		ctx.getDefaultListableBeanFactory().registerSingleton("server", getServer());
		ctx.refresh();
		return ctx;
	}

	protected void onSetUp() throws Exception {
	}

	protected void onTearDown() {
	}

	protected final MBeanServer getServer() {
		return this.server;
	}

	/**
	 * Start the specified {@link MBeanExporter}.
	 */
	protected void start(MBeanExporter exporter) {
		exporter.afterPropertiesSet();
		exporter.afterSingletonsInstantiated();
	}

	protected void assertIsRegistered(String message, ObjectName objectName) {
		assertThat(getServer().isRegistered(objectName)).as(message).isTrue();
	}

	protected void assertIsNotRegistered(String message, ObjectName objectName) {
		assertThat(getServer().isRegistered(objectName)).as(message).isFalse();
	}


	static class BindExceptionHandler implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

		@Override
		public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
			handleBindException(throwable);
		}

		@Override
		public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
				throws Throwable {
			handleBindException(throwable);
		}

		@Override
		public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
				throws Throwable {
			handleBindException(throwable);
		}

		private void handleBindException(Throwable throwable) throws Throwable {
			// Abort test?
			if (throwable instanceof BindException) {
				throw new TestAbortedException("Failed to bind to MBeanServer", throwable);
			}
			// Else rethrow to conform to the contracts of TestExecutionExceptionHandler and LifecycleMethodExecutionExceptionHandler
			throw throwable;
		}

	}

}

