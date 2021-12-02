/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can write a single
 * {@link ResourceRegion} or Collections of {@link ResourceRegion ResourceRegions}.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.3
 */
public class ResourceRegionHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	public ResourceRegionHttpMessageConverter() {
		super(MediaType.ALL);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected MediaType getDefaultContentType(Object object) {
		Resource resource = null;
		if (object instanceof ResourceRegion resourceRegion) {
			resource = resourceRegion.getResource();
		}
		else {
			Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
			if (!regions.isEmpty()) {
				resource = regions.iterator().next().getResource();
			}
		}
		return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected ResourceRegion readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, null, mediaType);
	}

	@Override
	public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
		if (!(type instanceof ParameterizedType parameterizedType)) {
			return (type instanceof Class<?> c && ResourceRegion.class.isAssignableFrom(c));
		}
		if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
			return false;
		}
		if (!(Collection.class.isAssignableFrom(rawType))) {
			return false;
		}
		if (parameterizedType.getActualTypeArguments().length != 1) {
			return false;
		}
		Type typeArgument = parameterizedType.getActualTypeArguments()[0];
		if (!(typeArgument instanceof Class<?> typeArgumentClass)) {
			return false;
		}
		return ResourceRegion.class.isAssignableFrom(typeArgumentClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		if (object instanceof ResourceRegion resourceRegion) {
			writeResourceRegion(resourceRegion, outputMessage);
		}
		else {
			Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
			if (regions.size() == 1) {
				writeResourceRegion(regions.iterator().next(), outputMessage);
			}
			else {
				writeResourceRegionCollection(regions, outputMessage);
			}
		}
	}


	protected void writeResourceRegion(ResourceRegion region, HttpOutputMessage outputMessage) throws IOException {
		Assert.notNull(region, "ResourceRegion must not be null");
		HttpHeaders responseHeaders = outputMessage.getHeaders();

		long start = region.getPosition();
		long end = start + region.getCount() - 1;
		long resourceLength = region.getResource().contentLength();
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

		Resource resource = null;
		InputStream in = null;
		long inputStreamPosition = 0;

		try {
			for (ResourceRegion region : resourceRegions) {
				long start = region.getPosition() - inputStreamPosition;
				if (start < 0 || resource != region.getResource()) {
					if (in != null) {
						in.close();
					}
					resource = region.getResource();
					in = resource.getInputStream();
					inputStreamPosition = 0;
					start = region.getPosition();
				}
				long end = start + region.getCount() - 1;
				// Writing MIME header.
				println(out);
				print(out, "--" + boundaryString);
				println(out);
				if (contentType != null) {
					print(out, "Content-Type: " + contentType);
					println(out);
				}
				long resourceLength = region.getResource().contentLength();
				end = Math.min(end, resourceLength - inputStreamPosition - 1);
				print(out, "Content-Range: bytes " +
						region.getPosition() + '-' + (region.getPosition() + region.getCount() - 1) +
						'/' + resourceLength);
				println(out);
				println(out);
				// Printing content
				StreamUtils.copyRange(in, out, start, end);
				inputStreamPosition += (end + 1);
			}
		}
		finally {
			try {
				if (in != null) {
					in.close();
				}
			}
			catch (IOException ex) {
				// ignore
			}
		}

		println(out);
		print(out, "--" + boundaryString + "--");
	}

	private static void println(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}

	private static void print(OutputStream os, String buf) throws IOException {
		os.write(buf.getBytes(StandardCharsets.US_ASCII));
	}

}
