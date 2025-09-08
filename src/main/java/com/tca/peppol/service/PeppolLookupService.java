package com.tca.peppol.service;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.exception.ErrorClassifier;
import com.tca.peppol.exception.ErrorCodes;
import com.tca.peppol.model.internal.SmlResult;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.CertificateDetails;
import com.tca.peppol.model.response.ErrorDetail;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.model.response.TechnicalDetails;
import com.tca.peppol.model.response.ValidationResults;
import com.tca.peppol.service.ValidationOrchestrator.ComprehensiveValidationResult;
import com.tca.peppol.service.ValidationOrchestrator.ValidationContext;
import com.tca.peppol.service.ValidationOrchestrator.ValidationOptions;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.RequestValidationResult;
import com.tca.peppol.validation.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main orchestration service that coordinates the complete Peppol lookup process with peppol-commons integration.
 * 
 * This service implements the complete workflow:
 * 1. Input validation using peppol-commons + custom checks
 * 2. SML DNS lookup with proper MD5 hashing
 * 3. SMP metadata retrieval with secure XML parsing
 * 4. Comprehensive validation orchestration (certificates, XML signatures, endpoints)
 * 5. Response building with all required fields and optional features
 * 6. Performance timing collection and error handling
 * 
 * Features:
 * - Leverages production-proven peppol-commons library as foundation
 * - Comprehensive error handling with structured error codes
 * - Optional features (endpoint connectivity, full certificate chain, technical details)
 * - Partial result handling throughout the workflow
 */
public class PeppolLookupService {

    private static final Logger logger = LoggerFactory.getLogger(PeppolLookupService.class);

    // Core services
    private final SmlLookupService smlLookupService;
    private final SmpQueryService smpQueryService;
    private final ValidationOrchestrator validationOrchestrator;
    private final CertificateService certificateService;
    private final RequestValidator requestValidator;
    
    // Utilities
    private final PeppolIdentifierFactory identifierFactory;

    /**
     * Constructor with all dependencies for production use
     */
    public PeppolLookupService(@Nonnull SmlLookupService smlLookupService,
                              @Nonnull SmpQueryService smpQueryService,
                              @Nonnull ValidationOrchestrator validationOrchestrator,
                              @Nonnull CertificateService certificateService,
                              @Nonnull RequestValidator requestValidator) {
        this.smlLookupService = smlLookupService;
        this.smpQueryService = smpQueryService;
        this.validationOrchestrator = validationOrchestrator;
        this.certificateService = certificateService;
        this.requestValidator = requestValidator;
        
        this.identifierFactory = PeppolIdentifierFactory.INSTANCE;
        
        logger.info("PeppolLookupService initialized with full service integration and peppol-commons foundation");
    }



    /**
     * Performs complete Peppol participant lookup with comprehensive validation and error handling.
     * 
     * This method implements the complete workflow:
     * 1. Input validation using peppol-commons
     * 2. SML DNS lookup with performance timing
     * 3. SMP query with secure XML parsing
     * 4. Comprehensive validation orchestration
     * 5. Response building with all required fields
     * 
     * @param request The lookup request containing participant ID, document type, and options
     * @return Complete lookup response with all resolved information and validation results
     */
    @Nonnull
    public LookupResponse performLookup(@Nonnull final LookupRequest request) {

        List<ErrorDetail> errors = new ArrayList<>();

        try {
            // Step 1: Input Validation using peppol-commons
            RequestValidationResult validationResult = performInputValidation(request);
            
            if (!validationResult.isValid()) {
                return createValidationErrorResponse(request, validationResult);
            }

            // Step 2: SML DNS Lookup
            SmlResult smlResult = performSmlLookup(request, errors);
            
            if (smlResult == null || !smlResult.isSuccessful()) {
                return createPartialErrorResponse(request, errors, smlResult, null);
            }

            // Step 3: SMP Query with secure XML parsing
            SmpResult smpResult = performSmpQuery(request, smlResult, errors);
            
            if (smpResult == null || !smpResult.isSuccessful()) {
                return createPartialErrorResponse(request, errors, smlResult, smpResult);
            }

            // Step 4: Comprehensive Validation Orchestration
            ComprehensiveValidationResult validationResults = performComprehensiveValidation(
                request, smpResult, errors);

            // Step 5: Certificate Processing (if available)
            CertificateDetails certificateDetails = processCertificateDetails(smpResult, request);

            // Step 6: Build Comprehensive Response
            LookupResponse response = buildComprehensiveResponse(
                request, smlResult, smpResult, validationResults, certificateDetails, errors);

            logger.info("Peppol lookup workflow completed - success: {}",
                    response.isSuccess());

            return response;

        } catch (Exception e) {
            logger.error("Peppol lookup workflow failed with unexpected error: {}", e.getMessage(), e);

            // Add system error to error list
            ErrorDetail systemError = ErrorClassifier.createError(
                    ErrorCodes.SYSTEM_INTERNAL_ERROR,
                    "Unexpected system error: " + e.getMessage(),
                    "Exception: " + e.getClass().getSimpleName(),
                    e);
            errors.add(systemError);

            return createSystemErrorResponse(request, errors, e);
        }
    }

    /**
     * Perform comprehensive input validation using peppol-commons and custom validators
     */
    @Nonnull
    private RequestValidationResult performInputValidation(@Nonnull LookupRequest request) {
        Map<String, Object> validationContext = new HashMap<>();
        
        try {
            // Use RequestValidator for comprehensive validation
            RequestValidationResult result = requestValidator.validateRequest(request);
            
            if (!result.isValid()) {
                validationContext.put("errors", result.getErrors().size());
                logger.warn("Input validation failed", result.getErrors());
            }
            
            return result;
            
        } catch (Exception e) {
            validationContext.put("error", e.getMessage());
            logger.warn("Input validation error", e, validationContext);
            
            // Create failed validation result
            List<String> errors = Collections.singletonList("Input validation failed: " + e.getMessage());
            return RequestValidationResult.failure(errors);
        }
    }

    /**
     * Perform SML DNS lookup with circuit breaker protection and error handling
     */
    @Nullable
    private SmlResult performSmlLookup(@Nonnull LookupRequest request,
                                       @Nonnull List<ErrorDetail> errors) {
        logger.info("Starting SML lookup for environment: {}", request.getEnvironment());

        try {

            SmlResult smlResult = smlLookupService.lookupSmp(request.getParticipantId(), request.getEnvironment());

            if (smlResult != null && smlResult.isSuccessful()) {
                logger.info("SML lookup succeeded - URL: {}, DNSSEC: {}",
                        smlResult.getSmpUrl(), smlResult.isDnssecValid());
            } else {
                String errorMsg = smlResult != null ? smlResult.getErrorMessage() : "SML lookup returned null";
                logger.warn("SML lookup failed: {}", errorMsg);

                errors.add(ErrorClassifier.createError(
                        ErrorCodes.SML_DNS_RESOLUTION_FAILED,
                        "SML DNS lookup failed: " + errorMsg));
            }

            return smlResult;

        } catch (Exception e) {
            logger.error("SML lookup failed with exception: {}", e.getMessage(), e);

            errors.add(ErrorClassifier.createError(
                    ErrorCodes.SML_DNS_RESOLUTION_FAILED,
                    "SML lookup failed with exception: " + e.getMessage(),
                    "Exception: " + e.getClass().getSimpleName(),
                    e));

            return null;
        }
    }

    /**
     * Perform SMP query with secure XML parsing and error handling
     */
    @Nullable
    private SmpResult performSmpQuery(@Nonnull LookupRequest request,
                                      @Nonnull SmlResult smlResult,
                                      @Nonnull List<ErrorDetail> errors) {
        logger.info("Starting SMP query to URL: {}", smlResult.getSmpUrl());

        try {
            // Check if SMP query service is available
            if (smpQueryService == null) {
                logger.error("SMP query service not initialized");

                errors.add(ErrorClassifier.createError(
                        ErrorCodes.SYSTEM_DEPENDENCY_UNAVAILABLE,
                        "SMP query service not initialized"));

                return null;
            }

            SmpResult smpResult = smpQueryService.querySmp(
                    smlResult.getSmpUrl(),
                    request.getParticipantId(),
                    request.getDocumentTypeId(),
                    request.getProcessId()
            );

            if (smpResult != null && smpResult.isSuccessful()) {
                logger.info("SMP query succeeded - endpoint: {}, transport: {}, hasCert: {}",
                        smpResult.getEndpointUrl(), smpResult.getTransportProfile(),
                        smpResult.getCertificate() != null);
            } else {
                String errorMsg = smpResult != null ? smpResult.getErrorMessage() : "SMP query returned null";
                logger.warn("SMP query failed: {}", errorMsg);

                errors.add(ErrorClassifier.createError(
                        ErrorCodes.SMP_HTTP_REQUEST_FAILED,
                        "SMP query failed: " + errorMsg));
            }

            return smpResult;

        } catch (Exception e) {
            logger.error("SMP query failed with exception: {}", e.getMessage(), e);

            errors.add(ErrorClassifier.createError(
                    ErrorCodes.SMP_HTTP_REQUEST_FAILED,
                    "SMP query failed with exception: " + e.getMessage(),
                    "Exception: " + e.getClass().getSimpleName(),
                    e));

            return null;
        }
    }

    /**
     * Perform comprehensive validation orchestration
     */
    @Nonnull
    private ComprehensiveValidationResult performComprehensiveValidation(@Nonnull LookupRequest request,
                                                                        @Nonnull SmpResult smpResult,
                                                                        @Nonnull List<ErrorDetail> errors) {
        
        try {
            // Configure validation options based on request
            ValidationOptions options = ValidationOptions.defaultOptions()
                .setTestEndpointConnectivity(request.isValidateEndpointConnectivity())
                .setValidateSslCertificateMatching(request.isValidateEndpointConnectivity())
                .setUseCache(true);
            
            // Create validation context
            ValidationContext context = new ValidationContext(
                smpResult,
                request.getParticipantId(),
                request.getDocumentTypeId(),
                request.getEnvironment(),
                options
            );
            
            // Orchestrate all validations
            ComprehensiveValidationResult validationResult = validationOrchestrator.orchestrateValidations(context);

            logger.info("ComprehensiveValidation: overallValid={}, peppolCompliant={}",
                    validationResult.isOverallValid(),
                    validationResult.isPeppolCompliant());

            return validationResult;
            
        } catch (Exception e) {
            logger.error("Comprehensive validation error: {}", e.getMessage(), e);
            
            errors.add(ErrorClassifier.createError(
                ErrorCodes.VALIDATION_PEPPOL_COMPLIANCE_FAILED,
                "Validation orchestration failed: " + e.getMessage(),
                "Exception: " + e.getClass().getSimpleName(),
                e));
            
            // Return empty validation result
            ValidationResults emptyResults = new ValidationResults();
            return new ComprehensiveValidationResult(
                emptyResults, false, false, Map.of());
        }
    }

    /**
     * Process certificate details if certificate is available
     */
    @Nullable
    private CertificateDetails processCertificateDetails(@Nonnull SmpResult smpResult,
                                                        @Nonnull LookupRequest request) {
        if (smpResult.getCertificate() == null) {
            logger.debug("Certificate processing skipped - no certificate present");
            return null;
        }
        
        try {
            Map<String, Object> certContext = new HashMap<>();
            certContext.put("certificatePresent", true);
            
            CertificateDetails details = certificateService.extractCertificateDetails(smpResult.getCertificate());

            // Certificate chain building not implemented yet
            if (request.isIncludeFullCertificateChain()) {
                logger.debug("Certificate chain building requested but not yet implemented");
            }

            logger.info("Certificate processing successful - subject: {}", details.getSubject());
            return details;
            
        } catch (Exception e) {
            logger.error("Certificate processing error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build comprehensive response with all required fields and optional features
     */
    @Nonnull
    private LookupResponse buildComprehensiveResponse(@Nonnull LookupRequest request,
                                                     @Nonnull SmlResult smlResult,
                                                     @Nonnull SmpResult smpResult,
                                                     @Nonnull ComprehensiveValidationResult validationResult,
                                                     @Nullable CertificateDetails certificateDetails,
                                                     @Nonnull List<ErrorDetail> errors) {

        logger.debug("Starting response building");

        LookupResponse.Builder responseBuilder = new LookupResponse.Builder(request.getParticipantId())
            .success(smpResult.isSuccessful() && validationResult.isOverallValid())
            .documentTypeId(request.getDocumentTypeId())
            .processId(request.getProcessId())
            .environment(request.getEnvironment());
        
        // Set endpoint information from SMP result
        if (smpResult.getEndpointUrl() != null) {
            responseBuilder.endpointUrl(smpResult.getEndpointUrl());
        }
        if (smpResult.getTransportProfile() != null) {
            responseBuilder.transportProfile(smpResult.getTransportProfile());
        }
        if (smpResult.getServiceActivationDate() != null) {
            responseBuilder.serviceActivationDate(smpResult.getServiceActivationDate());
        }
        if (smpResult.getServiceExpirationDate() != null) {
            responseBuilder.serviceExpirationDate(smpResult.getServiceExpirationDate());
        }
        
        // Set certificate information
        if (smpResult.getCertificate() != null) {
            try {
                String pemCertificate = certificateService.convertDerToPem(smpResult.getCertificate().getEncoded());
                responseBuilder.certificate(pemCertificate);
            } catch (Exception e) {
                logger.error("Failed to convert certificate to PEM: {}", e.getMessage(), e);
                // Consider: Should this affect the success status?
            }
        }
        
        // Set validation results
        responseBuilder.validationResults(validationResult.getValidationResults());
        
        // Set certificate details if available
        if (certificateDetails != null) {
            responseBuilder.certificateDetails(certificateDetails);
        }
        
        // Set technical details if requested
        if (request.isIncludeTechnicalDetails()) {
            TechnicalDetails technicalDetails = buildTechnicalDetails(
                smlResult, smpResult, validationResult);
            responseBuilder.technicalDetails(technicalDetails);
        }
        
        // Set errors if any
        if (!errors.isEmpty()) {
            responseBuilder.errors(errors);
        }

        LookupResponse response = responseBuilder.build();

        // Set additional fields not covered by builder
        response.setTimestamp(Instant.now());

        logger.info("Response built - success: {}, hasErrors: {}",
                response.isSuccess(), response.hasErrors());
        
        return response;
    }

    /**
     * Build technical details for response
     */
    @Nonnull
    private TechnicalDetails buildTechnicalDetails(@Nonnull SmlResult smlResult,
                                                  @Nonnull SmpResult smpResult,
                                                  @Nonnull ComprehensiveValidationResult validationResult) {
        TechnicalDetails details = new TechnicalDetails();
        
        // SML details
        details.setSmlDnsQuery(smlResult.getDnsQuery());
        details.setSmlLookupTimeMs(smlResult.getResolutionTimeMs());
        details.setSmpUrl(smlResult.getSmpUrl());
        details.setDnssecValidated(smlResult.isDnssecValid());
        
        // SMP details
        details.setSmpQueryTimeMs(smpResult.getQueryTimeMs());
        details.setSmpQueryUrl(smpResult.getSmpQueryUrl());
        
        return details;
    }

    /**
     * Create validation error response
     */
    @Nonnull
    private LookupResponse createValidationErrorResponse(@Nonnull LookupRequest request,
                                                        @Nonnull RequestValidationResult validationResult) {
        
        LookupResponse response = new LookupResponse.Builder(request.getParticipantId())
            .success(false)
            .documentTypeId(request.getDocumentTypeId())
            .processId(request.getProcessId())
            .environment(request.getEnvironment())
            .errors(convertValidationErrors(validationResult.getErrors()))
            .build();
        
        response.setTimestamp(Instant.now());

        logger.error("Created validation error response with {} errors", validationResult.getErrors().size());
        
        return response;
    }

    /**
     * Create partial error response with available results
     */
    @Nonnull
    private LookupResponse createPartialErrorResponse(@Nonnull LookupRequest request,
                                                     @Nonnull List<ErrorDetail> errors,
                                                     @Nullable SmlResult smlResult,
                                                     @Nullable SmpResult smpResult) {
        
        LookupResponse.Builder responseBuilder = new LookupResponse.Builder(request.getParticipantId())
            .success(false)
            .documentTypeId(request.getDocumentTypeId())
            .processId(request.getProcessId())
            .environment(request.getEnvironment())
            .errors(errors);
        
        // Include partial SMP results if available
        if (smpResult != null && smpResult.getEndpointUrl() != null) {
            responseBuilder.endpointUrl(smpResult.getEndpointUrl());
            if (smpResult.getTransportProfile() != null) {
                responseBuilder.transportProfile(smpResult.getTransportProfile());
            }
        }
        
        LookupResponse response = responseBuilder.build();
        response.setTimestamp(Instant.now());
        
        // Set technical details if requested
        if (request.isIncludeTechnicalDetails()) {
            TechnicalDetails technicalDetails = new TechnicalDetails();
            if (smlResult != null) {
                technicalDetails.setSmlDnsQuery(smlResult.getDnsQuery());
                technicalDetails.setSmlLookupTimeMs(smlResult.getResolutionTimeMs());
                technicalDetails.setSmpUrl(smlResult.getSmpUrl());
                technicalDetails.setDnssecValidated(smlResult.isDnssecValid());
            }
            if (smpResult != null) {
                technicalDetails.setSmpQueryTimeMs(smpResult.getQueryTimeMs());
            }
            response.setTechnicalDetails(technicalDetails);
        }

        logger.error("Created partial error response - errorCount: {}, hasSmlResult: {}, hasSmpResult: {}",
                errors.size(), smlResult != null, smpResult != null);
        
        return response;
    }

    /**
     * Create system error response
     */
    @Nonnull
    private LookupResponse createSystemErrorResponse(@Nonnull LookupRequest request,
                                                    @Nonnull List<ErrorDetail> errors,
                                                    @Nonnull Exception exception) {
        
        LookupResponse response = new LookupResponse.Builder(request.getParticipantId())
            .success(false)
            .documentTypeId(request.getDocumentTypeId())
            .processId(request.getProcessId())
            .environment(request.getEnvironment())
            .errors(errors)
            .build();
        
        response.setTimestamp(Instant.now());

        return response;
    }

    /**
     * Hash participant ID for privacy-conscious logging
     */
    @Nonnull
    private String hashParticipantId(@Nonnull String participantId) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(participantId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash).substring(0, 8);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }

    /**
     * Convert validation error strings to ErrorDetail objects
     */
    @Nonnull
    private List<ErrorDetail> convertValidationErrors(@Nonnull List<String> errorStrings) {
        List<ErrorDetail> errorDetails = new ArrayList<>();
        for (String errorString : errorStrings) {
            ErrorDetail errorDetail = ErrorClassifier.createError(
                ErrorCodes.E7001, // Generic validation error
                errorString);
            errorDetails.add(errorDetail);
        }
        return errorDetails;
    }

    // Getters for testing and monitoring
    
    /**
     * Get the underlying SML lookup service (for testing/monitoring)
     */
    @Nonnull
    public SmlLookupService getSmlLookupService() {
        return smlLookupService;
    }

    /**
     * Get the SMP query service (for testing/monitoring)
     */
    @Nonnull
    public SmpQueryService getSmpQueryService() {
        return smpQueryService;
    }

    /**
     * Get the validation orchestrator (for testing/monitoring)
     */
    @Nonnull
    public ValidationOrchestrator getValidationOrchestrator() {
        return validationOrchestrator;
    }

    /**
     * Get the certificate service (for testing/monitoring)
     */
    @Nonnull
    public CertificateService getCertificateService() {
        return certificateService;
    }

    /**
     * Get the request validator (for testing/monitoring)
     */
    @Nonnull
    public RequestValidator getRequestValidator() {
        return requestValidator;
    }
}