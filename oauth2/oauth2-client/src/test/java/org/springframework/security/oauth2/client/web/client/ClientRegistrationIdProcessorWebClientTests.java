package org.springframework.security.oauth2.client.web.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Runs tests for {@link ClientRegistrationIdProcessor} with {@link WebClient} to ensure
 * that all the parts work together properly.
 *
 * @author Rob Winch
 * @since 7.0
 */
@ExtendWith(MockitoExtension.class)
class ClientRegistrationIdProcessorWebClientTests extends AbstractMockServerClientRegistrationIdProcessorTests {

	@Test
	void clientRegistrationIdProcessorWorksWithReactiveWebClient() throws InterruptedException {
		ReactiveOAuth2AuthorizedClientManager authorizedClientManager = mock(
				ReactiveOAuth2AuthorizedClientManager.class);
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager);

		WebClient.Builder builder = WebClient.builder().filter(oauth2Client).baseUrl(this.baseUrl);

		ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequest = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
		given(authorizedClientManager.authorize(authorizeRequest.capture()))
			.willReturn(Mono.just(this.authorizedClient));

		testWithAdapter(WebClientAdapter.create(builder.build()));

		assertThat(authorizeRequest.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
	}

	@Test
	void clientRegistrationIdProcessorWorksWithServletWebClient() throws InterruptedException {
		OAuth2AuthorizedClientManager authorizedClientManager = mock(OAuth2AuthorizedClientManager.class);

		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager);

		WebClient.Builder builder = WebClient.builder().filter(oauth2Client).baseUrl(this.baseUrl);

		ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequest = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
		given(authorizedClientManager.authorize(authorizeRequest.capture())).willReturn(this.authorizedClient);

		testWithAdapter(WebClientAdapter.create(builder.build()));

		assertThat(authorizeRequest.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
	}

}
