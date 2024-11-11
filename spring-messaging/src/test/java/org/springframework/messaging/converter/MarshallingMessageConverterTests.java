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

package org.springframework.messaging.converter;

import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.diff.DifferenceEvaluator;

import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.diff.ComparisonType.XML_STANDALONE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;
import static org.xmlunit.diff.DifferenceEvaluators.chain;
import static org.xmlunit.diff.DifferenceEvaluators.downgradeDifferencesToEqual;

/**
 * @author Arjen Poutsma
 */
class MarshallingMessageConverterTests {

	private MarshallingMessageConverter converter;


	@BeforeEach
	void createMarshaller() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(MyBean.class);
		marshaller.afterPropertiesSet();

		this.converter = new MarshallingMessageConverter(marshaller);
	}


	@Test
	void fromMessage() {
		String payload = "<myBean><name>Foo</name></myBean>";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		MyBean actual = (MyBean) this.converter.fromMessage(message, MyBean.class);

		assertThat(actual).isNotNull();
		assertThat(actual.getName()).isEqualTo("Foo");
	}

	@Test
	void fromMessageInvalidXml() {
		String payload = "<myBean><name>Foo</name><myBean>";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.converter.fromMessage(message, MyBean.class));
	}

	@Test
	void fromMessageValidXmlWithUnknownProperty() {
		String payload = "<myBean><age>42</age><myBean>";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.converter.fromMessage(message, MyBean.class));
	}

	@Test
	void toMessage() {
		MyBean payload = new MyBean();
		payload.setName("Foo");

		Message<?> message = this.converter.toMessage(payload, null);
		assertThat(message).isNotNull();
		String actual = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);

		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.of(actual)).isSimilarTo("<myBean><name>Foo</name></myBean>", ev);
	}


	@XmlRootElement
	public static class MyBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
