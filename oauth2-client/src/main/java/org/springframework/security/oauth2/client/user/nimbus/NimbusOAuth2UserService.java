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
package org.springframework.security.oauth2.client.user.nimbus;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.user.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Joe Grandja
 */
public class NimbusOAuth2UserService implements OAuth2UserService {
	private final Map<URI, Converter<ClientHttpResponse, ? extends OAuth2User>> userInfoTypeConverters;

	public NimbusOAuth2UserService(Map<URI, Converter<ClientHttpResponse, ? extends OAuth2User>> userInfoTypeConverters) {
		Assert.notEmpty(userInfoTypeConverters, "userInfoTypeConverters cannot be empty");
		this.userInfoTypeConverters = new HashMap<>(userInfoTypeConverters);
	}

	@Override
	public OAuth2User loadUser(OAuth2AuthenticationToken token) throws OAuth2AuthenticationException {
		OAuth2User user;

		try {
			ClientRegistration clientRegistration = token.getClientRegistration();

			URI userInfoUri;
			try {
				userInfoUri = new URI(clientRegistration.getProviderDetails().getUserInfoUri());
			} catch (Exception ex) {
				throw new IllegalArgumentException("An error occurred parsing the userInfo URI: " +
					clientRegistration.getProviderDetails().getUserInfoUri(), ex);
			}

			Converter<ClientHttpResponse, ? extends OAuth2User> userInfoConverter = this.userInfoTypeConverters.get(userInfoUri);
			if (userInfoConverter == null) {
				throw new IllegalArgumentException("There is no available User Info converter for " + userInfoUri.toString());
			}

			BearerAccessToken accessToken = new BearerAccessToken(token.getAccessToken().getTokenValue());

			// Request the User Info
			UserInfoRequest userInfoRequest = new UserInfoRequest(userInfoUri, accessToken);
			HTTPRequest httpRequest = userInfoRequest.toHTTPRequest();
			httpRequest.setAccept(MediaType.APPLICATION_JSON_VALUE);
			HTTPResponse httpResponse = httpRequest.send();

			if (httpResponse.getStatusCode() != HTTPResponse.SC_OK) {
				UserInfoErrorResponse userInfoErrorResponse = UserInfoErrorResponse.parse(httpResponse);
				ErrorObject errorObject = userInfoErrorResponse.getErrorObject();
				OAuth2Error oauth2Error = OAuth2Error.valueOf(
					errorObject.getCode(), errorObject.getDescription(), errorObject.getURI().toString());
				throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
			}

			user = userInfoConverter.convert(new NimbusClientHttpResponse(httpResponse));

		} catch (ParseException ex) {
			// This error occurs if the User Info Response is not well-formed or invalid
			throw new OAuth2AuthenticationException(OAuth2Error.invalidUserInfoResponse(), ex);
		} catch (IOException ex) {
			// This error occurs when there is a network-related issue
			throw new AuthenticationServiceException("An error occurred while sending the User Info Request: " +
				ex.getMessage(), ex);
		}

		return user;
	}
}
