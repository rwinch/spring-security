/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.security.saml2.serviceprovider.provider;

/**
 * Resolves a configured service provider and remote identity provider pair by entityId or registrationId
 * @since 5.2
 */
public interface RelyingPartyRegistrationRepository {

	/**
	 * Resolves an entity provider by entityId, cannot be null
	 *
	 * @param idpEntityId - unique entityId for the remote identity provider, not null
	 * @return {@link RelyingPartyRegistration} if found, otherwise {@code null}
	 */
	RelyingPartyRegistration findByEntityId(String idpEntityId);

	/**
	 * Resolves an entity provider by registrationId, or returns the default provider
	 * if no registrationId is provided
	 *
	 * @param registrationId - unique registrationId, can be null
	 * @return {@link RelyingPartyRegistration} if found, {@code null} if an registrationId is provided and
	 * no provider is found, and the default {@link RelyingPartyRegistration} if no registrationId is provided
	 */
	RelyingPartyRegistration findByRegistrationId(String registrationId);

}
