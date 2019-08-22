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

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.credentials.Saml2X509Credential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.assertion.AssertionValidationException;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.assertion.ConditionValidator;
import org.opensaml.saml.saml2.assertion.SAML20AssertionValidator;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.assertion.StatementValidator;
import org.opensaml.saml.saml2.assertion.SubjectConfirmationValidator;
import org.opensaml.saml.saml2.assertion.impl.AudienceRestrictionConditionValidator;
import org.opensaml.saml.saml2.assertion.impl.BearerSubjectConfirmationValidator;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.impl.CollectionCredentialResolver;
import org.opensaml.xmlsec.config.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.springframework.util.Assert.notNull;
import static org.springframework.util.StringUtils.hasText;

/**
 * @since 5.2
 */
public class OpenSamlAuthenticationProvider implements AuthenticationProvider {

	private static Log logger = LogFactory.getLog(OpenSamlAuthenticationProvider.class);

	private final OpenSamlImplementation saml = OpenSamlImplementation.getInstance();
	private GrantedAuthoritiesMapper authoritiesMapper = (a -> a);
	private Duration responseTimeValidationSkewMillis = Duration.ofMillis(1000 * 60 * 5); // 5 minutes

	private String getUsername(Saml2AuthenticationToken idp, Assertion assertion) {
		final Subject subject = assertion.getSubject();
		if (subject == null) {
			return null;
		}
		if (subject.getNameID() != null) {
			return subject.getNameID().getValue();
		}
		if (subject.getEncryptedID() != null) {
			NameID nameId = decrypt(idp, subject.getEncryptedID());
			return nameId.getValue();
		}
		return null;
	}

	private Assertion validateSaml2Response(Saml2AuthenticationToken idp,
											String recipient,
											Response samlResponse) throws AuthenticationException {
		if (hasText(samlResponse.getDestination()) && !recipient.equals(samlResponse.getDestination())) {
			throw new ProviderNotFoundException("Invalid SAML response destination: " + samlResponse.getDestination());
		}

		final String issuer = samlResponse.getIssuer().getValue();
		if (logger.isDebugEnabled()) {
			logger.debug("Processing SAML response from " + issuer);
		}
		if (idp == null) {
			throw new ProviderNotFoundException(format("SAML 2 Provider for %s was not found.", issuer));
		}
		boolean responseSigned = hasValidSignature(samlResponse, idp);
		for (Assertion a : samlResponse.getAssertions()) {
			if (isValidAssertion(recipient, a, idp, !responseSigned)) {
				return a;
			}
		}
		for (EncryptedAssertion ea : samlResponse.getEncryptedAssertions()) {
			Assertion a = decrypt(idp, ea);
			if (isValidAssertion(recipient, a, idp, false)) {
				return a;
			}
		}
		throw new InsufficientAuthenticationException("Unable to find a valid assertion");
	}

	private boolean hasValidSignature(SignableSAMLObject samlResponse, Saml2AuthenticationToken idp) {
		if (!samlResponse.isSigned()) {
			return false;
		}

		final List<X509Certificate> verificationKeys = getVerificationKeys(idp);
		if (verificationKeys.isEmpty()) {
			return false;
		}

		for (X509Certificate key : verificationKeys) {
			final Credential credential = getVerificationCredential(key);
			try {
				SignatureValidator.validate(samlResponse.getSignature(), credential);
				return true;
			}
			catch (SignatureException ignored) {
			}
		}
		return false;
	}

	private boolean isValidAssertion(String recipient, Assertion a, Saml2AuthenticationToken idp, boolean signatureRequired) {
		final SAML20AssertionValidator validator = getAssertionValidator(idp);
		Map<String, Object> validationParams = new HashMap<>();
		validationParams.put(SAML2AssertionValidationParameters.SIGNATURE_REQUIRED, false);
		validationParams.put(
				SAML2AssertionValidationParameters.CLOCK_SKEW,
				this.responseTimeValidationSkewMillis
		);
		validationParams.put(
				SAML2AssertionValidationParameters.COND_VALID_AUDIENCES,
				singleton(idp.getLocalSpEntityId())
		);
		if (hasText(recipient)) {
			validationParams.put(SAML2AssertionValidationParameters.SC_VALID_RECIPIENTS, singleton(recipient));
		}

		if (signatureRequired && !hasValidSignature(a, idp)) {
			if (logger.isDebugEnabled()) {
				logger.debug(format("Assertion [%s] does not a valid signature.", a.getID()));
			}
			return false;
		}
		a.setSignature(null);

		// validation for recipient
		ValidationContext vctx = new ValidationContext(validationParams);
		try {
			final ValidationResult result = validator.validate(a, vctx);
			final boolean valid = result.equals(ValidationResult.VALID);
			if (!valid) {
				if (logger.isDebugEnabled()) {
					logger.debug(format("Failed to validate assertion from %s with user %s", idp.getIdpEntityId(),
							getUsername(idp, a)
					));
				}
			}
			return valid;
		}
		catch (AssertionValidationException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to validate assertion:", e);
			}
			return false;
		}

	}

	private Response getSaml2Response(String xml) throws Saml2Exception, AuthenticationException {
		final Object result = this.saml.resolve(xml);
		if (result == null) {
			throw new AuthenticationCredentialsNotFoundException("SAMLResponse returned null object");
		}
		else if (result instanceof Response) {
			return (Response) result;
		}
		throw new ClassCastException(result.getClass().getName());
	}

	private SAML20AssertionValidator getAssertionValidator(Saml2AuthenticationToken provider) {
		List<ConditionValidator> conditions = Collections.singletonList(new AudienceRestrictionConditionValidator());
		final BearerSubjectConfirmationValidator subjectConfirmationValidator =
				new BearerSubjectConfirmationValidator();

		List<SubjectConfirmationValidator> subjects = Collections.singletonList(subjectConfirmationValidator);
		List<StatementValidator> statements = Collections.emptyList();

		Set<Credential> credentials = new HashSet<>();
		for (X509Certificate key : getVerificationKeys(provider)) {
			final Credential cred = getVerificationCredential(key);
			credentials.add(cred);
		}
		CredentialResolver credentialsResolver = new CollectionCredentialResolver(credentials);
		SignatureTrustEngine signatureTrustEngine = new ExplicitKeySignatureTrustEngine(
				credentialsResolver,
				DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver()
		);
		SignaturePrevalidator signaturePrevalidator = new SAMLSignatureProfileValidator();
		return new SAML20AssertionValidator(
				conditions,
				subjects,
				statements,
				signatureTrustEngine,
				signaturePrevalidator
		);
	}

	private Credential getVerificationCredential(X509Certificate certificate) {
		return CredentialSupport.getSimpleCredential(certificate, null);
	}

	private Decrypter getDecrypter(Saml2X509Credential key) {
		Credential credential = CredentialSupport.getSimpleCredential(key.getCertificate(), key.getPrivateKey());
		KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(credential);
		Decrypter decrypter = new Decrypter(null, resolver, this.saml.getEncryptedKeyResolver());
		decrypter.setRootInNewDocument(true);
		return decrypter;
	}

	private Assertion decrypt(Saml2AuthenticationToken idp, EncryptedAssertion assertion) {
		Saml2Exception last = null;
		for (Saml2X509Credential key : getDecryptionCredentials(idp)) {
			final Decrypter decrypter = getDecrypter(key);
			try {
				return decrypter.decrypt(assertion);
			}
			catch (DecryptionException e) {
				throw new Saml2Exception(e);
			}
		}
		throw last;
	}

	private NameID decrypt(Saml2AuthenticationToken token, EncryptedID assertion) {
		Saml2Exception last = null;
		for (Saml2X509Credential key : getDecryptionCredentials(token)) {
			final Decrypter decrypter = getDecrypter(key);
			try {
				return (NameID) decrypter.decrypt(assertion);
			}
			catch (DecryptionException e) {
				last = new Saml2Exception(e);
			}
		}
		throw last;
	}

	private List<Saml2X509Credential> getDecryptionCredentials(Saml2AuthenticationToken token) {
		List<Saml2X509Credential> result = new LinkedList<>();
		for (Saml2X509Credential c : token.getX509Credentials()) {
			if (c.isDecryptionCredential()) {
				result.add(c);
			}
		}
		return result;
	}

	private List<X509Certificate> getVerificationKeys(Saml2AuthenticationToken token) {
		List<X509Certificate> result = new LinkedList<>();
		for (Saml2X509Credential c : token.getX509Credentials()) {
			if (c.isSignatureVerficationCredential()) {
				result.add(c.getCertificate());
			}
		}
		return result;
	}

	public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		notNull(authoritiesMapper, "authoritiesMapper cannot be null");
		this.authoritiesMapper = authoritiesMapper;
	}

	public void setResponseTimeValidationSkewMillis(Duration responseTimeValidationSkewMillis) {
		this.responseTimeValidationSkewMillis = responseTimeValidationSkewMillis;
	}

	protected List<? extends GrantedAuthority> getAssertionAuthorities(Assertion assertion) {
		return singletonList(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Saml2AuthenticationToken token = (Saml2AuthenticationToken) authentication;
		String xml = token.getSaml2Response();
		Response samlResponse = getSaml2Response(xml);

		Assertion assertion = validateSaml2Response(token, token.getRecipientUri(), samlResponse);
		final String username = getUsername(token, assertion);
		if (username == null) {
			throw new UsernameNotFoundException("Assertion [" + assertion.getID() + "] is missing a user identifier");
		}
		return new Saml2Authentication(token.getSaml2Response(), () -> username,
				this.authoritiesMapper.mapAuthorities(getAssertionAuthorities(assertion))
		);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication != null && Saml2AuthenticationToken.class.isAssignableFrom(authentication);
	}
}
