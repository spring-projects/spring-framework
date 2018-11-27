/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

    /**
     * 路径集合
     */
	private final Set<String> patterns;

    /**
     * URL 路径工具类
     */
	private final UrlPathHelper pathHelper;

    /**
     * 路径匹配器
     */
	private final PathMatcher pathMatcher;

    /**
     * 使用前置匹配
     */
	private final boolean useSuffixPatternMatch;

    /**
     * 使用后置的 / 匹配
     */
	private final boolean useTrailingSlashMatch;

    /**
     * 后缀拓展集合
     */
	private final List<String> fileExtensions = new ArrayList<>();

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is prepended with "/".
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request.
	 */
	public PatternsRequestCondition(String... patterns) {
		this(Arrays.asList(patterns), null, null, true, true, null);
	}

	/**
	 * Additional constructor with flags for using suffix pattern (.*) and
	 * trailing slash matches.
	 * @param patterns the URL patterns to use; if 0, the condition will match to every request.
	 * @param urlPathHelper for determining the lookup path of a request
	 * @param pathMatcher for path matching with patterns
	 * @param useSuffixPatternMatch whether to enable matching by suffix (".*")
	 * @param useTrailingSlashMatch whether to match irrespective of a trailing slash
	 */
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch) {
		this(Arrays.asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, null);
	}

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is pre-pended with "/".
	 * @param patterns the URL patterns to use; if 0, the condition will match to every request.
	 * @param urlPathHelper a {@link UrlPathHelper} for determining the lookup path for a request
	 * @param pathMatcher a {@link PathMatcher} for pattern path matching
	 * @param useSuffixPatternMatch whether to enable matching by suffix (".*")
	 * @param useTrailingSlashMatch whether to match irrespective of a trailing slash
	 * @param fileExtensions a list of file extensions to consider for path matching
	 */
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {
		this(Arrays.asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch,
				useTrailingSlashMatch, fileExtensions);
	}

	/**
	 * Private constructor accepting a collection of patterns.
	 */
	private PatternsRequestCondition(Collection<String> patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {

		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns)); // 保证前缀都有 / 。如果没有，则进行补充
		this.pathHelper = (urlPathHelper != null ? urlPathHelper : new UrlPathHelper());
		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.useTrailingSlashMatch = useTrailingSlashMatch;

		// 初始化 fileExtensions 属性
		if (fileExtensions != null) {
			for (String fileExtension : fileExtensions) {
				if (fileExtension.charAt(0) != '.') {
					fileExtension = "." + fileExtension; // 补充前缀 .
				}
				this.fileExtensions.add(fileExtension);
			}
		}
	}


	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		Set<String> result = new LinkedHashSet<>(patterns.size());
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and
	 * the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using {@link PathMatcher#combine(String, String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new PatternsRequestCondition(result, this.pathHelper, this.pathMatcher,
				this.useSuffixPatternMatch, this.useTrailingSlashMatch, this.fileExtensions);
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance
	 * that is guaranteed to contain matching patterns, sorted via
	 * {@link PathMatcher#getPatternComparator(String)}.
	 * <p>A matching pattern is obtained by making checks in the following order:
	 * <ul>
	 * <li>Direct match
	 * <li>Pattern match with ".*" appended if the pattern doesn't already contain a "."
	 * <li>Pattern match
	 * <li>Pattern match with "/" appended if the pattern doesn't already end in "/"
	 * </ul>
	 * @param request the current request
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 不存在路径，直接返回自己，说明匹配成功
	    if (this.patterns.isEmpty()) {
			return this;
		}
        // 获得请求的路径
		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
	    // 执行匹配
		List<String> matches = getMatchingPatterns(lookupPath);
		// 如果匹配成功，则创建 PatternsRequestCondition 对象
        // 如果匹配失败，则返回 null
		return (!matches.isEmpty() ?
				new PatternsRequestCondition(matches, this.pathHelper, this.pathMatcher,
						this.useSuffixPatternMatch, this.useTrailingSlashMatch, this.fileExtensions) : null);
	}

	/**
	 * Find the patterns matching the given lookup path. Invoking this method should
	 * yield results equivalent to those of calling
	 * {@link #getMatchingCondition(javax.servlet.http.HttpServletRequest)}.
	 * This method is provided as an alternative to be used if no request is available
	 * (e.g. introspection, tooling, etc).
	 * @param lookupPath the lookup path to match to existing patterns
	 * @return a collection of matching patterns sorted with the closest match at the top
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		List<String> matches = new ArrayList<>();
		// 遍历 patterns 数组，逐个匹配
		for (String pattern : this.patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			// 匹配成功，添加到 matches 中
			if (match != null) {
				matches.add(match);
			}
		}
		// 如果匹配数量超过 1 个，则进行排序
		if (matches.size() > 1) {
			matches.sort(this.pathMatcher.getPatternComparator(lookupPath));
		}
		return matches;
	}

	@Nullable
	private String getMatchingPattern(String pattern, String lookupPath) {
		// 相等，直接返回
	    if (pattern.equals(lookupPath)) {
			return pattern;
		}
		// 前置匹配
		if (this.useSuffixPatternMatch) {
	        // 有文件后缀的匹配
			if (!this.fileExtensions.isEmpty() && lookupPath.indexOf('.') != -1) {
				for (String extension : this.fileExtensions) {
					if (this.pathMatcher.match(pattern + extension, lookupPath)) {
						return pattern + extension;
					}
				}
            // 无文件后缀的匹配
			} else {
				boolean hasSuffix = pattern.indexOf('.') != -1;
				if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
					return pattern + ".*";
				}
			}
		}
		// 默认匹配
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		// 后置斜杆匹配
		if (this.useTrailingSlashMatch) {
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				return pattern +"/";
			}
		}
		return null;
	}

	/**
	 * Compare the two conditions based on the URL patterns they contain.
	 * Patterns are compared one at a time, from top to bottom via
	 * {@link PathMatcher#getPatternComparator(String)}. If all compared
	 * patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@SuppressWarnings("Duplicates")
    @Override
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
	    // 获得请求的路径
		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
		// 获得路径 Comparator 比较器
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		// 获得当前和 other 的迭代器
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		// 逐个迭代，直到有一个不相等
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		// 注意，这里的逻辑是，按照升序
		// 如果当前还有新的，选择当前
		if (iterator.hasNext()) {
			return -1;
        // 如果 other 还有新的，选择 other
		} else if (iteratorOther.hasNext()) {
			return 1;
        // 相等
		} else {
			return 0;
		}
	}

}
