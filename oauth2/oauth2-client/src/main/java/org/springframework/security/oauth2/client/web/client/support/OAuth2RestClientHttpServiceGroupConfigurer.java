package org.springframework.security.oauth2.client.web.client.support;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.ClientRegistrationIdProcessor;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.HttpRequestValues;

public class OAuth2RestClientHttpServiceGroupConfigurer implements
		RestClientHttpServiceGroupConfigurer {

	private final HttpRequestValues.Processor processor = new ClientRegistrationIdProcessor();

	private final ClientHttpRequestInterceptor interceptor;

	private OAuth2RestClientHttpServiceGroupConfigurer(
			ClientHttpRequestInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachClient((group, client) -> {
			client.requestInterceptor(this.interceptor);
		});
		groups.forEachProxyFactory((group,factory) -> {
			factory.httpRequestValuesProcessor(this.processor);
		});
	}

	public static OAuth2RestClientHttpServiceGroupConfigurer from(
			OAuth2AuthorizedClientManager authorizedClientManager) {
		OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager);
		return new OAuth2RestClientHttpServiceGroupConfigurer(interceptor);
	}

}
