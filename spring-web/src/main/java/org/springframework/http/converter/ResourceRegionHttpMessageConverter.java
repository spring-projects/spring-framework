/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can write a single {@link ResourceRegion},
 * or Collections of {@link ResourceRegion ResourceRegions}.
 *
 * @author Brian Clozel
 * @since 4.3
 */
public class ResourceRegionHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	public ResourceRegionHttpMessageConverter() {
		super(MediaType.ALL);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called as we override canRead/canWrite
		return false;
	}

	@Override
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return false;
	}

	@Override
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return null;
	}

	@Override
	protected ResourceRegion readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return null;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return canWrite(clazz, null, mediaType);
	}

	@Override
	public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
		if (!(type instanceof ParameterizedType)) {
			return ResourceRegion.class.isAssignableFrom((Class) type);
		}
		ParameterizedType parameterizedType = (ParameterizedType) type;
		if (!(parameterizedType.getRawType() instanceof Class)) {
			return false;
		}
		Class<?> rawType = (Class<?>) parameterizedType.getRawType();
		if (!(Collection.class.isAssignableFrom(rawType))) {
			return false;
		}
		if (parameterizedType.getActualTypeArguments().length != 1) {
			return false;
		}
		Type typeArgument = parameterizedType.getActualTypeArguments()[0];
		if (!(typeArgument instanceof Class)) {
			return false;
		}
		Class<?> typeArgumentClass = (Class<?>) typeArgument;
		return typeArgumentClass.isAssignableFrom(ResourceRegion.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		if (object instanceof ResourceRegion) {
			writeResourceRegion((ResourceRegion) object, outputMessage);
		}
		else {
			Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
			if (regions.size() == 1) {
				writeResourceRegion(regions.iterator().next(), outputMessage);
			}
			else {
				writeResourceRegionCollection((Collection<ResourceRegion>) object, outputMessage);
			}
		}
	}

	protected void writeResourceRegion(ResourceRegion region, HttpOutputMessage outputMessage) throws IOException {
		Assert.notNull(region, "ResourceRegion must not be null");
		HttpHeaders responseHeaders = outputMessage.getHeaders();
		long start = region.getPosition();
		long end = start + region.getCount() - 1;
		Long resourceLength = region.getResource().contentLength();
		end = Math.min(end, resourceLength - 1);
		long rangeLength = end - start + 1;
		responseHeaders.add("Content-Range", "bytes " + start + '-' + end + '/' + resourceLength);
		responseHeaders.setContentLength(rangeLength);
		InputStream in = region.getResource().getInputStream();
		try {
			StreamUtils.copyRange(in, outputMessage.getBody(), start, end);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	private void writeResourceRegionCollection(Collection<ResourceRegion> resourceRegions,
			HttpOutputMessage outputMessage) throws IOException {

		Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
		HttpHeaders responseHeaders = outputMessage.getHeaders();
		MediaType contentType = responseHeaders.getContentType();
		String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
		responseHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
		OutputStream out = outputMessage.getBody();
		for (ResourceRegion region : resourceRegions) {
			long start = region.getPosition();
			long end = start + region.getCount() - 1;
			InputStream in = region.getResource().getInputStream();
			// Writing MIME header.
			println(out);
			print(out, "--" + boundaryString);
			println(out);
			if (contentType != null) {
				print(out, "Content-Type: " + contentType.toString());
				println(out);
			}
			Long resourceLength = region.getResource().contentLength();
			end = Math.min(end, resourceLength - 1);
			print(out, "Content-Range: bytes " + start + '-' + end + '/' + resourceLength);
			println(out);
			println(out);
			// Printing content
			StreamUtils.copyRange(in, out, start, end);
		}
		println(out);
		print(out, "--" + boundaryString + "--");
	}



	private static void println(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}

	private static void print(OutputStream os, String buf) throws IOException {
		os.write(buf.getBytes("US-ASCII"));
	}

}
