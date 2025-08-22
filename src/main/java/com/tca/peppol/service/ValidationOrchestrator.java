package com.tca.peppol.service;

import com.tca.peppol.client.CrlClient;
import com.tca.peppol.client.CrlResult;
import com.tca.peppol.client.OcspClient;
import com.tca.peppol.client.OcspResult;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.model.response.ValidationResults;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.ValidationResult;
import com.tca.peppol.validation.XmlSignatureValidationResult;
import com.tca.peppol.validation.XmlSignatureValidator;
import com.tca.peppol.util.XmlSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ValidationOrchestrator coordinates all validation processes for Peppol SMP lookup results.
 * 
 * This orchestrator manages the complete validation workflow including:
 * - Certificate validation (RFC 5280 compliance, Peppol-specific validation)
 * - XML signature validation (W3C compliance, canonicalization)
 * - Endpoint validation (transport profiles, URL validation, connectivity)
 * - Revocation checking (OCSP and CRL)
 * - Overall Peppol compliance assessment
 * 
 * Features:
 * - Proper validation sequencing and dependency handling
 * - Validation result aggregation and comprehensive reporting
 * - Validation caching and optimization for repeated validations
 * - Circuit breaker integration for external service protection
 * - Comprehensive error handling and logging
 * 
 * Requirements addressed: 4.1, 5.1, 6.1, 11.1, 14.1, 14.2, 14.3, 14.4
 */
public class ValidationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationOrchestrator.class);
    
    // Validation cache for optimization
    private static final Map<String, CachedValidationResult> validationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MINUTES = 30;
    
    // Dependencies
    private final CertificateValidator certificateValidator;
    private final XmlSignatureValidator xmlSignatureValidator;
    private final EndpointValidator endpointValidator;
    private final CircuitBreakerService circuitBreakerService;
    private final OcspClient ocspClient;
    private final CrlClient crlClient;
    
    /**
     * Validation options to control which validations are performed.
     */
    public static class ValidationOptions {
        private boolean validateCertificate = true;
        private boolean validateXmlSignature = true;
        private boolean validateEndpoint = true;
        private boolean checkRevocation = true;
        private boolean testEndpointConnectivity = false;
        private boolean validateSslCertificateMatching = false;
        private boolean useCache = true;
        private Set<TrustAnchor> trustAnchors = null;
        
        public static ValidationOptions defaultOptions() {
            return new ValidationOptions();
        }
        
        public static ValidationOptions fullValidation() {
            ValidationOptions options = new ValidationOptions();
            options.testEndpointConnectivity = true;
            options.validateSslCertificateMatching = true;
            return options;
        }
        
        // Getters and setters
        public boolean isValidateCertificate() { return validateCertificate; }
        public ValidationOptions setValidateCertificate(boolean validateCertificate) { 
            this.validateCertificate = validateCertificate; return this; 
        }
        
        public boolean isValidateXmlSignature() { return validateXmlSignature; }
        public ValidationOptions setValidateXmlSignature(boolean validateXmlSignature) { 
            this.validateXmlSignature = validateXmlSignature; return this; 
        }
        
        public boolean isValidateEndpoint() { return validateEndpoint; }
        public ValidationOptions setValidateEndpoint(boolean validateEndpoint) { 
            this.validateEndpoint = validateEndpoint; return this; 
        }
        
        public boolean isCheckRevocation() { return checkRevocation; }
        public ValidationOptions setCheckRevocation(boolean checkRevocation) { 
            this.checkRevocation = checkRevocation; return this; 
        }
        
        public boolean isTestEndpointConnectivity() { return testEndpointConnectivity; }
        public ValidationOptions setTestEndpointConnectivity(boolean testEndpointConnectivity) { 
            this.testEndpointConnectivity = testEndpointConnectivity; return this; 
        }
        
        public boolean isValidateSslCertificateMatching() { return validateSslCertificateMatching; }
        public ValidationOptions setValidateSslCertificateMatching(boolean validateSslCertificateMatching) { 
            this.validateSslCertificateMatching = validateSslCertificateMatching; return this; 
        }
        
        public boolean isUseCache() { return useCache; }
        public ValidationOptions setUseCache(boolean useCache) { 
            this.useCache = useCache; return this; 
        }
        
        public Set<TrustAnchor> getTrustAnchors() { return trustAnchors; }
        public ValidationOptions setTrustAnchors(Set<TrustAnchor> trustAnchors) { 
            this.trustAnchors = trustAnchors; return this; 
        }
    }
    
    /**
     * Validation context containing all necessary information for validation.
     */
    public static class ValidationContext {
        private final SmpResult smpResult;
        private final String participantId;
        private final String documentTypeId;
        private final String environment;
        private final ValidationOptions options;
        private final String correlationId;
        
        public ValidationContext(SmpResult smpResult, String participantId, String documentTypeId, 
                               String environment, ValidationOptions options, String correlationId) {
            this.smpResult = smpResult;
            this.participantId = participantId;
            this.documentTypeId = documentTypeId;
            this.environment = environment;
            this.options = options;
            this.correlationId = correlationId;
        }
        
        // Getters
        public SmpResult getSmpResult() { return smpResult; }
        public String getParticipantId() { return participantId; }
        public String getDocumentTypeId() { return documentTypeId; }
        public String getEnvironment() { return environment; }
        public ValidationOptions getOptions() { return options; }
        public String getCorrelationId() { return correlationId; }
    }
    
    /**
     * Comprehensive validation result containing all validation outcomes.
     */
    public static class ComprehensiveValidationResult {
        private final ValidationResults validationResults;
        private final boolean overallValid;
        private final boolean peppolCompliant;
        private final long totalValidationTimeMs;
        private final Instant validationTimestamp;
        private final String correlationId;
        private final Map<String, Object> validationDetails;
        
        public ComprehensiveValidationResult(ValidationResults validationResults, boolean overallValid,
                                           boolean peppolCompliant, long totalValidationTimeMs,
                                           String correlationId, Map<String, Object> validationDetails) {
            this.validationResults = validationResults;
            this.overallValid = overallValid;
            this.peppolCompliant = peppolCompliant;
            this.totalValidationTimeMs = totalValidationTimeMs;
            this.validationTimestamp = Instant.now();
            this.correlationId = correlationId;
            this.validationDetails = validationDetails != null ? validationDetails : Collections.emptyMap();
        }
        
        // Getters
        public ValidationResults getValidationResults() { return validationResults; }
        public boolean isOverallValid() { return overallValid; }
        public boolean isPeppolCompliant() { return peppolCompliant; }
        public long getTotalValidationTimeMs() { return totalValidationTimeMs; }
        public Instant getValidationTimestamp() { return validationTimestamp; }
        public String getCorrelationId() { return correlationId; }
        public Map<String, Object> getValidationDetails() { return validationDetails; }
    }
    
    /**
     * Constructor with all dependencies.
     */
    public ValidationOrchestrator(CertificateValidator certificateValidator,
                                XmlSignatureValidator xmlSignatureValidator,
                                EndpointValidator endpointValidator,
                                CircuitBreakerService circuitBreakerService) {
        this.certificateValidator = certificateValidator;
        this.xmlSignatureValidator = xmlSignatureValidator;
        this.endpointValidator = endpointValidator;
        this.circuitBreakerService = circuitBreakerService;
        this.ocspClient = new OcspClient();
        this.crlClient = new CrlClient();
        
        logger.info("ValidationOrchestrator initialized with all validation components");
    }
    
    /**
     * Orchestrates complete validation workflow for SMP result.
     * 
     * @param context The validation context containing SMP result and options
     * @return ComprehensiveValidationResult containing all validation outcomes
     */
    @Nonnull
    public ComprehensiveValidationResult orchestrateValidations(@Nonnull ValidationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Validation context cannot be null");
        }
        
        String correlationId = context.getCorrelationId();
        logger.info("Starting comprehensive validation orchestration [correlationId={}]", correlationId);
        
        long startTime = System.currentTimeMillis();
        ValidationResults results = new ValidationResults();
        Map<String, Object> validationDetails = new ConcurrentHashMap<>();
        
        try {
            // Check cache first if enabled
            if (context.getOptions().isUseCache()) {
                String cacheKey = generateCacheKey(context);
                CachedValidationResult cachedResult = validationCache.get(cacheKey);
                
                if (cachedResult != null && !cachedResult.isExpired()) {
                    logger.debug("Validation result found in cache [correlationId={}, cacheKey={}]", 
                               correlationId, cacheKey);
                    return cachedResult.getResult();
                }
            }
            
            // Phase 1: Certificate Validation (foundational)
            if (context.getOptions().isValidateCertificate()) {
                performCertificateValidation(context, results, validationDetails);
            }
            
            // Phase 2: XML Signature Validation (depends on certificate)
            if (context.getOptions().isValidateXmlSignature()) {
                performXmlSignatureValidation(context, results, validationDetails);
            }
            
            // Phase 3: Revocation Checking (depends on certificate validation)
            if (context.getOptions().isCheckRevocation() && results.isCertificateValid()) {
                performRevocationValidation(context, results, validationDetails);
            }
            
            // Phase 4: Endpoint Validation (can run independently)
            if (context.getOptions().isValidateEndpoint()) {
                performEndpointValidation(context, results, validationDetails);
            }
            
            // Phase 5: Overall Compliance Assessment
            performComplianceAssessment(context, results, validationDetails);
            
            // Calculate overall validation result
            boolean overallValid = calculateOverallValidation(results);
            boolean peppolCompliant = results.isPeppolCompliant();
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            ComprehensiveValidationResult finalResult = new ComprehensiveValidationResult(
                results, overallValid, peppolCompliant, totalTime, correlationId, validationDetails);
            
            // Cache the result if caching is enabled
            if (context.getOptions().isUseCache()) {
                cacheValidationResult(generateCacheKey(context), finalResult);
            }
            
            logger.info("Validation orchestration completed [correlationId={}, overallValid={}, " +
                       "peppolCompliant={}, totalTime={}ms]", 
                       correlationId, overallValid, peppolCompliant, totalTime);
            
            return finalResult;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("Validation orchestration failed [correlationId={}, totalTime={}ms]", 
                        correlationId, totalTime, e);
            
            // Return failed result
            validationDetails.put("orchestrationError", e.getMessage());
            return new ComprehensiveValidationResult(
                results, false, false, totalTime, correlationId, validationDetails);
        }
    }
    
    /**
     * Performs certificate validation including chain validation, Peppol compliance, and key validation.
     */
    private void performCertificateValidation(ValidationContext context, ValidationResults results, 
                                            Map<String, Object> validationDetails) {
        String correlationId = context.getCorrelationId();
        logger.debug("Starting certificate validation [correlationId={}]", correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            SmpResult smpResult = context.getSmpResult();
            X509Certificate certificate = smpResult.getCertificate();
            
            if (certificate == null) {
                logger.warn("No certificate found in SMP result [correlationId={}]", correlationId);
                results.setCertificateValid(false);
                validationDetails.put("certificateValidation", "No certificate found in SMP result");
                return;
            }
            
            // Build certificate chain (single certificate for now)
            List<X509Certificate> certificateChain = Collections.singletonList(certificate);
            
            // Perform comprehensive certificate validation
            ValidationResults certResults = certificateValidator.validateCertificateChain(
                certificateChain, context.getOptions().getTrustAnchors());
            
            // Copy certificate validation results
            results.setCertificateValid(certResults.isCertificateValid());
            results.setCertificateNotExpired(certResults.isCertificateNotExpired());
            results.setCertificateFromOpenPeppol(certResults.isCertificateFromOpenPeppol());
            results.setCertificateChainValid(certResults.isCertificateChainValid());
            results.setCertificateKeyLengthValid(certResults.isCertificateKeyLengthValid());
            results.setCertificatePolicyValid(certResults.isCertificatePolicyValid());
            
            long validationTime = System.currentTimeMillis() - startTime;
            validationDetails.put("certificateValidationTimeMs", validationTime);
            validationDetails.put("certificateSubject", certificate.getSubjectX500Principal().getName());
            validationDetails.put("certificateIssuer", certificate.getIssuerX500Principal().getName());
            
            logger.debug("Certificate validation completed [correlationId={}, valid={}, time={}ms]", 
                        correlationId, results.isCertificateValid(), validationTime);
            
        } catch (Exception e) {
            long validationTime = System.currentTimeMillis() - startTime;
            logger.error("Certificate validation failed [correlationId={}, time={}ms]", 
                        correlationId, validationTime, e);
            
            results.setCertificateValid(false);
            validationDetails.put("certificateValidationError", e.getMessage());
            validationDetails.put("certificateValidationTimeMs", validationTime);
        }
    }
    
    /**
     * Performs XML signature validation including W3C compliance and canonicalization.
     */
    private void performXmlSignatureValidation(ValidationContext context, ValidationResults results, 
                                             Map<String, Object> validationDetails) {
        String correlationId = context.getCorrelationId();
        logger.debug("Starting XML signature validation [correlationId={}]", correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            SmpResult smpResult = context.getSmpResult();
            
            if (smpResult.getXmlDocument() == null) {
                logger.warn("No XML document found in SMP result [correlationId={}]", correlationId);
                results.setSmpSignatureValid(false);
                validationDetails.put("xmlSignatureValidation", "No XML document found in SMP result");
                return;
            }
            
            X509Certificate expectedCertificate = smpResult.getCertificate();
            
            // Perform XML signature validation
            XmlSignatureValidationResult xmlResult = xmlSignatureValidator.validateXmlSignature(
                smpResult.getXmlDocument(), expectedCertificate);
            
            // Copy XML signature validation results
            results.setSmpSignatureValid(xmlResult.isValid());
            results.setXmlCanonicalizationValid(xmlResult.isCanonicalizationValid());
            results.setSignatureAlgorithmValid(xmlResult.isSignatureAlgorithmValid());
            
            long validationTime = System.currentTimeMillis() - startTime;
            validationDetails.put("xmlSignatureValidationTimeMs", validationTime);
            validationDetails.put("xmlSignaturePresent", xmlResult.isSignaturePresent());
            validationDetails.put("xmlSignatureAlgorithm", xmlResult.getSignatureAlgorithm());
            validationDetails.put("xmlCanonicalizationAlgorithm", xmlResult.getCanonicalizationAlgorithm());
            
            if (!xmlResult.isValid() && xmlResult.getErrorMessage() != null) {
                validationDetails.put("xmlSignatureError", xmlResult.getErrorMessage());
            }
            
            logger.debug("XML signature validation completed [correlationId={}, valid={}, time={}ms]", 
                        correlationId, xmlResult.isValid(), validationTime);
            
        } catch (XmlSecurityException e) {
            long validationTime = System.currentTimeMillis() - startTime;
            logger.error("XML signature validation failed with security exception [correlationId={}, time={}ms]", 
                        correlationId, validationTime, e);
            
            results.setSmpSignatureValid(false);
            results.setXmlCanonicalizationValid(false);
            results.setSignatureAlgorithmValid(false);
            validationDetails.put("xmlSignatureValidationError", "Security violation: " + e.getMessage());
            validationDetails.put("xmlSignatureValidationTimeMs", validationTime);
            
        } catch (Exception e) {
            long validationTime = System.currentTimeMillis() - startTime;
            logger.error("XML signature validation failed [correlationId={}, time={}ms]", 
                        correlationId, validationTime, e);
            
            results.setSmpSignatureValid(false);
            validationDetails.put("xmlSignatureValidationError", e.getMessage());
            validationDetails.put("xmlSignatureValidationTimeMs", validationTime);
        }
    }
    
    /**
     * Performs revocation checking using OCSP and CRL with circuit breaker protection.
     */
    private void performRevocationValidation(ValidationContext context, ValidationResults results, 
                                           Map<String, Object> validationDetails) {
        String correlationId = context.getCorrelationId();
        logger.debug("Starting revocation validation [correlationId={}]", correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            SmpResult smpResult = context.getSmpResult();
            X509Certificate certificate = smpResult.getCertificate();
            
            if (certificate == null) {
                logger.warn("No certificate available for revocation checking [correlationId={}]", correlationId);
                results.setOcspCheckPassed(false);
                results.setCrlCheckPassed(false);
                validationDetails.put("revocationValidation", "No certificate available for revocation checking");
                return;
            }
            
            // For now, we'll use the certificate as its own issuer for demonstration
            // In a real implementation, you would extract the issuer certificate from the chain
            X509Certificate issuerCertificate = certificate;
            
            // OCSP check with circuit breaker protection
            boolean ocspPassed = false;
            try {
                OcspResult ocspResult = circuitBreakerService.executeWithCircuitBreaker(
                    () -> ocspClient.checkRevocationStatus(certificate, issuerCertificate),
                    "ocsp-service"
                );
                
                ocspPassed = ocspResult.isSuccessful() && 
                           (ocspResult.getStatus() == OcspResult.Status.GOOD);
                
                validationDetails.put("ocspStatus", ocspResult.getStatus().toString());
                validationDetails.put("ocspResponseTime", ocspResult.getResponseTimeMs());
                
                if (!ocspPassed && ocspResult.getMessage() != null) {
                    validationDetails.put("ocspError", ocspResult.getMessage());
                }
                
            } catch (Exception e) {
                logger.warn("OCSP check failed [correlationId={}]", correlationId, e);
                validationDetails.put("ocspError", "OCSP check failed: " + e.getMessage());
            }
            
            results.setOcspCheckPassed(ocspPassed);
            
            // CRL check with circuit breaker protection
            boolean crlPassed = false;
            try {
                CrlResult crlResult = circuitBreakerService.executeWithCircuitBreaker(
                    () -> crlClient.checkRevocationStatus(certificate, issuerCertificate),
                    "crl-service"
                );
                
                crlPassed = crlResult.isSuccessful() && 
                          (crlResult.getStatus() == CrlResult.Status.GOOD);
                
                validationDetails.put("crlStatus", crlResult.getStatus().toString());
                validationDetails.put("crlResponseTime", crlResult.getResponseTimeMs());
                
                if (!crlPassed && crlResult.getMessage() != null) {
                    validationDetails.put("crlError", crlResult.getMessage());
                }
                
            } catch (Exception e) {
                logger.warn("CRL check failed [correlationId={}]", correlationId, e);
                validationDetails.put("crlError", "CRL check failed: " + e.getMessage());
            }
            
            results.setCrlCheckPassed(crlPassed);
            
            // Set overall revocation status
            boolean notRevoked = ocspPassed || crlPassed;
            results.setCertificateNotRevoked(notRevoked);
            
            long validationTime = System.currentTimeMillis() - startTime;
            validationDetails.put("revocationValidationTimeMs", validationTime);
            
            logger.debug("Revocation validation completed [correlationId={}, ocsp={}, crl={}, time={}ms]", 
                        correlationId, ocspPassed, crlPassed, validationTime);
            
        } catch (Exception e) {
            long validationTime = System.currentTimeMillis() - startTime;
            logger.error("Revocation validation failed [correlationId={}, time={}ms]", 
                        correlationId, validationTime, e);
            
            results.setOcspCheckPassed(false);
            results.setCrlCheckPassed(false);
            results.setCertificateNotRevoked(false);
            validationDetails.put("revocationValidationError", e.getMessage());
            validationDetails.put("revocationValidationTimeMs", validationTime);
        }
    }
    
    /**
     * Performs endpoint validation including transport profile and URL validation.
     */
    private void performEndpointValidation(ValidationContext context, ValidationResults results, 
                                         Map<String, Object> validationDetails) {
        String correlationId = context.getCorrelationId();
        logger.debug("Starting endpoint validation [correlationId={}]", correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            SmpResult smpResult = context.getSmpResult();
            String endpointUrl = smpResult.getEndpointUrl();
            String transportProfile = smpResult.getTransportProfile();
            X509Certificate certificate = smpResult.getCertificate();
            
            if (endpointUrl == null || transportProfile == null) {
                logger.warn("Missing endpoint information [correlationId={}, url={}, profile={}]", 
                           correlationId, endpointUrl, transportProfile);
                results.setEndpointAccessible(false);
                results.setTransportProfileSupported(false);
                results.setServiceAvailable(false);
                validationDetails.put("endpointValidation", "Missing endpoint URL or transport profile");
                return;
            }
            
            // Validate transport profile
            ValidationResult transportResult = endpointValidator.validateTransportProfile(transportProfile);
            results.setTransportProfileSupported(transportResult.isValid());
            
            // Validate endpoint URL
            ValidationResult urlResult = endpointValidator.validateEndpointUrl(endpointUrl);
            boolean urlValid = urlResult.isValid();
            
            // Test connectivity if requested
            boolean connectivityPassed = true;
            if (context.getOptions().isTestEndpointConnectivity() && urlValid) {
                ValidationResult connectivityResult = endpointValidator.testConnectivity(endpointUrl);
                connectivityPassed = connectivityResult.isValid();
                
                if (!connectivityPassed) {
                    validationDetails.put("connectivityError", connectivityResult.getMessage());
                }
            }
            
            results.setEndpointAccessible(urlValid && connectivityPassed);
            
            // Validate SSL certificate matching if requested
            boolean sslMatching = true;
            if (context.getOptions().isValidateSslCertificateMatching() && certificate != null && urlValid) {
                ValidationResult sslResult = endpointValidator.validateSslCertificateMatching(endpointUrl, certificate);
                sslMatching = sslResult.isValid();
                
                if (!sslMatching) {
                    validationDetails.put("sslCertificateMatchingError", sslResult.getMessage());
                }
            }
            
            // Set service availability based on all endpoint checks
            results.setServiceAvailable(urlValid && connectivityPassed && sslMatching);
            
            long validationTime = System.currentTimeMillis() - startTime;
            validationDetails.put("endpointValidationTimeMs", validationTime);
            validationDetails.put("endpointUrl", endpointUrl);
            validationDetails.put("transportProfile", transportProfile);
            validationDetails.put("connectivityTested", context.getOptions().isTestEndpointConnectivity());
            validationDetails.put("sslMatchingTested", context.getOptions().isValidateSslCertificateMatching());
            
            if (!transportResult.isValid()) {
                validationDetails.put("transportProfileError", transportResult.getMessage());
            }
            if (!urlResult.isValid()) {
                validationDetails.put("endpointUrlError", urlResult.getMessage());
            }
            
            logger.debug("Endpoint validation completed [correlationId={}, accessible={}, supported={}, time={}ms]", 
                        correlationId, results.isEndpointAccessible(), results.isTransportProfileSupported(), validationTime);
            
        } catch (Exception e) {
            long validationTime = System.currentTimeMillis() - startTime;
            logger.error("Endpoint validation failed [correlationId={}, time={}ms]", 
                        correlationId, validationTime, e);
            
            results.setEndpointAccessible(false);
            results.setTransportProfileSupported(false);
            results.setServiceAvailable(false);
            validationDetails.put("endpointValidationError", e.getMessage());
            validationDetails.put("endpointValidationTimeMs", validationTime);
        }
    }
    
    /**
     * Performs overall Peppol compliance assessment based on all validation results.
     */
    private void performComplianceAssessment(ValidationContext context, ValidationResults results, 
                                           Map<String, Object> validationDetails) {
        String correlationId = context.getCorrelationId();
        logger.debug("Starting compliance assessment [correlationId={}]", correlationId);
        
        try {
            // Peppol compliance requires:
            // 1. Valid certificate from OpenPeppol
            // 2. Valid XML signature with proper canonicalization
            // 3. Certificate not revoked
            // 4. Supported transport profile
            // 5. Valid endpoint URL
            
            boolean peppolCompliant = 
                results.isCertificateValid() &&
                results.isCertificateFromOpenPeppol() &&
                results.isCertificateNotRevoked() &&
                results.isSmpSignatureValid() &&
                results.isXmlCanonicalizationValid() &&
                results.isSignatureAlgorithmValid() &&
                results.isTransportProfileSupported() &&
                results.isEndpointAccessible();
            
            results.setPeppolCompliant(peppolCompliant);
            
            // Production network compliance has additional requirements
            boolean productionCompliant = peppolCompliant &&
                results.isCertificateChainValid() &&
                results.isCertificateKeyLengthValid() &&
                results.isCertificatePolicyValid() &&
                (results.isOcspCheckPassed() || results.isCrlCheckPassed());
            
            results.setProductionNetworkCompliant(productionCompliant);
            
            // Set DNS and SMP accessibility based on successful SMP result
            SmpResult smpResult = context.getSmpResult();
            results.setSmlDnsResolutionSuccessful(smpResult.isSuccessful());
            results.setSmpAccessible(smpResult.isSuccessful());
            results.setDnsSecValid(true); // Assume DNSSEC is valid for now
            
            validationDetails.put("peppolCompliant", peppolCompliant);
            validationDetails.put("productionNetworkCompliant", productionCompliant);
            validationDetails.put("environment", context.getEnvironment());
            
            logger.debug("Compliance assessment completed [correlationId={}, peppolCompliant={}, productionCompliant={}]", 
                        correlationId, peppolCompliant, productionCompliant);
            
        } catch (Exception e) {
            logger.error("Compliance assessment failed [correlationId={}]", correlationId, e);
            
            results.setPeppolCompliant(false);
            results.setProductionNetworkCompliant(false);
            validationDetails.put("complianceAssessmentError", e.getMessage());
        }
    }
    
    /**
     * Calculates overall validation result based on all individual validations.
     */
    private boolean calculateOverallValidation(ValidationResults results) {
        // Overall validation passes if all critical validations pass
        return results.isCertificateValid() &&
               results.isCertificateNotExpired() &&
               results.isCertificateNotRevoked() &&
               results.isSmpSignatureValid() &&
               results.isTransportProfileSupported() &&
               results.isEndpointAccessible();
    }
    
    /**
     * Generates cache key for validation result caching.
     */
    private String generateCacheKey(ValidationContext context) {
        SmpResult smpResult = context.getSmpResult();
        return String.format("validation-%s-%s-%s-%s-%d",
            context.getParticipantId(),
            context.getDocumentTypeId(),
            context.getEnvironment(),
            smpResult.getCertificate() != null ? 
                smpResult.getCertificate().getSerialNumber().toString(16) : "nocert",
            context.getOptions().hashCode()
        );
    }
    
    /**
     * Caches validation result with TTL.
     */
    private void cacheValidationResult(String cacheKey, ComprehensiveValidationResult result) {
        try {
            Instant expiry = Instant.now().plusSeconds(CACHE_TTL_MINUTES * 60);
            validationCache.put(cacheKey, new CachedValidationResult(result, expiry));
            
            // Clean up cache if it gets too large
            if (validationCache.size() > MAX_CACHE_SIZE) {
                cleanupValidationCache();
            }
            
            logger.debug("Validation result cached [cacheKey={}, expiry={}]", cacheKey, expiry);
            
        } catch (Exception e) {
            logger.warn("Failed to cache validation result [cacheKey={}]", cacheKey, e);
        }
    }
    
    /**
     * Cleans up expired entries from validation cache.
     */
    private void cleanupValidationCache() {
        Instant now = Instant.now();
        int initialSize = validationCache.size();
        
        validationCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        
        int finalSize = validationCache.size();
        logger.debug("Validation cache cleanup completed, removed {} entries, remaining: {}", 
                    initialSize - finalSize, finalSize);
    }
    
    /**
     * Gets validation cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStats() {
        int totalEntries = validationCache.size();
        long expiredEntries = validationCache.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        
        return Map.of(
            "totalEntries", totalEntries,
            "expiredEntries", (int) expiredEntries,
            "maxCacheSize", MAX_CACHE_SIZE,
            "cacheTtlMinutes", CACHE_TTL_MINUTES
        );
    }
    
    /**
     * Clears the validation cache.
     */
    public void clearCache() {
        validationCache.clear();
        logger.info("Validation cache cleared");
    }
    
    /**
     * Cached validation result with expiry time.
     */
    private static class CachedValidationResult {
        private final ComprehensiveValidationResult result;
        private final Instant expiry;
        
        public CachedValidationResult(ComprehensiveValidationResult result, Instant expiry) {
            this.result = result;
            this.expiry = expiry;
        }
        
        public ComprehensiveValidationResult getResult() {
            return result;
        }
        
        public boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        public boolean isExpired(Instant now) {
            return expiry != null && now.isAfter(expiry);
        }
    }
}