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

package org.springframework.http.support;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.eclipse.jetty.http.HttpFields;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * Benchmark for implementations of MultiValueMap adapters over native HTTP
 * headers implementations.
 * <p>Run JMH with {@code -p implementation=Netty,Netty5,HttpComponents,Jetty}
 * to cover all implementations
 * @author Simon Baslé
 */
@BenchmarkMode(Mode.Throughput)
public class HeadersAdapterBenchmark {

	@Benchmark
	public void iterateEntries(BenchmarkData data, Blackhole bh) {
		for (Map.Entry<String, List<String>> entry : data.entriesProvider.apply(data.headers)) {
			bh.consume(entry.getKey());
			for (String s : entry.getValue()) {
				bh.consume(s);
			}
		}
	}

	@Benchmark
	public void toString(BenchmarkData data, Blackhole bh) {
		bh.consume(data.headers.toString());
	}

	@State(Scope.Benchmark)
	public static class BenchmarkData {

		@Param({"NONE"})
		public String implementation;

		@Param({"true"})
		public boolean duplicate;

		public MultiValueMap<String, String> headers;
		public Function<MultiValueMap<String, String>, Set<Map.Entry<String, List<String>>>> entriesProvider;

		//Uncomment the following line and comment the similar line for setupImplementationBaseline below
		//to benchmark current implementations
		@Setup(Level.Trial)
		public void initImplementationNew() {
			this.entriesProvider = map -> new HttpHeaders(map).headerSet();

			this.headers = switch (this.implementation) {
				case "Netty" -> new Netty4HeadersAdapter(new DefaultHttpHeaders());
				case "HttpComponents" -> new HttpComponentsHeadersAdapter(new HttpGet("https://example.com"));
				case "Netty5" -> new Netty5HeadersAdapter(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders());
				case "Jetty" -> new JettyHeadersAdapter(HttpFields.build());
				//FIXME tomcat/undertow implementations (in another package)
//				case "Tomcat" -> new TomcatHeadersAdapter(new MimeHeaders());
//				case "Undertow" -> new UndertowHeadersAdapter(new HeaderMap());
				default -> throw new IllegalArgumentException("Unsupported implementation: " + this.implementation);
			};
			initHeaders();
		}

		//Uncomment the following line and comment the similar line for setupImplementationNew above
		//to benchmark old implementations
//		@Setup(Level.Trial)
		public void setupImplementationBaseline() {
			this.entriesProvider = MultiValueMap::entrySet;

			this.headers = switch (this.implementation) {
				case "Netty" -> new HeadersAdaptersBaseline.Netty4(new DefaultHttpHeaders());
				case "HttpComponents" -> new HeadersAdaptersBaseline.HttpComponents(new HttpGet("https://example.com"));
				case "Netty5" -> new HeadersAdaptersBaseline.Netty5(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders());
				case "Jetty" -> new HeadersAdaptersBaseline.Jetty(HttpFields.build());
				default -> throw new IllegalArgumentException("Unsupported implementation: " + this.implementation);
			};
			initHeaders();
		}

		private void initHeaders() {
			this.headers.add("TestHeader", "first");
			this.headers.add("SecondHeader", "value");
			if (this.duplicate) {
				this.headers.add("TestHEADER", "second");
			}
			else {
				this.headers.add("TestHeader", "second");
			}
			this.headers.add("TestHeader", "third");
		}
	}
}
