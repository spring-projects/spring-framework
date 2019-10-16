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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.Arrays;

import io.rsocket.frame.FrameType;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RSocketFrameTypeMessageCondition}.
 * @author Rossen Stoyanchev
 */
public class RSocketFrameTypeMessageConditionTests {

	@Test
	public void getMatchingCondition() {
		Message<?> message = message(FrameType.REQUEST_RESPONSE);
		RSocketFrameTypeMessageCondition condition = condition(FrameType.REQUEST_FNF, FrameType.REQUEST_RESPONSE);
		RSocketFrameTypeMessageCondition actual = condition.getMatchingCondition(message);

		assertThat(actual).isNotNull();
		assertThat(actual.getFrameTypes()).hasSize(1).containsOnly(FrameType.REQUEST_RESPONSE);
	}

	@Test
	public void compareTo() {
		Message<byte[]> message = message(null);
		assertThat(condition(FrameType.SETUP).compareTo(condition(FrameType.SETUP), message)).isEqualTo(0);
		assertThat(condition(FrameType.SETUP).compareTo(condition(FrameType.METADATA_PUSH), message)).isEqualTo(0);
	}

	private Message<byte[]> message(@Nullable FrameType frameType) {
		MessageBuilder<byte[]> builder = MessageBuilder.withPayload(new byte[0]);
		if (frameType != null) {
			builder.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, frameType);
		}
		return builder.build();
	}

	private RSocketFrameTypeMessageCondition condition(FrameType... frameType) {
		return new RSocketFrameTypeMessageCondition(Arrays.asList(frameType));
	}

}
