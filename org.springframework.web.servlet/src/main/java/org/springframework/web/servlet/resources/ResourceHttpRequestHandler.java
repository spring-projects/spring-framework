package org.springframework.web.servlet.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPOutputStream;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * {@link HttpRequestHandler} that serves static resources optimized for superior browser performance 
 * (according to the guidelines of Page Speed, YSlow, etc.) by adding far future cache expiration headers 
 * and gzip compressing the resources if supported by the client.
 * 
 * <p>TODO - expand the docs further
 * 
 * @author Keith Donald
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler implements HttpRequestHandler, LastModified {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private final List<Resource> resourcePaths;
	
	private int maxAge = 31556926;
	
	private FileMediaTypeMap fileMediaTypeMap = new DefaultFileMediaTypeMap();

	private boolean gzipEnabled = true;
	
	private int minGzipSize = 150;
	
	private int maxGzipSize = 500000;
	
	public ResourceHttpRequestHandler(List<Resource> resourcePaths) {
		Assert.notNull(resourcePaths, "Resource paths must not be null");
		this.resourcePaths = resourcePaths;
	}
	
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		if (!"GET".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"GET"}, "ResourceHttpRequestHandler only supports GET requests");
		}
		URLResource resource = getResource(request);
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		prepareResponse(resource, response);
		writeResponse(resource, request, response);
	}
	
	public long getLastModified(HttpServletRequest request) {
		try {
			Resource resource = getResource(request);
			if (resource == null) {
				return -1;
			}
			return resource.lastModified();
		} catch (Exception e) {
			return -1;
		}
	}
	
	public void setGzipEnabled(boolean gzipEnabled) {
		this.gzipEnabled = gzipEnabled;
	}

	public void setMinGzipSize(int minGzipSize) {
		this.minGzipSize = minGzipSize;
	}

	public void setMaxGzipSize(int maxGzipSize) {
		this.maxGzipSize = maxGzipSize;
	}

	private URLResource getResource(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" + HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");			
		}
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return null;
		}
		for (Resource resourcePath : this.resourcePaths) {
			Resource resource;
			try {
				resource = resourcePath.createRelative(path);
				if (isValidFile(resource)) {
					return new URLResource(resource);
				}
			} catch (IOException e) {
				//Resource not found
				return null;
			}
		}
		return null;
	}
	
	private void prepareResponse(URLResource resource, HttpServletResponse response) throws IOException {
		MediaType mediaType = null;
		if (mediaType == null) {
			mediaType = fileMediaTypeMap.getMediaType(resource.getFilename());
		}		
		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
		response.setContentLength(resource.getContentLength());
		response.setDateHeader("Last-Modified", resource.lastModified());
		if (this.maxAge > 0) {
			// HTTP 1.0 header
			response.setDateHeader("Expires", System.currentTimeMillis() + this.maxAge * 1000L);
			// HTTP 1.1 header
			response.setHeader("Cache-Control", "max-age=" + this.maxAge);
		}
	}
	
	private void writeResponse(URLResource resource, HttpServletRequest request, HttpServletResponse response) throws IOException {
		OutputStream out = selectOutputStream(resource, request, response);
		try {
			InputStream in = resource.getInputStream();
			try {
				byte[] buffer = new byte[1024];
				int bytesRead = -1;
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	private OutputStream selectOutputStream(URLResource resource, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String acceptEncoding = request.getHeader("Accept-Encoding");
		boolean isGzipEligible = resource.getContentLength() >= this.minGzipSize && resource.getContentLength() <= this.maxGzipSize;
		if (this.gzipEnabled && isGzipEligible && StringUtils.hasText(acceptEncoding)
				&& acceptEncoding.indexOf("gzip") > -1 
				&& response.getContentType().startsWith("text/")){
			return new GZIPResponseStream(response);
		} else {
			return response.getOutputStream();
		}
	}
	
	private boolean isValidFile(Resource resource) throws IOException {
		return resource.exists() && StringUtils.hasText(resource.getFilename());
	}
		
	// TODO promote to top-level and make reusable
	// TODO check ServletContext.getMimeType(String) first
	
	public interface FileMediaTypeMap {
		MediaType getMediaType(String fileName);
	}
	
	public static class DefaultFileMediaTypeMap implements FileMediaTypeMap {

		private static final boolean jafPresent =
			ClassUtils.isPresent("javax.activation.FileTypeMap", ContentNegotiatingViewResolver.class.getClassLoader());

		private boolean useJaf = true;

		private ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>();

		public MediaType getMediaType(String filename) {
			String extension = StringUtils.getFilenameExtension(filename);
			if (!StringUtils.hasText(extension)) {
				return null;
			}
			extension = extension.toLowerCase(Locale.ENGLISH);
			MediaType mediaType = this.mediaTypes.get(extension);
			if (mediaType == null && useJaf && jafPresent) {
				mediaType = ActivationMediaTypeFactory.getMediaType(filename);
				if (mediaType != null) {
					this.mediaTypes.putIfAbsent(extension, mediaType);
				}
			}
			return mediaType;
		}
		
		/**
		 * Inner class to avoid hard-coded JAF dependency.
		 */
		private static class ActivationMediaTypeFactory {

			private static final FileTypeMap fileTypeMap;

			static {
				fileTypeMap = loadFileTypeMapFromContextSupportModule();
			}

			private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
				// see if we can find the extended mime.types from the context-support module
				Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
				if (mappingLocation.exists()) {
					if (logger.isTraceEnabled()) {
						logger.trace("Loading Java Activation Framework FileTypeMap from " + mappingLocation);
					}
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
				if (logger.isTraceEnabled()) {
					logger.trace("Loading default Java Activation Framework FileTypeMap");
				}
				return FileTypeMap.getDefaultFileTypeMap();
			}

			public static MediaType getMediaType(String fileName) {
				String mediaType = fileTypeMap.getContentType(fileName);
				return StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null;
			}
		}
	}
	
	private class GZIPResponseStream extends ServletOutputStream {

		private ByteArrayOutputStream byteStream = null;

		private GZIPOutputStream gzipStream = null;

		private boolean closed = false;

		private HttpServletResponse response = null;

		private ServletOutputStream servletStream = null;

		public GZIPResponseStream(HttpServletResponse response) throws IOException {
			super();
			closed = false;
			this.response = response;
			this.servletStream = response.getOutputStream();
			byteStream = new ByteArrayOutputStream();
			gzipStream = new GZIPOutputStream(byteStream);
		}

		public void close() throws IOException {
			if (closed) {
				throw new IOException("This output stream has already been closed");
			}
			gzipStream.finish();
			byte[] bytes = byteStream.toByteArray();
			response.setContentLength(bytes.length);
			response.addHeader("Content-Encoding", "gzip");
			servletStream.write(bytes);
			servletStream.flush();
			servletStream.close();
			closed = true;
		}

		public void flush() throws IOException {
			if (closed) {
				throw new IOException("Cannot flush a closed output stream");
			}
			gzipStream.flush();
		}

		public void write(int b) throws IOException {
			if (closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			gzipStream.write((byte) b);
		}

		public void write(byte b[]) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte b[], int off, int len) throws IOException {
			if (closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			gzipStream.write(b, off, len);
		}
	}
	
	private static class URLResource implements Resource {
		
		private final Resource wrapped;
		
		private final long lastModified;
		
		private final int contentLength;
		
		public URLResource(Resource wrapped) throws IOException {
			this.wrapped = wrapped;
			URLConnection connection = null;
			try {
				connection = wrapped.getURL().openConnection();
				this.lastModified = connection.getLastModified();
				this.contentLength = connection.getContentLength();
			} finally {
				if (connection != null) {
					connection.getInputStream().close();
				}
			}
		}
		
		public int getContentLength() {
			return this.contentLength;
		}
		
		public long lastModified() throws IOException {
			return this.lastModified;
		}

		public Resource createRelative(String relativePath) throws IOException {
			return wrapped.createRelative(relativePath);
		}

		public boolean exists() {
			return wrapped.exists();
		}

		public String getDescription() {
			return wrapped.getDescription();
		}

		public File getFile() throws IOException {
			return wrapped.getFile();
		}

		public String getFilename() {
			return wrapped.getFilename();
		}

		public URI getURI() throws IOException {
			return wrapped.getURI();
		}

		public URL getURL() throws IOException {
			return wrapped.getURL();
		}

		public boolean isOpen() {
			return wrapped.isOpen();
		}

		public boolean isReadable() {
			return wrapped.isReadable();
		}

		public InputStream getInputStream() throws IOException {
			return wrapped.getInputStream();
		}		
	}
}
