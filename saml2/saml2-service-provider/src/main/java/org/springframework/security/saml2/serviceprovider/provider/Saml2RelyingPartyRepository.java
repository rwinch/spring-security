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
 * Resolves a configured remote provider by entityId or alias
 */
public interface Saml2RelyingPartyRepository {

	/**
	 * Resolves an entity provider by entityId, cannot be null
	 *
	 * @param idpEntityId - unique entityId for the remote identity provider, not null
	 * @return a configured remote identity provider, or null if none found
	 */
	Saml2RelyingPartyRegistration findByEntityId(String idpEntityId);

	/**
	 * Resolves an entity provider by alias, or returns the default provider
	 * if no alias is provided
	 *
	 * @param alias - unique alias, can be null
	 * @return a configured remote identity provider, or null if none found
	 */
	Saml2RelyingPartyRegistration findByAlias(String alias);

}
