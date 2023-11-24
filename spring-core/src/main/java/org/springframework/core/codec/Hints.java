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

package org.springframework.core.codec;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Constants and convenience methods for working with hints.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see ResourceRegionEncoder#BOUNDARY_STRING_HINT
 */
public abstract class Hints {

	/**
	 * Name of hint exposing a prefix to use for correlating log messages.
	 */
	public static final String LOG_PREFIX_HINT = Log.class.getName() + ".PREFIX";

	/**
	 * Name of boolean hint whether to avoid logging data either because it's
	 * potentially sensitive, or because it has been logged by a composite
	 * encoder, e.g. for multipart requests.
	 */
	public static final String SUPPRESS_LOGGING_HINT = Log.class.getName() + ".SUPPRESS_LOGGING";


	/**
	 * Create a map wit a single hint via {@link Collections#singletonMap}.
	 * @param hintName the hint name
	 * @param value the hint value
	 * @return the created map
	 */
	public static Map<String, Object> from(String hintName, Object value) {
		return Collections.singletonMap(hintName, value);
	}

	/**
	 * Return an empty map of hints via {@link Collections#emptyMap()}.
	 * @return the empty map
	 */
	public static Map<String, Object> none() {
		return Collections.emptyMap();
	}

	/**
	 * Obtain the value for a required hint.
	 * @param hints the hints map
	 * @param hintName the required hint name
	 * @param <T> the hint type to cast to
	 * @return the hint value
	 * @throws IllegalArgumentException if the hint is not found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequiredHint(@Nullable Map<String, Object> hints, String hintName) {
		if (hints == null) {
			throw new IllegalArgumentException("No hints map for required hint '" + hintName + "'");
		}
		T hint = (T) hints.get(hintName);
		if (hint == null) {
			throw new IllegalArgumentException("Hints map must contain the hint '" + hintName + "'");
		}
		return hint;
	}

	/**
	 * Obtain the hint {@link #LOG_PREFIX_HINT}, if present, or an empty String.
	 * @param hints the hints passed to the encode method
	 * @return the log prefix
	 */
	public static String getLogPrefix(@Nullable Map<String, Object> hints) {
		return (hints != null ? (String) hints.getOrDefault(LOG_PREFIX_HINT, "") : "");
	}

	/**
	 * Whether to suppress logging based on the hint {@link #SUPPRESS_LOGGING_HINT}.
	 * @param hints the hints map
	 * @return whether logging of data is allowed
	 */
	public static boolean isLoggingSuppressed(@Nullable Map<String, Object> hints) {
		return (hints != null && (boolean) hints.getOrDefault(SUPPRESS_LOGGING_HINT, false));
	}

	/**
	 * Merge two maps of hints, creating and copying into a new map if both have
	 * values, or returning the non-empty map, or an empty map if both are empty.
	 * @param hints1 1st map of hints
	 * @param hints2 2nd map of hints
	 * @return a single map with hints from both
	 */
	public static Map<String, Object> merge(
			@Nullable Map<String, Object> hints1, @Nullable Map<String, Object> hints2) {

		if (ObjectUtils.isEmpty(hints1) && ObjectUtils.isEmpty(hints2)) {
			return Collections.emptyMap();
		}
		else if (ObjectUtils.isEmpty(hints2)) {
			return (hints1 != null ? hints1 : Collections.emptyMap());
		}
		else if (ObjectUtils.isEmpty(hints1)) {
			return hints2;
		}
		Map<String, Object> result = CollectionUtils.newHashMap(hints1.size() + hints2.size());
		result.putAll(hints1);
		result.putAll(hints2);
		return result;
	}

	/**
	 * Merge a single hint into a map of hints, possibly creating and copying
	 * all hints into a new map, or otherwise if the map of hints is empty,
	 * creating a new single entry map.
	 * @param hints a map of hints to be merged
	 * @param hintName the hint name to merge
	 * @param hintValue the hint value to merge
	 * @return a single map with all hints
	 */
	public static Map<String, Object> merge(@Nullable Map<String, Object> hints, String hintName, Object hintValue) {
		if (ObjectUtils.isEmpty(hints)) {
			return Collections.singletonMap(hintName, hintValue);
		}
		Map<String, Object> result = CollectionUtils.newHashMap(hints.size() + 1);
		result.putAll(hints);
		result.put(hintName, hintValue);
		return result;
	}

	/**
	 * If the hints contain a {@link #LOG_PREFIX_HINT} and the given logger has
	 * DEBUG level enabled, apply the log prefix as a hint to the given buffer
	 * via {@link DataBufferUtils#touch(DataBuffer, Object)}.
	 * @param buffer the buffer to touch
	 * @param hints the hints map to check for a log prefix
	 * @param logger the logger whose level to check
	 * @since 5.3.2
	 */
	public static void touchDataBuffer(DataBuffer buffer, @Nullable Map<String, Object> hints, Log logger) {
		if (logger.isDebugEnabled() && hints != null) {
			Object logPrefix = hints.get(LOG_PREFIX_HINT);
			if (logPrefix != null) {
				DataBufferUtils.touch(buffer, logPrefix);
			}
		}
	}

}
