/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.reactive.codec.support;

import org.springframework.reactive.codec.decoder.ByteToMessageDecoder;
import org.springframework.reactive.codec.encoder.MessageToByteEncoder;

/**
 * Utility methods for dealing with codec hints.
 *
 * @author Sebastien Deleuze
 * @see Hints
 * @see MessageToByteEncoder
 * @see ByteToMessageDecoder
 */
public abstract class HintUtils {

	public static <T> T getHintByClass(Class<T> clazz, Object[] hints) {
		return getHintByClass(clazz, hints, null);
	}

	public static <T> T getHintByClass(Class<T> clazz, Object[] hints, T defaultValue) {
		for (Object hint : hints) {
			if (hint.getClass().isAssignableFrom(clazz)) {
				return (T)hint;
			}
		}
		return defaultValue;
	}

	public static boolean containsHint(Object hint, Object[] hints) {
		for (Object h : hints) {
			if (h.equals(hint)) {
				return true;
			}
		}
		return false;
	}

}
