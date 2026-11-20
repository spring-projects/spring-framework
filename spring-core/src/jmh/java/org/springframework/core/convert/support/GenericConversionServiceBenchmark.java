/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.CollectionUtils;

/**
 * Benchmarks for {@link GenericConversionService}.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class GenericConversionServiceBenchmark {

	@Benchmark
	public void convertListOfStringToListOfIntegerWithConversionService(ListBenchmarkState state, Blackhole bh) {
		TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(state.source);
		bh.consume(state.conversionService.convert(state.source, sourceTypeDesc, state.targetTypeDesc));
	}

	@Benchmark
	public void convertListOfStringToListOfIntegerBaseline(ListBenchmarkState state, Blackhole bh) {
		List<Integer> target = new ArrayList<>(state.source.size());
		for (String element : state.source) {
			target.add(Integer.valueOf(element));
		}
		bh.consume(target);
	}

	@State(Scope.Benchmark)
	public static class ListBenchmarkState extends BenchmarkState {

		List<String> source;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			this.source = IntStream.rangeClosed(1, collectionSize).mapToObj(String::valueOf).toList();
			List<Integer> target = new ArrayList<>();
			this.targetTypeDesc = TypeDescriptor.forObject(target);
		}
	}

	@Benchmark
	public void convertMapOfStringToListOfIntegerWithConversionService(MapBenchmarkState state, Blackhole bh) {
		TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(state.source);
		bh.consume(state.conversionService.convert(state.source, sourceTypeDesc, state.targetTypeDesc));
	}

	@Benchmark
	public void convertMapOfStringToListOfIntegerBaseline(MapBenchmarkState state, Blackhole bh) {
		Map<String, Integer> target = CollectionUtils.newHashMap(state.source.size());
		state.source.forEach((k, v) -> target.put(k, Integer.valueOf(v)));
		bh.consume(target);
	}


	@State(Scope.Benchmark)
	public static class MapBenchmarkState extends BenchmarkState {

		Map<String, String> source;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			this.source = CollectionUtils.newHashMap(this.collectionSize);
			Map<String, Integer> target = new HashMap<>();
			this.targetTypeDesc = TypeDescriptor.forObject(target);
			this.source = IntStream.rangeClosed(1, collectionSize).mapToObj(String::valueOf)
					.collect(Collectors.toMap(String::valueOf, String::valueOf));
		}
	}


	@State(Scope.Benchmark)
	public static class BenchmarkState {

		GenericConversionService conversionService = new GenericConversionService();

		@Param({"10"})
		int collectionSize;

		TypeDescriptor targetTypeDesc;
	}

}
