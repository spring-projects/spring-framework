/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.http.converter.xml;

import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using Spring's {@link Marshaller} and {@link Unmarshaller} abstractions.
 *
 * <p>This converter requires a {@code Marshaller} and {@code Unmarshaller} before it can be used.
 * These can be injected by the {@linkplain #MarshallingHttpMessageConverter(Marshaller) constructor}
 * or {@linkplain #setMarshaller(Marshaller) bean properties}.
 *
 * <p>By default, this converter supports {@code text/xml} and {@code application/xml}. This can be
 * overridden by setting the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class MarshallingHttpMessageConverter extends AbstractXmlHttpMessageConverter<Object> {

	private Marshaller[] marshallers;

	private Unmarshaller[] unmarshallers;

	/**
	 * Construct a new {@code MarshallingHttpMessageConverter} with no {@link Marshaller} or
	 * {@link Unmarshaller} set. The Marshaller and Unmarshaller must be set after construction
	 * by invoking {@link #setMarshaller(Marshaller)} and {@link #setUnmarshaller(Unmarshaller)} .
	 */
	public MarshallingHttpMessageConverter() {
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the given {@link Marshaller} set.
	 * <p>If the given {@link Marshaller} also implements the {@link Unmarshaller} interface,
	 * it is used for both marshalling and unmarshalling. Otherwise, an exception is thrown.
	 * <p>Note that all {@code Marshaller} implementations in Spring also implement the
	 * {@code Unmarshaller} interface, so that you can safely use this constructor.
	 * @param marshaller object used as marshaller and unmarshaller
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller) {
		setMarshaller(marshaller);
		if (marshaller instanceof Unmarshaller) {
			setUnmarshaller((Unmarshaller) marshaller);
		}
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the given {@link Unmarshaller}.
	 */
	public MarshallingHttpMessageConverter(Unmarshaller unmarshaller) {
		setUnmarshaller(unmarshaller);
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the given
	 * {@code Marshaller} and {@code Unmarshaller}.
	 * @param marshaller the Marshaller to use
	 * @param unmarshaller the Unmarshaller to use
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		setMarshaller(marshaller);
		setUnmarshaller(unmarshaller);
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the given {@code Marshaller}s and
	 * {@code Unmarshaller}s.
	 * @param marshallers the Marshallers to use
	 * @param unmarshallers the Unmarshallers to use
	 */
	public MarshallingHttpMessageConverter(Marshaller[] marshallers, Unmarshaller[] unmarshallers) {
		setMarshallers(marshallers);
		setUnmarshallers(unmarshallers);
	}

	/**
	 * Set the {@link Marshaller} to be used by this message converter.
	 */
	public void setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshallers = new Marshaller[] { marshaller };
	}

	/**
	 * Set {@link Marshaller}s to be used by this message converter.
	 */
	public void setMarshallers(Marshaller... marshallers) {
		Assert.notEmpty(marshallers, "Marshallers must not be empty");
		this.marshallers = marshallers;
	}

	/**
	 * Set the {@link Unmarshaller} to be used by this message converter.
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.unmarshallers = new Unmarshaller[] { unmarshaller };
	}

	/**
	 * Set {@link Unmarshaller}s to be used by this message converter.
	 */
	public void setUnmarshallers(Unmarshaller... unmarshallers) {
		Assert.notEmpty(unmarshallers, "Unmarshallers must not be empty");
		this.unmarshallers = unmarshallers;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (!canRead(mediaType)) {
			return false;
		}

		for (Marshaller marshaller : marshallers) {
			if (marshaller.supports(clazz)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		if (!canWrite(mediaType)) {
			return false;
		}

		for (Unmarshaller unmarshaller : unmarshallers) {
			if (unmarshaller.supports(clazz)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source)
			throws IOException {
		Assert.notEmpty(this.unmarshallers, "Property 'unmarshallers' is required");
		for (Unmarshaller unmarshaller : unmarshallers) {
			if (unmarshaller.supports(clazz)) {
				try {
					Object result = unmarshaller.unmarshal(source);

					if (!clazz.isInstance(result)) {
						throw new TypeMismatchException(result, clazz);
					}

					return result;
				}
				catch (MarshallingFailureException ex) {
					throw new HttpMessageNotReadableException("Could not write [" + clazz.getName()
							+ "]", ex);
				}
			}
		}

		throw new HttpMessageNotReadableException("No suitable unmarshaller found for class "
				+ clazz.getName());
	}

	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws IOException {
		Assert.notNull(this.marshallers, "Property 'marshallers' is required");
		for (Marshaller marshaller : marshallers) {
			if (marshaller.supports(o.getClass())) {
				try {
					marshaller.marshal(o, result);

					return;
				}
				catch (MarshallingFailureException ex) {
					throw new HttpMessageNotWritableException("Could not write ["
							+ o.getClass().getName() + "]", ex);
				}
			}
		}

		throw new HttpMessageNotWritableException("No suitable marshaller found for class "
				+ o.getClass().getName());
	}
}
