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

import dagger.Component;
import javax.inject.Singleton;

/**
 * Main Dagger component that provides all dependencies for the Peppol lookup application.
 * This is the root of the dependency graph.
 */
@Singleton
@Component(modules = {PeppolModule.class})
public interface ApplicationComponent {

    // Main entry point
    PeppolLookupHandler peppolLookupHandler();

    // Services
    PeppolLookupService peppolLookupService();
    SmlLookupService smlLookupService();
    SmpQueryService smpQueryService();
    ValidationOrchestrator validationOrchestrator();
    CertificateService certificateService();
    RequestValidator requestValidator();

    // HTTP Client
    SecureHttpClient secureHttpClient();

    // Validators
    CertificateValidator certificateValidator();
    XmlSignatureValidator xmlSignatureValidator();
    EndpointValidator endpointValidator();

    // Clients
    OcspClient ocspClient();
    CrlClient crlClient();

    // JSON Processing
    ObjectMapper objectMapper();
}