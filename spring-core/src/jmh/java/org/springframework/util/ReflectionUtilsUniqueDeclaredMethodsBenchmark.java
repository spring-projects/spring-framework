/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import java.lang.reflect.Method;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

/**
 * Benchmark for finding declared methods on a class using {@link ReflectionUtils}.
 * This benchmark is using {@link Mode#SingleShotTime} since we want to benchmark
 * cold JVM iterations.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.SingleShotTime)
public class ReflectionUtilsUniqueDeclaredMethodsBenchmark {

	@Benchmark
	public Method[] findMethods() {
		return ReflectionUtils.getUniqueDeclaredMethods(C.class);
	}

	@SuppressWarnings("unused")
	class C {
		void m00() { } void m01() { } void m02() { } void m03() { } void m04() { }
		void m05() { } void m06() { } void m07() { } void m08() { } void m09() { }
		void m10() { } void m11() { } void m12() { } void m13() { } void m14() { }
		void m15() { } void m16() { } void m17() { } void m18() { } void m19() { }
		void m20() { } void m21() { } void m22() { } void m23() { } void m24() { }
		void m25() { } void m26() { } void m27() { } void m28() { } void m29() { }
		void m30() { } void m31() { } void m32() { } void m33() { } void m34() { }
		void m35() { } void m36() { } void m37() { } void m38() { } void m39() { }
		void m40() { } void m41() { } void m42() { } void m43() { } void m44() { }
		void m45() { } void m46() { } void m47() { } void m48() { } void m49() { }
		void m50() { } void m51() { } void m52() { } void m53() { } void m54() { }
		void m55() { } void m56() { } void m57() { } void m58() { } void m59() { }
		void m60() { } void m61() { } void m62() { } void m63() { } void m64() { }
		void m65() { } void m66() { } void m67() { } void m68() { } void m69() { }
		void m70() { } void m71() { } void m72() { } void m73() { } void m74() { }
		void m75() { } void m76() { } void m77() { } void m78() { } void m79() { }
		void m80() { } void m81() { } void m82() { } void m83() { } void m84() { }
		void m85() { } void m86() { } void m87() { } void m88() { } void m89() { }
		void m90() { } void m91() { } void m92() { } void m93() { } void m94() { }
		void m95() { } void m96() { } void m97() { } void m98() { } void m99() { }
	}

}
