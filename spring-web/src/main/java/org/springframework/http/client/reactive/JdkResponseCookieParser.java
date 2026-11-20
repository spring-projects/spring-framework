/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseCookie;

/**
 * Parser that delegates to {@link java.net.HttpCookie#parse(String)} for parsing,
 * but also extracts and sets {@code sameSite}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
final class JdkResponseCookieParser implements ResponseCookie.Parser {

	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");


	/**
	 * Parse the given headers.
	 */
	@Override
	public List<ResponseCookie> parse(String header) {
		Matcher matcher = SAME_SITE_PATTERN.matcher(header);
		String sameSite = (matcher.matches() ? matcher.group(1) : null);
		List<java.net.HttpCookie> cookies = java.net.HttpCookie.parse(header);
		List<ResponseCookie> result = new ArrayList<>(cookies.size());
		cookies.forEach(cookie -> result.add(ResponseCookie.from(cookie).sameSite(sameSite).build()));
		return result;
	}

}
