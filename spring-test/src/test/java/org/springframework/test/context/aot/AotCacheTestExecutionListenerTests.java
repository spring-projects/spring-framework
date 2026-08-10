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

package org.springframework.test.context.aot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AotCacheTestExecutionListener}.
 *
 * @author Vasily Pelikh
 * @since 7.1
 */
class AotCacheTestExecutionListenerTests {

	private final AotCacheTestExecutionListener listener = new AotCacheTestExecutionListener();

	@AfterEach
	void clearProperties() {
		SpringProperties.setProperty(DefaultLifecycleProcessor.EXIT_PROPERTY_NAME, null);
	}

	@Test
	void orderValue() {
		assertThat(listener.getOrder()).isEqualTo(3006);
	}

	@Test
	void beforeTestClassWhenRecordingIsNotEnabled() throws Exception {
		TestContext testContext = mock();
		listener.beforeTestClass(testContext);
		verify(testContext, never()).getApplicationContext();
	}

	@Test
	void beforeTestClassWhenAotCacheOutputFlagIsPresent() throws Exception {
		ApplicationContext applicationContext = mock();
		TestContext testContext = mock();
		given((Class) testContext.getTestClass()).willReturn((Class) AotCacheTestExecutionListenerTests.class);
		given(testContext.getApplicationContext()).willReturn(applicationContext);

		AotCacheTestExecutionListener recordingListener = listenerWithArgs("-XX:AOTCacheOutput=build/app.aot");
		recordingListener.beforeTestClass(testContext);

		verify(testContext).getApplicationContext();
	}

	@Test
	void beforeTestClassWhenJdkVersionIsUnsupported() {
		AotCacheTestExecutionListener unsupportedListener = new AotCacheTestExecutionListener() {
			@Override
			protected List<String> getInputArguments() {
				return List.of("-XX:AOTCacheOutput=build/app.aot");
			}

			@Override
			protected int getRequiredJavaFeatureVersion() {
				return 9999;
			}
		};
		TestContext testContext = mock();
		given((Class) testContext.getTestClass()).willReturn((Class) AotCacheTestExecutionListenerTests.class);

		assertThatIllegalStateException()
			.isThrownBy(() -> unsupportedListener.beforeTestClass(testContext))
			.withMessageContaining("JDK");
		verify(testContext, never()).getApplicationContext();
	}

	@Test
	void beforeTestClassWhenClassLoaderIsNotStandard() throws Exception {
		AotCacheTestExecutionListener warnListener = new AotCacheTestExecutionListener() {
			@Override
			protected List<String> getInputArguments() {
				return List.of("-XX:AOTCacheOutput=build/app.aot");
			}

			@Override
			protected boolean isStandardClassLoader(ClassLoader classLoader) {
				return false;
			}
		};
		ApplicationContext applicationContext = mock();
		given(applicationContext.getClassLoader()).willReturn(AotCacheTestExecutionListenerTests.class.getClassLoader());
		TestContext testContext = mock();
		given((Class) testContext.getTestClass()).willReturn((Class) AotCacheTestExecutionListenerTests.class);
		given(testContext.getApplicationContext()).willReturn(applicationContext);

		warnListener.beforeTestClass(testContext);

		verify(testContext).getApplicationContext();
	}

	@Test
	void isExitOnRefreshConfiguredWhenPropertyIsSet() {
		SpringProperties.setProperty(DefaultLifecycleProcessor.EXIT_PROPERTY_NAME, "onRefresh");
		assertThat(listener.isExitOnRefreshConfigured()).isTrue();
	}

	@Test
	void isExitOnRefreshConfiguredWhenPropertyIsNotSet() {
		assertThat(listener.isExitOnRefreshConfigured()).isFalse();
	}

	@Test
	void isAotRecordingEnabledWhenAotCacheOutputFlagIsPresent() {
		assertThat(AotCacheTestExecutionListener.isAotRecordingEnabled(
				List.of("-Xmx512m", "-XX:AOTCacheOutput=build/app.aot", "-jar", "app.jar"))).isTrue();
	}

	@Test
	void isAotRecordingEnabledWhenAotModeRecordFlagIsPresentIsNotSupported() {
		// JDK 24 two-step record mode is not supported; only the JDK 25+ single-step flag
		assertThat(AotCacheTestExecutionListener.isAotRecordingEnabled(
				List.of("-XX:AOTMode=record", "-XX:AOTConfiguration=build/app.aotconf"))).isFalse();
	}

	@Test
	void isAotRecordingEnabledWhenNoRecordingFlagIsPresent() {
		assertThat(AotCacheTestExecutionListener.isAotRecordingEnabled(
				List.of("-Xmx512m", "-jar", "app.jar"))).isFalse();
	}

	@Test
	void isAotRecordingEnabledWhenAotModeCreateFlagIsPresent() {
		// -XX:AOTMode=create alone does not record a training run
		assertThat(AotCacheTestExecutionListener.isAotRecordingEnabled(
				List.of("-XX:AOTMode=create", "-XX:AOTCache=build/app.aot"))).isFalse();
	}

	@Test
	void findAotCacheOutputWhenFlagIsPresent() {
		assertThat(AotCacheTestExecutionListener.findAotCacheOutput(
				List.of("-Xmx512m", "-XX:AOTCacheOutput=build/app.aot"))).isEqualTo("build/app.aot");
	}

	@Test
	void findAotCacheOutputWhenFlagIsAbsent() {
		assertThat(AotCacheTestExecutionListener.findAotCacheOutput(List.of("-Xmx512m"))).isNull();
	}

	@Test
	void verifyCacheOutputWhenFileExists(@TempDir Path tempDir) throws Exception {
		Path cacheFile = tempDir.resolve("app.aot");
		Files.writeString(cacheFile, "test");
		assertThat(listener.verifyCacheOutput(cacheFile.toString())).isTrue();
	}

	@Test
	void verifyCacheOutputWhenFileDoesNotExist() {
		assertThat(listener.verifyCacheOutput("does-not-exist.aot")).isFalse();
	}

	private AotCacheTestExecutionListener listenerWithArgs(String... args) {
		List<String> inputArgs = List.of(args);
		return new AotCacheTestExecutionListener() {
			@Override
			protected List<String> getInputArguments() {
				return inputArgs;
			}
		};
	}

}
