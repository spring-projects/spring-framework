package org.springframework.http.converter.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;

/** @author Arjen Poutsma */
public class SourceHttpMessageConverter<T extends Source> extends AbstractXmlHttpMessageConverter<T> {

	public boolean supports(Class<? extends T> clazz) {
		return Source.class.isAssignableFrom(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T readFromSource(Class<T> clazz, HttpHeaders headers, Source source) throws IOException {
		if (DOMSource.class.equals(clazz)) {
			DOMResult domResult = new DOMResult();
			transform(source, domResult);
			return (T) new DOMSource(domResult.getNode());
		}
		else if (SAXSource.class.equals(clazz)) {
			ByteArrayInputStream bis = transformToByteArray(source);
			return (T) new SAXSource(new InputSource(bis));
		}
		else if (StreamSource.class.equals(clazz) || Source.class.equals(clazz)) {
			ByteArrayInputStream bis = transformToByteArray(source);
			return (T) new StreamSource(bis);
		}
		else {
			throw new HttpMessageConversionException(
					"Could not read class [" + clazz + "]. Only DOMSource, SAXSource, and StreamSource are supported.");
		}
	}

	private ByteArrayInputStream transformToByteArray(Source source) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		transform(source, new StreamResult(bos));
		return new ByteArrayInputStream(bos.toByteArray());
	}

	@Override
	protected void writeToResult(T t, HttpHeaders headers, Result result) throws IOException {
		transform(t, result);
	}
}
