/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.security.access.expression.method;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultMethodSecurityExpressionHandlerTests {

	private DefaultMethodSecurityExpressionHandler handler;

	@Mock
	private Authentication authentication;

	@Mock
	private MethodInvocation methodInvocation;

	@Mock
	private AuthenticationTrustResolver trustResolver;

	@Before
	public void setup() {
		handler = new DefaultMethodSecurityExpressionHandler();
		when(methodInvocation.getThis()).thenReturn(new Foo());
		when(methodInvocation.getMethod()).thenReturn(Foo.class.getMethods()[0]);
	}

	@After
	public void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setTrustResolverNull() {
		handler.setTrustResolver(null);
	}

	@Test
	public void createEvaluationContextCustomTrustResolver() {
		handler.setTrustResolver(trustResolver);

		Expression expression = handler.getExpressionParser().parseExpression("anonymous");
		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);
		expression.getValue(context, Boolean.class);

		verify(trustResolver).isAnonymous(authentication);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filterByKeyWhenUsingMapThenFiltersMap() {
		final Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");

		Expression expression = handler.getExpressionParser().parseExpression("filterObject.key eq 'key2'");

		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);

		Object filtered = handler.filter(map, expression, context);

		assertThat(filtered == map);
		Map<String, String> result = ((Map<String, String>) filtered);
		assertThat(result.size() == 1);
		assertThat(result).containsKey("key2");
		assertThat(result).containsValue("value2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filterByValueWhenUsingMapThenFiltersMap() {
		final Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");

		Expression expression = handler.getExpressionParser().parseExpression("filterObject.value eq 'value3'");

		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);

		Object filtered = handler.filter(map, expression, context);

		assertThat(filtered == map);
		Map<String, String> result = ((Map<String, String>) filtered);
		assertThat(result.size() == 1);
		assertThat(result).containsKey("key3");
		assertThat(result).containsValue("value3");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filterByKeyAndValueWhenUsingMapThenFiltersMap() {
		final Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");

		Expression expression = handler.getExpressionParser()
				.parseExpression("(filterObject.key eq 'key1') or (filterObject.value eq 'value2')");

		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);

		Object filtered = handler.filter(map, expression, context);

		assertThat(filtered == map);
		Map<String, String> result = ((Map<String, String>) filtered);
		assertThat(result.size() == 2);
		assertThat(result).containsKeys("key1", "key2");
		assertThat(result).containsValues("value1", "value2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filterWhenUsingStreamThenFiltersStream() {
		final Stream<String> stream = Stream.of("1", "2", "3");

		Expression expression = handler.getExpressionParser().parseExpression("filterObject ne '2'");

		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);

		Object filtered = handler.filter(stream, expression, context);

		assertThat(filtered).isInstanceOf(Stream.class);
		List<String> list = ((Stream<String>) filtered).collect(Collectors.toList());
		assertThat(list).containsExactly("1", "3");
	}

	@Test
	public void filterStreamWhenClosedThenUpstreamGetsClosed() {
		final Stream<?> upstream = mock(Stream.class);
		doReturn(Stream.<String>empty()).when(upstream).filter(any());

		Expression expression = handler.getExpressionParser().parseExpression("true");

		EvaluationContext context = handler.createEvaluationContext(authentication, methodInvocation);

		((Stream) handler.filter(upstream, expression, context)).close();
		verify(upstream).close();
	}

	private static class Foo {

		public void bar() {
		}

	}

}
