package com.tca.peppol.dagger;
import com.tca.peppol.handler.PeppolLookupHandler;
import com.tca.peppol.service.PeppolLookupService;
import com.tca.peppol.service.SmlLookupService;
import com.tca.peppol.service.SmpQueryService;
import com.tca.peppol.service.ValidationOrchestrator;
import com.tca.peppol.service.CertificateService;
import com.tca.peppol.validation.RequestValidator;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.client.OcspClient;
import com.tca.peppol.client.CrlClient;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.XmlSignatureValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Dagger module that provides all the dependencies for the Peppol lookup application.
 * This module defines how to create each dependency.
 */
@Module
public class PeppolModule {

    /**
     * Provides the main handler
     */
    @Provides
    @Singleton
    PeppolLookupHandler providePeppolLookupHandler(PeppolLookupService peppolLookupService) {
        return new PeppolLookupHandler(peppolLookupService);
    }

    /**
     * Provides the main service
     */
    @Provides
    @Singleton
    PeppolLookupService providePeppolLookupService(
            SmlLookupService smlLookupService,
            SmpQueryService smpQueryService,
            ValidationOrchestrator validationOrchestrator,
            CertificateService certificateService,
            RequestValidator requestValidator) {
        return new PeppolLookupService(smlLookupService, smpQueryService, validationOrchestrator, certificateService, requestValidator);
    }

    /**
     * Provides SML lookup service
     */
    @Provides
    @Singleton
    SmlLookupService provideSmlLookupService() {
        return new SmlLookupService();
    }

    /**
     * Provides SMP query service
     */
    @Provides
    @Singleton
    SmpQueryService provideSmpQueryService(SecureHttpClient httpClient) {
        return new SmpQueryService(httpClient, null);
    }

    /**
     * Provides validation orchestrator
     */
    @Provides
    @Singleton
    ValidationOrchestrator provideValidationOrchestrator(
            CertificateValidator certificateValidator,
            XmlSignatureValidator xmlSignatureValidator,
            EndpointValidator endpointValidator,
            OcspClient ocspClient,
            CrlClient crlClient) {
        return new ValidationOrchestrator(certificateValidator, xmlSignatureValidator, endpointValidator, ocspClient, crlClient);
    }

    /**
     * Provides certificate service
     */
    @Provides
    @Singleton
    CertificateService provideCertificateService() {
        return new CertificateService();
    }

    /**
     * Provides request validator
     */
    @Provides
    @Singleton
    RequestValidator provideRequestValidator() {
        return new RequestValidator();
    }

    /**
     * Provides secure HTTP client
     */
    @Provides
    @Singleton
    SecureHttpClient provideSecureHttpClient() {
        return new SecureHttpClient();
    }

    /**
     * Provides certificate validator
     */
    @Provides
    @Singleton
    CertificateValidator provideCertificateValidator() {
        return new CertificateValidator();
    }

    /**
     * Provides XML signature validator
     */
    @Provides
    @Singleton
    XmlSignatureValidator provideXmlSignatureValidator() {
        return new XmlSignatureValidator();
    }

    /**
     * Provides endpoint validator
     */
    @Provides
    @Singleton
    EndpointValidator provideEndpointValidator(SecureHttpClient httpClient) {
        return new EndpointValidator(httpClient);
    }

    /**
     * Provides OCSP client
     */
    @Provides
    @Singleton
    OcspClient provideOcspClient() {
        return new OcspClient();
    }

    /**
     * Provides CRL client
     */
    @Provides
    @Singleton
    CrlClient provideCrlClient() {
        return new CrlClient();
    }

    /**
     * Provides ObjectMapper for JSON processing
     */
    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
