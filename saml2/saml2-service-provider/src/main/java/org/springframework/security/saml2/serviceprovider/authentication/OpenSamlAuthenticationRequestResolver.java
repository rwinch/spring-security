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

package org.springframework.security.saml2.serviceprovider.authentication;

import java.time.Clock;
import java.util.UUID;

import org.joda.time.DateTime;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.signature.support.SignatureException;

/**
 * @since 5.2
 */
public class OpenSamlAuthenticationRequestResolver implements Saml2AuthenticationRequestResolver {
	private final Clock clock = Clock.systemUTC();
	private final OpenSamlImplementation saml = OpenSamlImplementation.getInstance();

	@Override
	public String resolveAuthenticationRequest(Saml2AuthenticationRequest request) {
		AuthnRequest auth = this.saml.buildSAMLObject(AuthnRequest.class);
		auth.setID("ARQ" + UUID.randomUUID().toString().substring(1));
		auth.setIssueInstant(new DateTime(this.clock.millis()));
		auth.setForceAuthn(Boolean.FALSE);
		auth.setIsPassive(Boolean.FALSE);
		auth.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
		Issuer issuer = this.saml.buildSAMLObject(Issuer.class);
		issuer.setValue(request.getLocalSpEntityId());
		auth.setIssuer(issuer);
		auth.setDestination(request.getWebSsoUri());
		try {
			return this.saml.toXml(
					auth,
					request.getCredentials(),
					request.getLocalSpEntityId()
			);
		}
		catch (MarshallingException | SignatureException | SecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
