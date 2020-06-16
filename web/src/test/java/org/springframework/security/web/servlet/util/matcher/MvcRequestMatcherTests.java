/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.security.web.servlet.util.matcher;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.ServletRequestPathFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 * @author Eddú Meléndez
 */
@RunWith(MockitoJUnitRunner.class)
public class MvcRequestMatcherTests {
	@Mock
	HandlerMappingIntrospector introspector;
	@Mock
	MatchableHandlerMapping mapping;
	@Mock
	RequestMatchResult result;
	@Captor
	ArgumentCaptor<String> pattern;
	MockHttpServletRequest request;

	MvcRequestMatcher matcher;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setMethod("GET");
		this.request.setServletPath("/path");
		this.matcher = new MvcRequestMatcher(this.introspector, "/path");
	}

	@Test
	public void extractUriTemplateVariablesSuccess() throws Exception {
		this.matcher = new MvcRequestMatcher(this.introspector, "/{p}");
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenReturn(null);

		assertThat(this.matcher.extractUriTemplateVariables(this.request))
				.containsEntry("p", "path");
		assertThat(this.matcher.matcher(this.request).getVariables())
				.containsEntry("p", "path");
	}

	@Test
	public void extractUriTemplateVariablesFail() throws Exception {
		when(this.result.extractUriTemplateVariables())
				.thenReturn(Collections.<String, String>emptyMap());
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);
		when(this.mapping.match(eq(this.request), this.pattern.capture()))
				.thenReturn(this.result);

		assertThat(this.matcher.extractUriTemplateVariables(this.request)).isEmpty();
		assertThat(this.matcher.matcher(this.request).getVariables()).isEmpty();
	}

	@Test
	public void extractUriTemplateVariablesDefaultSuccess() throws Exception {
		this.matcher = new MvcRequestMatcher(this.introspector, "/{p}");
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenReturn(null);

		assertThat(this.matcher.extractUriTemplateVariables(this.request))
				.containsEntry("p", "path");
		assertThat(this.matcher.matcher(this.request).getVariables())
				.containsEntry("p", "path");
	}

	@Test
	public void extractUriTemplateVariablesDefaultFail() throws Exception {
		this.matcher = new MvcRequestMatcher(this.introspector, "/nomatch/{p}");
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenReturn(null);

		assertThat(this.matcher.extractUriTemplateVariables(this.request)).isEmpty();
		assertThat(this.matcher.matcher(this.request).getVariables()).isEmpty();
	}

	@Test
	public void matchesServletPathTrue() throws Exception {
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);
		when(this.mapping.match(eq(this.request), this.pattern.capture()))
				.thenReturn(this.result);
		this.matcher.setServletPath("/spring");
		this.request.setServletPath("/spring");

		assertThat(this.matcher.matches(this.request)).isTrue();
		assertThat(this.pattern.getValue()).isEqualTo("/path");
	}

	@Test
	public void matchesServletPathFalse() {
		this.matcher.setServletPath("/spring");
		this.request.setServletPath("/");

		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesPathOnlyTrue() throws Exception {
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);
		when(this.mapping.match(eq(this.request), this.pattern.capture()))
				.thenReturn(this.result);

		assertThat(this.matcher.matches(this.request)).isTrue();
		assertThat(this.pattern.getValue()).isEqualTo("/path");
	}

	@Test
	public void matchesDefaultMatches() throws Exception {
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenReturn(null);

		assertThat(this.matcher.matches(this.request)).isTrue();
	}

	@Test
	public void matchesDefaultDoesNotMatch() throws Exception {
		this.request.setServletPath("/other");
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenReturn(null);

		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesPathOnlyFalse() throws Exception {
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);

		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesMethodAndPathTrue() throws Exception {
		this.matcher.setMethod(HttpMethod.GET);
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);
		when(this.mapping.match(eq(this.request), this.pattern.capture()))
				.thenReturn(this.result);

		assertThat(this.matcher.matches(this.request)).isTrue();
		assertThat(this.pattern.getValue()).isEqualTo("/path");
	}

	@Test
	public void matchesMethodAndPathFalseMethod() {
		this.matcher.setMethod(HttpMethod.POST);

		assertThat(this.matcher.matches(this.request)).isFalse();
		// method compare should be done first since faster
		verifyZeroInteractions(this.introspector);
	}

	/**
	 * Malicious users can specify any HTTP Method to create a stacktrace and try to
	 * expose useful information about the system. We should ensure we ignore invalid HTTP
	 * methods.
	 */
	@Test
	public void matchesInvalidMethodOnRequest() {
		this.matcher.setMethod(HttpMethod.GET);
		this.request.setMethod("invalid");

		assertThat(this.matcher.matches(this.request)).isFalse();
		// method compare should be done first since faster
		verifyZeroInteractions(this.introspector);
	}

	@Test
	public void matchesMethodAndPathFalsePath() throws Exception {
		this.matcher.setMethod(HttpMethod.GET);
		when(this.introspector.getMatchableHandlerMapping(this.request))
				.thenReturn(this.mapping);

		assertThat(this.matcher.matches(this.request)).isFalse();
	}

	@Test
	public void matchesGetMatchableHandlerMappingNull() {
		assertThat(this.matcher.matches(this.request)).isTrue();
	}

	@Test
	public void matchesGetMatchableHandlerMappingThrows() throws Exception {
		when(this.introspector.getMatchableHandlerMapping(this.request)).thenThrow(
				new HttpRequestMethodNotSupportedException(this.request.getMethod()));
		assertThat(this.matcher.matches(this.request)).isTrue();
	}

	@Test
	public void patternParser() throws Exception {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			context.register(MyRestController.class, PathMatchConfigurerConfiguration.class);
			context.setServletContext(new MockServletContext());
			context.setServletConfig(new MockServletConfig());
			context.refresh();
			HandlerMappingIntrospector introspector = context.getBean(HandlerMappingIntrospector.class);

			MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/");
			ServletRequestPathUtils.parseAndCache(request);
			MatchableHandlerMapping matchableHandlerMapping = introspector.getMatchableHandlerMapping(request);
			assertThat(matchableHandlerMapping.getPatternParser()).isNotNull();

			this.matcher = new MvcRequestMatcher(introspector, "/**");

			assertThat(this.matcher.matches(request)).isTrue();
		}
	}

	@RestController
	static class MyRestController {
		@GetMapping("/")
		String home() { return "home"; }
	}

	@EnableWebMvc
	@Configuration
	static class PathMatchConfigurerConfiguration implements WebMvcConfigurer {
		@Override
		public void configurePathMatch(PathMatchConfigurer pathMatchConfigurer) {
			pathMatchConfigurer.setPatternParser(new PathPatternParser());
		}
	}

	@Test
	public void toStringWhenAll() {
		this.matcher.setMethod(HttpMethod.GET);
		this.matcher.setServletPath("/spring");

		assertThat(this.matcher.toString()).isEqualTo("Mvc [pattern='/path', servletPath='/spring', GET]");
	}

	@Test
	public void toStringWhenHttpMethod() {
		this.matcher.setMethod(HttpMethod.GET);

		assertThat(this.matcher.toString()).isEqualTo("Mvc [pattern='/path', GET]");
	}

	@Test
	public void toStringWhenServletPath() {
		this.matcher.setServletPath("/spring");

		assertThat(this.matcher.toString()).isEqualTo("Mvc [pattern='/path', servletPath='/spring']");
	}

	@Test
	public void toStringWhenOnlyPattern() {
		assertThat(this.matcher.toString()).isEqualTo("Mvc [pattern='/path']");
	}
}
