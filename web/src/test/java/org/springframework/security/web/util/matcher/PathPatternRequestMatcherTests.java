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

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Rob Winch
 * @since 5.1
 */
public class PathPatternRequestMatcherTests {
	private PathPatternRequestMatcher matcher;

	private MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

	@Before
	public void setup() {
		this.matcher = new PathPatternRequestMatcher("/foo");
	}

	@Test
	public void matchesWhenUriExactThenTrue() {
		this.request.setRequestURI("/foo");
		assertThat(this.matcher.matches(this.request)).isTrue();
	}

	@Test
	public void matchesWhenUrinotMatchThenTrue() {
		this.request.setRequestURI("/bar");
		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesWhenDifferentMethodAndExactThenFalse() {
		this.matcher = new PathPatternRequestMatcher("/foo", HttpMethod.POST);
		this.request.setRequestURI("/foo");
		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesWhenUriTrailingSlashThenTrue() {
		this.request.setRequestURI("/foo/");
		assertThat(this.matcher.matches(this.request)).isTrue();
	}

}
