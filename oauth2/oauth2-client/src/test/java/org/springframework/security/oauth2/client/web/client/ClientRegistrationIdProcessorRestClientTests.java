package org.springframework.security.oauth2.client.web.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Runs tests of {@link ClientRegistrationIdProcessor} with {@link RestClient} to ensure
 * that all the parts work together properly.
 *
 * @author Rob Winch
 * @since 7.0
 */
@ExtendWith(MockitoExtension.class)
class ClientRegistrationIdProcessorRestClientTests extends AbstractMockServerClientRegistrationIdProcessorTests {

	@Mock
	private OAuth2AuthorizedClientManager authorizedClientManager;

	@Test
	void clientRegistrationIdProcessorWorksWithRestClientAdapter() throws InterruptedException {
		OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(
				this.authorizedClientManager);
		RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor).baseUrl(this.baseUrl);

		ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequest = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
		given(this.authorizedClientManager.authorize(authorizeRequest.capture())).willReturn(authorizedClient);

		testWithAdapter(RestClientAdapter.create(builder.build()));

		assertThat(authorizeRequest.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
	}

}
