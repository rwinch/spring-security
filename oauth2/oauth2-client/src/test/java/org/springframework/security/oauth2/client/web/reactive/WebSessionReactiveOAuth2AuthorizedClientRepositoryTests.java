package org.springframework.security.oauth2.client.web.reactive;

import org.junit.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.WebSession;

import javax.servlet.http.HttpSession;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Winch
 * @since 5.1
 */
public class WebSessionReactiveOAuth2AuthorizedClientRepositoryTests {
	private WebSessionReactiveOAuth2AuthorizedClientRepository authorizedClientRepository =
			new WebSessionReactiveOAuth2AuthorizedClientRepository();

	private MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private String registrationId1 = "registration-1";
	private String registrationId2 = "registration-2";
	private String principalName1 = "principalName-1";

	private ClientRegistration registration1 = ClientRegistration.withRegistrationId(this.registrationId1)
			.clientId("client-1")
			.clientSecret("secret")
			.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("user")
			.authorizationUri("https://provider.com/oauth2/authorize")
			.tokenUri("https://provider.com/oauth2/token")
			.userInfoUri("https://provider.com/oauth2/user")
			.userNameAttributeName("id")
			.clientName("client-1")
			.build();

	private ClientRegistration registration2 = ClientRegistration.withRegistrationId(this.registrationId2)
			.clientId("client-2")
			.clientSecret("secret")
			.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("openid", "profile", "email")
			.authorizationUri("https://provider.com/oauth2/authorize")
			.tokenUri("https://provider.com/oauth2/token")
			.userInfoUri("https://provider.com/oauth2/userinfo")
			.jwkSetUri("https://provider.com/oauth2/keys")
			.clientName("client-2")
			.build();


	@Test
	public void loadAuthorizedClientWhenClientRegistrationIdIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.loadAuthorizedClient(null, null, this.exchange).block())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void loadAuthorizedClientWhenPrincipalNameIsNullThenExceptionNotThrown() {
		this.authorizedClientRepository.loadAuthorizedClient(this.registrationId1, null, this.exchange).block();
	}

	@Test
	public void loadAuthorizedClientWhenRequestIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.loadAuthorizedClient(this.registrationId1, null, null).block())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void loadAuthorizedClientWhenClientRegistrationNotFoundThenReturnNull() {
		OAuth2AuthorizedClient authorizedClient =
				this.authorizedClientRepository.loadAuthorizedClient("registration-not-found", null, this.exchange).block();
		assertThat(authorizedClient).isNull();
	}

	@Test
	public void loadAuthorizedClientWhenSavedThenReturnAuthorizedClient() {
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				this.registration1, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, null, this.exchange).block();

		OAuth2AuthorizedClient loadedAuthorizedClient =
				this.authorizedClientRepository.loadAuthorizedClient(this.registrationId1, null, this.exchange).block();
		assertThat(loadedAuthorizedClient).isEqualTo(authorizedClient);
	}

	@Test
	public void saveAuthorizedClientWhenAuthorizedClientIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.saveAuthorizedClient(null, null, this.exchange).block())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void saveAuthorizedClientWhenAuthenticationIsNullThenExceptionNotThrown() {
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				this.registration2, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, null, this.exchange).block();
	}

	@Test
	public void saveAuthorizedClientWhenRequestIsNullThenThrowIllegalArgumentException() {
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				this.registration2, this.principalName1, mock(OAuth2AccessToken.class));
		assertThatThrownBy(() -> this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, null, null).block())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void saveAuthorizedClientWhenSavedThenSavedToSession() {
		OAuth2AuthorizedClient expected = new OAuth2AuthorizedClient(
				this.registration2, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(expected, null, this.exchange).block();

		OAuth2AuthorizedClient result = this.authorizedClientRepository
				.loadAuthorizedClient(this.registrationId2, null, this.exchange).block();

		assertThat(result).isEqualTo(expected);
	}

	@Test
	public void removeAuthorizedClientWhenClientRegistrationIdIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.removeAuthorizedClient(
				null, null, this.exchange)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void removeAuthorizedClientWhenPrincipalNameIsNullThenExceptionNotThrown() {
		this.authorizedClientRepository.removeAuthorizedClient(this.registrationId1, null, this.exchange);
	}

	@Test
	public void removeAuthorizedClientWhenRequestIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId1, null, null)).isInstanceOf(IllegalArgumentException.class);
	}


	@Test
	public void removeAuthorizedClientWhenNotSavedThenSessionNotCreated() {
		this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId2, null, this.exchange);
		assertThat(this.exchange.getSession().block().isStarted()).isFalse();
	}

	@Test
	public void removeAuthorizedClientWhenClient1SavedAndClient2RemovedThenClient1NotRemoved() {
		OAuth2AuthorizedClient authorizedClient1 = new OAuth2AuthorizedClient(
				this.registration1, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient1, null, this.exchange).block();

		// Remove registrationId2 (never added so is not removed either)
		this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId2, null, this.exchange);

		OAuth2AuthorizedClient loadedAuthorizedClient1 = this.authorizedClientRepository.loadAuthorizedClient(
				this.registrationId1, null, this.exchange).block();
		assertThat(loadedAuthorizedClient1).isNotNull();
		assertThat(loadedAuthorizedClient1).isSameAs(authorizedClient1);
	}

	@Test
	public void removeAuthorizedClientWhenSavedThenRemoved() {
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				this.registration2, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, null, this.exchange).block();
		OAuth2AuthorizedClient loadedAuthorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
				this.registrationId2, null, this.exchange).block();
		assertThat(loadedAuthorizedClient).isSameAs(authorizedClient);
		this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId2, null, this.exchange).block();
		loadedAuthorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
				this.registrationId2, null, this.exchange).block();
		assertThat(loadedAuthorizedClient).isNull();
	}

	@Test
	public void removeAuthorizedClientWhenSavedThenRemovedFromSession() {
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				this.registration1, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, null, this.exchange).block();
		OAuth2AuthorizedClient loadedAuthorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
				this.registrationId1, null, this.exchange).block();
		assertThat(loadedAuthorizedClient).isSameAs(authorizedClient);
		this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId1, null, this.exchange).block();

		WebSession session = this.exchange.getSession().block();
		assertThat(session).isNotNull();
		assertThat(session.getAttributes()).isEmpty();
	}

	@Test
	public void removeAuthorizedClientWhenClient1Client2SavedAndClient1RemovedThenClient2NotRemoved() {
		OAuth2AuthorizedClient authorizedClient1 = new OAuth2AuthorizedClient(
				this.registration1, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient1, null, this.exchange).block();

		OAuth2AuthorizedClient authorizedClient2 = new OAuth2AuthorizedClient(
				this.registration2, this.principalName1, mock(OAuth2AccessToken.class));
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient2, null, this.exchange).block();

		this.authorizedClientRepository.removeAuthorizedClient(
				this.registrationId1, null, this.exchange).block();

		OAuth2AuthorizedClient loadedAuthorizedClient2 = this.authorizedClientRepository.loadAuthorizedClient(
				this.registrationId2, null, this.exchange).block();
		assertThat(loadedAuthorizedClient2).isNotNull();
		assertThat(loadedAuthorizedClient2).isSameAs(authorizedClient2);
	}
}
