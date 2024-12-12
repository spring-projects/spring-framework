package org.springframework.http.support;

import org.springframework.http.ResponseCookie;

import java.util.stream.Stream;

public interface HttpCookieParser {

	Stream<ResponseCookie> parse(String header);
}
