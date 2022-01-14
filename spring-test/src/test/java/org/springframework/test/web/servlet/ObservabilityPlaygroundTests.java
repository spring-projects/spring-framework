/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet;

import java.time.Duration;
import java.util.function.BiConsumer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.TimerRecordingHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.tracing.context.HttpServerHandlerContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.SampleTestRunner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.observability.DefaultWebMvcTagsProvider;
import org.springframework.web.servlet.observability.WebMvcObservabilityFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Just a demo to try MVC instrumentation out with Zipkin, will be deleted later.
 */
public class ObservabilityPlaygroundTests extends SampleTestRunner {
	private MockMvc mockMvc;

	public ObservabilityPlaygroundTests() {
		super(SampleTestRunner.SamplerRunnerConfig.builder().build());
		this.mockMvc = standaloneSetup(new TestController())
				.addFilters(new WebMvcObservabilityFilter(meterRegistry, new DefaultWebMvcTagsProvider(), "http.server.rq", null))
				.build();
		this.meterRegistry.config().timerRecordingHandler(new TestTimerRecordingHandler());
	}

	@Override
	public BiConsumer<Tracer, MeterRegistry> yourCode() {
		return ((tracer, meterRegistry1) -> {
			try {
				mockMvc.perform(get("/"));
				mockMvc.perform(get("/api/people/12345"));
				mockMvc.perform(get("/oops"));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println(((SimpleMeterRegistry) meterRegistry).getMetersAsString());
		});
	}

	@Timed
	@RestController
	static class TestController {
		@GetMapping("/")
		String hello() {
			return "hello";
		}

		@GetMapping("/api/people/{id}")
		String personById(@PathVariable String id) {
			return id;
		}

		@GetMapping("/oops")
		ResponseEntity<String> oops() {
			return ResponseEntity.badRequest().body("oops");
		}
	}

	static class TestTimerRecordingHandler implements TimerRecordingHandler<HttpServerHandlerContext> {
		@Override
		public void onStart(Timer.Sample sample, HttpServerHandlerContext context) {
			System.out.println(sample + " started " + context);
		}

		@Override
		public void onError(Timer.Sample sample, HttpServerHandlerContext context, Throwable throwable) {
			System.out.println(sample + " failed " + context);
		}

		@Override
		public void onStop(Timer.Sample sample, HttpServerHandlerContext context, Timer timer, Duration duration) {
			System.out.println(sample + " stopped " + context);
		}

		@Override
		public boolean supportsContext(Timer.HandlerContext context) {
			return context instanceof HttpServerHandlerContext;
		}
	}

}
