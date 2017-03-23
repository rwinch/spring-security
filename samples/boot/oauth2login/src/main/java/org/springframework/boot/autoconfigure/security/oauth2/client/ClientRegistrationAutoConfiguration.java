/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.util.StringUtils;

/**
 * @author Joe Grandja
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(ClientRegistration.class)
public class ClientRegistrationAutoConfiguration {
	private static final String OAUTH2_CLIENT_PROPERTY_PREFIX_FORMAT = "security.oauth2.client.%1$s.";
	private static final String CLIENT_ID_PROPERTY = "client-id";
	private static final String CLIENT_SECRET_PROPERTY = "client-secret";

	@Configuration
	@Conditional(GoogleClientCondition.class)
	@EnableConfigurationProperties(GoogleClientProperties.class)
	protected static class GoogleClientRegistration {
		private final GoogleClientProperties properties;

		protected GoogleClientRegistration(GoogleClientProperties properties) {
			this.properties = properties;
		}

		@Bean
		protected ClientRegistration googleClientRegistration() {
			return new ClientRegistration.Builder(this.properties).build();
		}
	}

	@Configuration
	@Conditional(GitHubClientCondition.class)
	@EnableConfigurationProperties(GitHubClientProperties.class)
	protected static class GitHubClientRegistration {
		private final GitHubClientProperties properties;

		protected GitHubClientRegistration(GitHubClientProperties properties) {
			this.properties = properties;
		}

		@Bean
		protected ClientRegistration githubClientRegistration() {
			return new ClientRegistration.Builder(this.properties).build();
		}
	}

	@Configuration
	@Conditional(FacebookClientCondition.class)
	@EnableConfigurationProperties(FacebookClientProperties.class)
	protected static class FacebookClientRegistration {
		private final FacebookClientProperties properties;

		protected FacebookClientRegistration(FacebookClientProperties properties) {
			this.properties = properties;
		}

		@Bean
		protected ClientRegistration facebookClientRegistration() {
			return new ClientRegistration.Builder(this.properties).build();
		}
	}

	@Configuration
	@Conditional(OktaClientCondition.class)
	@EnableConfigurationProperties(OktaClientProperties.class)
	protected static class OktaClientRegistration {
		private final OktaClientProperties properties;

		protected OktaClientRegistration(OktaClientProperties properties) {
			this.properties = properties;
		}

		@Bean
		protected ClientRegistration oktaClientRegistration() {
			return new ClientRegistration.Builder(this.properties).build();
		}
	}

	private static class GoogleClientCondition extends OAuth2ClientCondition {
		private GoogleClientCondition() {
			super("Google", String.format(OAUTH2_CLIENT_PROPERTY_PREFIX_FORMAT, "google"));
		}
	}

	private static class GitHubClientCondition extends OAuth2ClientCondition {
		private GitHubClientCondition() {
			super("GitHub", String.format(OAUTH2_CLIENT_PROPERTY_PREFIX_FORMAT, "github"));
		}
	}

	private static class FacebookClientCondition extends OAuth2ClientCondition {
		private FacebookClientCondition() {
			super("Facebook", String.format(OAUTH2_CLIENT_PROPERTY_PREFIX_FORMAT, "facebook"));
		}
	}

	private static class OktaClientCondition extends OAuth2ClientCondition {
		private OktaClientCondition() {
			super("Okta", String.format(OAUTH2_CLIENT_PROPERTY_PREFIX_FORMAT, "okta"));
		}
	}

	private static class OAuth2ClientCondition extends SpringBootCondition implements ConfigurationCondition {
		private final String clientName;
		private final String propertyPrefix;

		private OAuth2ClientCondition(String clientName, String propertyPrefix) {
			this.clientName = clientName;
			this.propertyPrefix = propertyPrefix;
		}

		@Override
		public ConfigurationCondition.ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder messageBuilder = ConditionMessage.forCondition(this.clientName + " OAuth2 Client");
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), this.propertyPrefix);
			String clientId = resolver.getProperty(CLIENT_ID_PROPERTY);
			String clientSecret = resolver.getProperty(CLIENT_SECRET_PROPERTY);
			if (StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)) {
				return ConditionOutcome.match(messageBuilder.foundExactly(this.clientName + " Client: " + clientId));
			}
			return ConditionOutcome.noMatch(messageBuilder.notAvailable(this.clientName + " Client"));
		}
	}
}
