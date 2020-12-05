/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;

/**
 * hints Metadata codec.
 * @author  Rudy Steiner
 * @since 5.3.2
 **/
public interface HintMetadataCodec {

	/**
	 *  Serialize hints to ByteBuf.
	 *
	 **/
	void encodeHints(Map<String, Object> hints, Consumer<ByteBuf> consumer);

	/**
	 * Deserialize  Hints  ByteBuf to result.
	 **/
	void decodeHints(ByteBuf hints, Map<String, Object> result);
}
