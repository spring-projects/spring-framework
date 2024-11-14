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

package org.springframework.expression.spel;

/**
 * Captures the possible configuration settings for a compiler that can be used
 * when evaluating expressions.
 *
 * <p>Spring provides a basic compiler for SpEL expressions. Expressions are usually
 * interpreted, which provides a lot of dynamic flexibility during evaluation but does
 * not provide optimum performance. For occasional expression usage, this is fine, but
 * when used by other components such as Spring Integration, performance can be very
 * important, and there is no real need for the dynamism.
 *
 * <p>The SpEL compiler is intended to address this need. During evaluation, the compiler
 * generates a Java class that embodies the expression behavior at runtime and uses that
 * class to achieve much faster expression evaluation. Due to the lack of typing around
 * expressions, the compiler uses information gathered during the interpreted evaluations
 * of an expression when performing compilation. For example, SpEL does not know the type
 * of a property reference purely from the expression, but during the first interpreted
 * evaluation, it finds out what it is. Of course, basing compilation on such derived
 * information can cause trouble later if the types of the various expression elements
 * change over time. For this reason, compilation is best suited to expressions whose
 * type information is not going to change on repeated evaluations.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 4.1
 */
public enum SpelCompilerMode {

	/**
	 * The compiler is turned off, and all expressions will be evaluated in
	 * <em>interpreted</em> mode.
	 * <p>This is the default.
	 */
	OFF,

	/**
	 * In immediate mode, expressions are compiled as soon as possible, typically
	 * after the first interpreted evaluation.
	 * <p>If evaluation of the compiled expression fails (for example, due to a type
	 * changing), the caller of the expression evaluation receives an exception.
	 * <p>If the types of various expression elements change over time, consider
	 * switching to {@link #MIXED} mode or turning {@linkplain #OFF off} the compiler.
	 */
	IMMEDIATE,

	/**
	 * In mixed mode, expression evaluation silently switches between <em>interpreted</em>
	 * and <em>compiled</em> over time.
	 * <p>After some number of successful interpreted runs, the expression gets
	 * compiled. If evaluation of the compiled expression fails (for example, due
	 * to a type changing), that failure will be caught internally, and the system
	 * will switch back to interpreted mode for the given expression. Basically,
	 * the exception that the caller receives in {@link #IMMEDIATE} mode is instead
	 * handled internally. Sometime later, the compiler may generate another compiled form and switch to it.
	 * This cycle of switching between interpreted and compiled mode will continue
	 * until the system determines that it does not make sense to continue trying
	 * &mdash; for example, when a certain failure threshold has been reached &mdash;
	 * at which point the system will permanently switch to interpreted mode for the
	 * given expression.
	 */
	MIXED

}
