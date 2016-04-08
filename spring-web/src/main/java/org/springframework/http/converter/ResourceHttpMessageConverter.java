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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpRangeResource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write {@link Resource Resources}
 * and supports byte range requests.
 *
 * <p>By default, this converter can read all media types. The Java Activation Framework (JAF) -
 * if available - is used to determine the {@code Content-Type} of written resources.
 * If JAF is not available, {@code application/octet-stream} is used.
 *
 * <p>This converter supports HTTP byte range requests and can write partial content, when provided
 * with an {@link HttpRangeResource} instance containing the required Range information.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @author Brian Clozel
 * @since 3.0.2
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

	private static final boolean jafPresent = ClassUtils.isPresent(
			"javax.activation.FileTypeMap", ResourceHttpMessageConverter.class.getClassLoader());


	public ResourceHttpMessageConverter() {
		super(MediaType.ALL);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz);
	}

	@Override
	protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		if (InputStreamResource.class == clazz) {
			return new InputStreamResource(inputMessage.getBody());
		}
		else if (clazz.isAssignableFrom(ByteArrayResource.class)) {
			byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
			return new ByteArrayResource(body);
		}
		else {
			throw new IllegalStateException("Unsupported resource class: " + clazz);
		}
	}

	@Override
	protected MediaType getDefaultContentType(Resource resource) {
		if (jafPresent) {
			return ActivationMediaTypeFactory.getMediaType(resource);
		}
		else {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@Override
	protected Long getContentLength(Resource resource, MediaType contentType) throws IOException {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		if (InputStreamResource.class == resource.getClass()) {
			return null;
		}
		long contentLength = resource.contentLength();
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		outputMessage.getHeaders().add(HttpHeaders.ACCEPT_RANGES, "bytes");
		if (resource instanceof HttpRangeResource) {
			writePartialContent((HttpRangeResource) resource, outputMessage);
		}
		else {
			writeContent(resource, outputMessage);
		}
	}

	protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		try {
			InputStream in = resource.getInputStream();
			try {
				StreamUtils.copy(in, outputMessage.getBody());
			}
			catch (NullPointerException ex) {
				// ignore, see SPR-13620
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ex) {
					// ignore, see SPR-12999
				}
			}
		}
		catch (FileNotFoundException ex) {
			// ignore, see SPR-12999
		}
	}

	/**
	 * Write parts of the resource as indicated by the request {@code Range} header.
	 * @param resource the identified resource (never {@code null})
	 * @param outputMessage current servlet response
	 * @throws IOException in case of errors while writing the content
	 */
	protected void writePartialContent(HttpRangeResource resource, HttpOutputMessage outputMessage) throws IOException {
		Assert.notNull(resource, "Resource should not be null");
		List<HttpRange> ranges = resource.getHttpRanges();
		HttpHeaders responseHeaders = outputMessage.getHeaders();
		MediaType contentType = responseHeaders.getContentType();
		Long length = getContentLength(resource, contentType);

		if (ranges.size() == 1) {
			HttpRange range = ranges.get(0);
			long start = range.getRangeStart(length);
			long end = range.getRangeEnd(length);
			long rangeLength = end - start + 1;
			responseHeaders.add("Content-Range", "bytes " + start + "-" + end + "/" + length);
			responseHeaders.setContentLength(rangeLength);
			InputStream in = resource.getInputStream();
			try {
				copyRange(in, outputMessage.getBody(), start, end);
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
		else {
			String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
			responseHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
			OutputStream out = outputMessage.getBody();
			for (HttpRange range : ranges) {
				long start = range.getRangeStart(length);
				long end = range.getRangeEnd(length);
				InputStream in = resource.getInputStream();
				// Writing MIME header.
				println(out);
				print(out, "--" + boundaryString);
				println(out);
				if (contentType != null) {
					print(out, "Content-Type: " + contentType.toString());
					println(out);
				}
				print(out, "Content-Range: bytes " + start + "-" + end + "/" + length);
				println(out);
				println(out);
				// Printing content
				copyRange(in, out, start, end);
			}
			println(out);
			print(out, "--" + boundaryString + "--");
		}
	}

	private static void println(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}

	private static void print(OutputStream os, String buf) throws IOException {
		os.write(buf.getBytes("US-ASCII"));
	}

	private void copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
		long skipped = in.skip(start);
		if (skipped < start) {
			throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required.");
		}

		long bytesToCopy = end - start + 1;
		byte buffer[] = new byte[StreamUtils.BUFFER_SIZE];
		while (bytesToCopy > 0) {
			int bytesRead = in.read(buffer);
			if (bytesRead <= bytesToCopy) {
				out.write(buffer, 0, bytesRead);
				bytesToCopy -= bytesRead;
			}
			else {
				out.write(buffer, 0, (int) bytesToCopy);
				bytesToCopy = 0;
			}
			if (bytesRead == -1) {
				break;
			}
		}
	}


	/**
	 * Inner class to avoid a hard-coded JAF dependency.
	 */
	private static class ActivationMediaTypeFactory {

		private static final FileTypeMap fileTypeMap;

		static {
			fileTypeMap = loadFileTypeMapFromContextSupportModule();
		}

		private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
			// See if we can find the extended mime.types from the context-support module...
			Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
			if (mappingLocation.exists()) {
				InputStream inputStream = null;
				try {
					inputStream = mappingLocation.getInputStream();
					return new MimetypesFileTypeMap(inputStream);
				}
				catch (IOException ex) {
					// ignore
				}
				finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						}
						catch (IOException ex) {
							// ignore
						}
					}
				}
			}
			return FileTypeMap.getDefaultFileTypeMap();
		}

		public static MediaType getMediaType(Resource resource) {
			String filename = resource.getFilename();
			if (filename != null) {
				String mediaType = fileTypeMap.getContentType(filename);
				if (StringUtils.hasText(mediaType)) {
					return MediaType.parseMediaType(mediaType);
				}
			}
			return null;
		}
	}

}
