package org.springframework.security.access.expression;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;

/**
 * @author Luke Taylor
 */
public class AbstractSecurityExpressionHandlerTests {
	private AbstractSecurityExpressionHandler<Object> handler;

	@Before
	public void setUp() throws Exception {
		handler = new AbstractSecurityExpressionHandler<Object>() {
			@Override
			protected SecurityExpressionOperations createSecurityExpressionRoot(
					Authentication authentication, Object o) {
				return new SecurityExpressionRoot(authentication) {
				};
			}
		};
	}

	@Test
	public void beanNamesAreCorrectlyResolved() throws Exception {
		handler.setApplicationContext(new AnnotationConfigApplicationContext(
				TestConfiguration.class));

		Expression expression = handler.getExpressionParser().parseExpression(
				"@number10.compareTo(@number20) < 0");
		assertThat(expression.getValue(handler.createEvaluationContext(
				mock(Authentication.class), new Object()))).isEqualTo(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setExpressionParserNull() {
		handler.setExpressionParser(null);
	}

	@Test
	public void setExpressionParser() {
		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setExpressionParser(parser);
		assertThat(parser == handler.getExpressionParser()).isTrue();
	}
}

@Configuration
class TestConfiguration {

	@Bean
	Integer number10() {
		return 10;
	}

	@Bean
	Integer number20() {
		return 20;
	}
}
