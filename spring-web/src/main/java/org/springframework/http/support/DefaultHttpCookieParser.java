package org.springframework.http.support;

import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;

import java.net.HttpCookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DefaultHttpCookieParser implements HttpCookieParser {

	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

	@Override
	public Stream<ResponseCookie> parse(String header) {
		Matcher matcher = SAME_SITE_PATTERN.matcher(header);
		String sameSite = (matcher.matches() ? matcher.group(1) : null);
		return HttpCookie.parse(header).stream().map(cookie -> toResponseCookie(cookie, sameSite));
	}

	private static ResponseCookie toResponseCookie(HttpCookie cookie, @Nullable String sameSite) {
		return ResponseCookie.from(cookie.getName(), cookie.getValue())
				.domain(cookie.getDomain())
				.httpOnly(cookie.isHttpOnly())
				.maxAge(cookie.getMaxAge())
				.path(cookie.getPath())
				.secure(cookie.getSecure())
				.sameSite(sameSite)
				.build();
	}
}
