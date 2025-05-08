package org.springframework.security.oauth2.client.web.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.TestOAuth2AccessTokens;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
public class ClientRegistrationIdProcessorHttpClientTests {
	@Mock
	private OAuth2AuthorizedClientManager authorizedClientManager;

	@Test
	void go() {
		OAuth2ClientHttpRequestInterceptor oauthRequestInterceptor = new OAuth2ClientHttpRequestInterceptor(this.authorizedClientManager);
		RestClient.Builder builder = RestClient.builder().requestInterceptor(oauthRequestInterceptor);
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ClientRegistrationIdProcessor processor = new ClientRegistrationIdProcessor();
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(RestClientAdapter.create(builder.build()))
				// FIXME: test webclient
//				.exchangeAdapter(WebClientAdapter.create(null))
				// FIXME strange that RestTemplateAdapter is never going to see the attributes
//				.exchangeAdapter(RestTemplateAdapter.create(null))
			.httpRequestValuesProcessor(processor)
			.build();
		MessageClient messages = factory.createClient(MessageClient.class);
		ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
				.build();
		OAuth2AccessToken accessToken = TestOAuth2AccessTokens.scopes("read", "write");
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				clientRegistration, "user", accessToken);

		ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequest = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
		given(this.authorizedClientManager.authorize(authorizeRequest.capture())).willReturn(authorizedClient);

		server.expect(requestTo("/message"))
				.andExpect(hasAuthorizationHeader(accessToken))
				.andRespond(withSuccess().body("Hello OAuth2!"));
		assertThat(messages.getMessage()).isEqualTo("Hello OAuth2!");
		server.verify();
		assertThat(authorizeRequest.getValue().getClientRegistrationId()).isEqualTo("okta");
	}

	private static RequestMatcher hasAuthorizationHeader(OAuth2AccessToken accessToken) {
		String tokenType = accessToken.getTokenType().getValue();
		String tokenValue = accessToken.getTokenValue();
		return header(HttpHeaders.AUTHORIZATION, "%s %s".formatted(tokenType, tokenValue));
	}

	static interface MessageClient {
		@GetExchange("/message")
		@ClientRegistrationId("okta")
		String getMessage();
	}

}
