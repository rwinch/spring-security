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

package org.springframework.security.web.util.matcher;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link RequestMatcher} that leverages the HTTP method and a {@link PathPattern} for matching. The URL is parsed
 * using a {@link UrlPathHelper}
 *
 * @author Rob Winch
 * @since 5.1
 */
public class PathPatternRequestMatcher implements RequestMatcher {
	private static final PathPatternParser DEFAULT_PATTERN_PARSER = new PathPatternParser();

	private static final UrlPathHelper DEFAULT_URL_PATH_HELPER = new UrlPathHelper();

	private UrlPathHelper urlPathHelper = DEFAULT_URL_PATH_HELPER;
	private final PathPattern pattern;
	private final String method;

	public PathPatternRequestMatcher(PathPattern pattern) {
		this(pattern, null);
	}

	public PathPatternRequestMatcher(PathPattern pattern, HttpMethod method) {
		Assert.notNull(pattern, "pattern cannot be null");
		this.pattern = pattern;
		this.method = method.name();
	}

	public PathPatternRequestMatcher(String pattern, HttpMethod method) {
		Assert.notNull(pattern, "pattern cannot be null");
		this.pattern = DEFAULT_PATTERN_PARSER.parse(pattern);
		this.method = method == null ? null : method.name();
	}

	public PathPatternRequestMatcher(String pattern) {
		this(pattern, null);
	}

	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "urlPathHelper cannot be null");
		this.urlPathHelper = urlPathHelper;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		if(this.method != null && !this.method.equals(request.getMethod())) {
			return false;
		}
		String path = this.urlPathHelper.getPathWithinApplication(request);
		PathContainer pathContainer = PathContainer.parsePath(path);
		return this.pattern.matches(pathContainer);
	}
}
