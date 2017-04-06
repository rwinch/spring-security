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
package org.springframework.security.oauth2.oidc.core.user;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;

/**
 * A resource representation of a User as defined by the <b>OpenID Connect</b>
 * <a target="_blank" href="http://openid.net/specs/openid-connect-core-1_0.html#UserInfo">UserInfo Endpoint</a>.
 *
 * @author Joe Grandja
 */
public interface UserInfo extends OAuth2User {

	String getSubject();

	String getGivenName();

	String getFamilyName();

	String getMiddleName();

	String getNickName();

	String getPreferredUsername();

	String getProfile();

	String getPicture();

	String getWebsite();

	String getEmail();

	Boolean getEmailVerified();

	String getGender();

	String getBirthdate();

	String getZoneInfo();

	String getLocale();

	String getPhoneNumber();

	Boolean getPhoneNumberVerified();

	Address getAddress();

	Instant getUpdatedAt();


	interface Address {

		String getFormatted();

		String getStreetAddress();

		String getLocality();

		String getRegion();

		String getPostalCode();

		String getCountry();
	}
}
