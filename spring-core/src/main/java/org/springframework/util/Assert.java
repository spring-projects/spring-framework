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

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Assertion utility class that assists in validating arguments.
 *
 * <p>Useful for identifying programmer errors early and clearly at runtime.
 *
 * <p>For example, if the contract of a public method states it does not
 * allow {@code null} arguments, {@code Assert} can be used to validate that
 * contract. Doing this clearly indicates a contract violation when it
 * occurs and protects the class's invariants.
 *
 * <p>Typically used to validate method arguments rather than configuration
 * properties, to check for cases that are usually programmer errors rather
 * than configuration errors. In contrast to configuration initialization
 * code, there is usually no point in falling back to defaults in such methods.
 *
 * <p>This class is similar to JUnit's assertion library. If an argument value is
 * deemed invalid, an {@link IllegalArgumentException} is thrown (typically).
 * For example:
 *
 * <pre class="code">
 * Assert.notNull(clazz, "The class must not be null");
 * Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
 *
 * <p>Mainly for internal use within the framework; for a more comprehensive suite
 * of assertion utilities consider {@code org.apache.commons.lang3.Validate} from
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache Commons Lang</a>,
 * Google Guava's
 * <a href="https://github.com/google/guava/wiki/PreconditionsExplained">Preconditions</a>,
 * or similar third-party libraries.
 *
 * <p>辅助验证参数的断言工具类。
 * <p>有助于在运行时尽早明确地识别程序员错误。
 * <p>例如，如果公共方法的契约声明它不允许{@code null}参数，{@code Assert}可用于验证合同。
 * 这样做显然表明违反了合同发生并保护类的不变量。
 * <p>通常用于验证方法参数而不是配置属性，检查通常是程序员错误的情况，而不是而不是配置错误。
 * 与配置初始化相反在代码中，通常没有必要在这些方法中回到默认值。
 * <p>这个类类似于JUnit的断言库。如果参数值为当被视为无效时，会抛出{@link IllegalArgumentException}(通常)。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Colin Sampaleanu
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class Assert {

	/**
	 * Assert a boolean expression, throwing an {@code IllegalStateException}
	 * if the expression evaluates to {@code false}.
	 * <p>Call {@link #isTrue} if you wish to throw an {@code IllegalArgumentException}
	 * on an assertion failure.
	 * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
	 *
	 * @param expression a boolean expression
	 * @param message    the exception message to use if the assertion fails
	 * @throws IllegalStateException if {@code expression} is {@code false}
	 */
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalStateException}
	 * if the expression evaluates to {@code false}.
	 * <p>Call {@link #isTrue} if you wish to throw an {@code IllegalArgumentException}
	 * on an assertion failure.
	 * <pre class="code">
	 * Assert.state(entity.getId() == null,
	 *     () -&gt; "ID for entity " + entity.getName() + " must not already be initialized");
	 * </pre>
	 *
	 * @param expression      a boolean expression
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalStateException if {@code expression} is {@code false}
	 * @since 5.0
	 */
	public static void state(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalStateException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalStateException}
	 * if the expression evaluates to {@code false}.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #state(boolean, String)}
	 */
	@Deprecated
	public static void state(boolean expression) {
		state(expression, "[Assertion failed] - this state invariant must be true");
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalArgumentException}
	 * if the expression evaluates to {@code false}.
	 * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
	 *
	 * @param expression a boolean expression
	 * @param message    the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if {@code expression} is {@code false}
	 */
	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalArgumentException}
	 * if the expression evaluates to {@code false}.
	 * <pre class="code">
	 * Assert.isTrue(i &gt; 0, () -&gt; "The value '" + i + "' must be greater than zero");
	 * </pre>
	 *
	 * @param expression      a boolean expression
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if {@code expression} is {@code false}
	 * @since 5.0
	 */
	public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalArgumentException}
	 * if the expression evaluates to {@code false}.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #isTrue(boolean, String)}
	 */
	@Deprecated
	public static void isTrue(boolean expression) {
		isTrue(expression, "[Assertion failed] - this expression must be true");
	}

	/**
	 * Assert that an object is {@code null}.
	 * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
	 *
	 * @param object  the object to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the object is not {@code null}
	 */
	public static void isNull(@Nullable Object object, String message) {
		if (object != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that an object is {@code null}.
	 * <pre class="code">
	 * Assert.isNull(value, () -&gt; "The value '" + value + "' must be null");
	 * </pre>
	 *
	 * @param object          the object to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the object is not {@code null}
	 * @since 5.0
	 */
	public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object != null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that an object is {@code null}.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #isNull(Object, String)}
	 */
	@Deprecated
	public static void isNull(@Nullable Object object) {
		isNull(object, "[Assertion failed] - the object argument must be null");
	}

	/**
	 * Assert that an object is not {@code null}.
	 * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
	 *
	 * @param object  the object to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the object is {@code null}
	 */
	public static void notNull(@Nullable Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that an object is not {@code null}.
	 * <pre class="code">
	 * Assert.notNull(entity.getId(),
	 *     () -&gt; "ID for entity " + entity.getName() + " must not be null");
	 * </pre>
	 *
	 * @param object          the object to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the object is {@code null}
	 * @since 5.0
	 */
	public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object == null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that an object is not {@code null}.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #notNull(Object, String)}
	 */
	@Deprecated
	public static void notNull(@Nullable Object object) {
		notNull(object, "[Assertion failed] - this argument is required; it must not be null");
	}

	/**
	 * Assert that the given String is not empty; that is,
	 * it must not be {@code null} and not the empty String.
	 * <pre class="code">Assert.hasLength(name, "Name must not be empty");</pre>
	 *
	 * @param text    the String to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the text is empty
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that the given String is not empty; that is,
	 * it must not be {@code null} and not the empty String.
	 * <pre class="code">
	 * Assert.hasLength(account.getName(),
	 *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
	 * </pre>
	 *
	 * @param text            the String to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the text is empty
	 * @see StringUtils#hasLength
	 * @since 5.0
	 */
	public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that the given String is not empty; that is,
	 * it must not be {@code null} and not the empty String.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #hasLength(String, String)}
	 */
	@Deprecated
	public static void hasLength(@Nullable String text) {
		hasLength(text,
				"[Assertion failed] - this String argument must have length; it must not be null or empty");
	}

	/**
	 * Assert that the given String contains valid text content; that is, it must not
	 * be {@code null} and must contain at least one non-whitespace character.
	 * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
	 *
	 * @param text    the String to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the text does not contain valid text content
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that the given String contains valid text content; that is, it must not
	 * be {@code null} and must contain at least one non-whitespace character.
	 * <pre class="code">
	 * Assert.hasText(account.getName(),
	 *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
	 * </pre>
	 *
	 * @param text            the String to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the text does not contain valid text content
	 * @see StringUtils#hasText
	 * @since 5.0
	 */
	public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that the given String contains valid text content; that is, it must not
	 * be {@code null} and must contain at least one non-whitespace character.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #hasText(String, String)}
	 */
	@Deprecated
	public static void hasText(@Nullable String text) {
		hasText(text,
				"[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * <pre class="code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
	 *
	 * @param textToSearch the text to search
	 * @param substring    the substring to find within the text
	 * @param message      the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the text contains the substring
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * <pre class="code">
	 * Assert.doesNotContain(name, forbidden, () -&gt; "Name must not contain '" + forbidden + "'");
	 * </pre>
	 *
	 * @param textToSearch    the text to search
	 * @param substring       the substring to find within the text
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the text contains the substring
	 * @since 5.0
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #doesNotContain(String, String, String)}
	 */
	@Deprecated
	public static void doesNotContain(@Nullable String textToSearch, String substring) {
		doesNotContain(textToSearch, substring,
				() -> "[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
	}

	/**
	 * Assert that an array contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">Assert.notEmpty(array, "The array must contain elements");</pre>
	 *
	 * @param array   the array to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the object array is {@code null} or contains no elements
	 */
	public static void notEmpty(@Nullable Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that an array contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">
	 * Assert.notEmpty(array, () -&gt; "The " + arrayType + " array must contain elements");
	 * </pre>
	 *
	 * @param array           the array to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the object array is {@code null} or contains no elements
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that an array contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Object[], String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Object[] array) {
		notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
	}

	/**
	 * Assert that an array contains no {@code null} elements.
	 * <p>Note: Does not complain if the array is empty!
	 * <pre class="code">Assert.noNullElements(array, "The array must contain non-null elements");</pre>
	 *
	 * @param array   the array to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the object array contains a {@code null} element
	 */
	public static void noNullElements(@Nullable Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * Assert that an array contains no {@code null} elements.
	 * <p>Note: Does not complain if the array is empty!
	 * <pre class="code">
	 * Assert.noNullElements(array, () -&gt; "The " + arrayType + " array must contain non-null elements");
	 * </pre>
	 *
	 * @param array           the array to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the object array contains a {@code null} element
	 * @since 5.0
	 */
	public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * Assert that an array contains no {@code null} elements.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #noNullElements(Object[], String)}
	 */
	@Deprecated
	public static void noNullElements(@Nullable Object[] array) {
		noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
	}

	/**
	 * Assert that a collection contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">Assert.notEmpty(collection, "Collection must contain elements");</pre>
	 *
	 * @param collection the collection to check
	 * @param message    the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the collection is {@code null} or
	 *                                  contains no elements
	 */
	public static void notEmpty(@Nullable Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that a collection contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">
	 * Assert.notEmpty(collection, () -&gt; "The " + collectionType + " collection must contain elements");
	 * </pre>
	 *
	 * @param collection      the collection to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the collection is {@code null} or
	 *                                  contains no elements
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that a collection contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Collection, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Collection<?> collection) {
		notEmpty(collection,
				"[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
	}

	/**
	 * Assert that a collection contains no {@code null} elements.
	 * <p>Note: Does not complain if the collection is empty!
	 * <pre class="code">Assert.noNullElements(collection, "Collection must contain non-null elements");</pre>
	 *
	 * @param collection the collection to check
	 * @param message    the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the collection contains a {@code null} element
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, String message) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * Assert that a collection contains no {@code null} elements.
	 * <p>Note: Does not complain if the collection is empty!
	 * <pre class="code">
	 * Assert.noNullElements(collection, () -&gt; "Collection " + collectionName + " must contain non-null elements");
	 * </pre>
	 *
	 * @param collection      the collection to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the collection contains a {@code null} element
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * Assert that a Map contains entries; that is, it must not be {@code null}
	 * and must contain at least one entry.
	 * <pre class="code">Assert.notEmpty(map, "Map must contain entries");</pre>
	 *
	 * @param map     the map to check
	 * @param message the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the map is {@code null} or contains no entries
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Assert that a Map contains entries; that is, it must not be {@code null}
	 * and must contain at least one entry.
	 * <pre class="code">
	 * Assert.notEmpty(map, () -&gt; "The " + mapType + " map must contain entries");
	 * </pre>
	 *
	 * @param map             the map to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails
	 * @throws IllegalArgumentException if the map is {@code null} or contains no entries
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that a Map contains entries; that is, it must not be {@code null}
	 * and must contain at least one entry.
	 *
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Map, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Map<?, ?> map) {
		notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
	}

	/**
	 * Assert that the provided object is an instance of the provided class.
	 * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo expected");</pre>
	 *
	 * @param type    the type to check against
	 * @param obj     the object to check
	 * @param message a message which will be prepended to provide further context.
	 *                If it is empty or ends in ":" or ";" or "," or ".", a full exception message
	 *                will be appended. If it ends in a space, the name of the offending object's
	 *                type will be appended. In any other case, a ":" with a space and the name
	 *                of the offending object's type will be appended.
	 * @throws IllegalArgumentException if the object is not an instance of type
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, message);
		}
	}

	/**
	 * Assert that the provided object is an instance of the provided class.
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo, () -&gt; "Processing " + Foo.class.getSimpleName() + ":");
	 * </pre>
	 *
	 * @param type            the type to check against
	 * @param obj             the object to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails. See {@link #isInstanceOf(Class, Object, String)} for details.
	 * @throws IllegalArgumentException if the object is not an instance of type
	 * @since 5.0
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that the provided object is an instance of the provided class.
	 * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
	 *
	 * @param type the type to check against
	 * @param obj  the object to check
	 * @throws IllegalArgumentException if the object is not an instance of type
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj) {
		isInstanceOf(type, obj, "");
	}

	/**
	 * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
	 * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number expected");</pre>
	 *
	 * @param superType the supertype to check against
	 * @param subType   the subtype to check
	 * @param message   a message which will be prepended to provide further context.
	 *                  If it is empty or ends in ":" or ";" or "," or ".", a full exception message
	 *                  will be appended. If it ends in a space, the name of the offending subtype
	 *                  will be appended. In any other case, a ":" with a space and the name of the
	 *                  offending subtype will be appended.
	 * @throws IllegalArgumentException if the classes are not assignable
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
		notNull(superType, "Supertype to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, message);
		}
	}

	/**
	 * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass, () -&gt; "Processing " + myAttributeName + ":");
	 * </pre>
	 *
	 * @param superType       the supertype to check against
	 * @param subType         the subtype to check
	 * @param messageSupplier a supplier for the exception message to use if the
	 *                        assertion fails. See {@link #isAssignable(Class, Class, String)} for details.
	 * @throws IllegalArgumentException if the classes are not assignable
	 * @since 5.0
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
		notNull(superType, "Supertype to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
	 * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
	 *
	 * @param superType the supertype to check
	 * @param subType   the subtype to check
	 * @throws IllegalArgumentException if the classes are not assignable
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, "");
	}


	private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
		String className = (obj != null ? obj.getClass().getName() : "null");
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			} else {
				result = messageWithTypeName(msg, className);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + ("Object of class [" + className + "] must be an instance of " + type);
		}
		throw new IllegalArgumentException(result);
	}

	private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			} else {
				result = messageWithTypeName(msg, subType);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + (subType + " is not assignable to " + superType);
		}
		throw new IllegalArgumentException(result);
	}

	private static boolean endsWithSeparator(String msg) {
		return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
	}

	private static String messageWithTypeName(String msg, @Nullable Object typeName) {
		return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
	}

	@Nullable
	private static String nullSafeGet(@Nullable Supplier<String> messageSupplier) {
		return (messageSupplier != null ? messageSupplier.get() : null);
	}

}
