package com.tca.peppol.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tca.peppol.model.response.ValidationResults;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.service.MetricsCollector;
import com.tca.peppol.service.MetricsCollectorFactory;
import com.tca.peppol.service.PeppolLookupService;
import com.tca.peppol.util.CorrelationIdUtils;
import com.tca.peppol.util.MetricsTimer;
import com.tca.peppol.util.StructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda handler for Peppol SML/SMP lookup operations.
 * 
 * This handler provides the entry point for AWS Lambda execution and handles:
 * - Request parsing and validation
 * - Service orchestration through PeppolLookupService
 * - Response formatting and error handling
 * - CloudWatch metrics and X-Ray tracing
 * - Proper HTTP status code mapping
 */
public class PeppolLookupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PeppolLookupHandler.class);
    
    private final ObjectMapper objectMapper;
    private final PeppolLookupService peppolLookupService;
    private final MetricsCollector metricsCollector;
    
    // CORS headers for API Gateway
    private static final Map<String, String> CORS_HEADERS = Map.of(
        "Access-Control-Allow-Origin", "*",
        "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
        "Access-Control-Allow-Methods", "GET,POST,OPTIONS",
        "Content-Type", "application/json"
    );

    /**
     * Default constructor - initializes services and JSON mapper
     */
    public PeppolLookupHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.metricsCollector = MetricsCollectorFactory.create(System.getenv("ENVIRONMENT"));
        this.peppolLookupService = new PeppolLookupService(metricsCollector);
        
        logger.info("PeppolLookupHandler initialized successfully with X-Ray tracing");
    }

    /**
     * Constructor for testing with custom service
     */
    public PeppolLookupHandler(PeppolLookupService peppolLookupService) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.metricsCollector = MetricsCollectorFactory.createNoOp();
        this.peppolLookupService = peppolLookupService;

        logger.info("PeppolLookupHandler initialized with custom service");
    }
    
    /**
     * Constructor for testing with custom service and metrics collector
     */
    public PeppolLookupHandler(PeppolLookupService peppolLookupService, MetricsCollector metricsCollector) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.metricsCollector = metricsCollector;
        this.peppolLookupService = peppolLookupService;
        logger.info("PeppolLookupHandler initialized with custom service and metrics collector");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String lambdaRequestId = context.getAwsRequestId();
        
        // Set up correlation IDs for structured logging
        String correlationId = CorrelationIdUtils.generateAndSetCorrelationId();
        CorrelationIdUtils.setLambdaRequestId(lambdaRequestId);
        
        try {
            // Log request start with structured logging
            Map<String, Object> requestDetails = new HashMap<>();
            requestDetails.put("httpMethod", input.getHttpMethod());
            requestDetails.put("path", input.getPath());
            requestDetails.put("userAgent", input.getHeaders() != null ? input.getHeaders().get("User-Agent") : null);
            
            StructuredLogger.logBusinessEvent("Lambda request started", requestDetails);

            // Handle CORS preflight requests
            if ("OPTIONS".equals(input.getHttpMethod())) {
                StructuredLogger.logBusinessEvent("CORS preflight request handled", Map.of("method", "OPTIONS"));
                return createResponse(200, null, CORS_HEADERS);
            }

            try {
                // Parse request
                LookupRequest lookupRequest = parseRequest(input, lambdaRequestId);
                if (lookupRequest == null) {
                    Map<String, Object> errorDetails = new HashMap<>();
                    errorDetails.put("errorType", "INVALID_REQUEST_FORMAT");
                    errorDetails.put("httpMethod", input.getHttpMethod());
                    
                    StructuredLogger.logError("Invalid request format", null, errorDetails);

                    return createErrorResponse(400, "Invalid request format", lambdaRequestId);
                }

                // Set request ID for correlation
                CorrelationIdUtils.setRequestId(lookupRequest.getRequestId());

                // Log business event for lookup start
                Map<String, Object> lookupDetails = new HashMap<>();
                lookupDetails.put("participantId", lookupRequest.getParticipantId());
                lookupDetails.put("documentTypeId", lookupRequest.getDocumentTypeId());
                lookupDetails.put("processId", lookupRequest.getProcessId());
                lookupDetails.put("environment", lookupRequest.getEnvironment());
                
                StructuredLogger.logBusinessEvent("Peppol lookup started", lookupDetails);

                // Perform lookup with performance tracking and metrics
                MetricsTimer timer = MetricsTimer.start(metricsCollector);
                LookupResponse lookupResponse = peppolLookupService.performLookup(lookupRequest);
                timer.stopAndRecordProcessingTime();
                long processingTime = timer.getElapsedTime();
                
                lookupResponse.setRequestId(lambdaRequestId);

                // Add validation results to X-Ray metadata
                if (lookupResponse.getValidationResults() != null) {
                    Map<String, Object> validationMetadata = createValidationMetadata(lookupResponse.getValidationResults());
                }

                // Log performance metrics
                Map<String, Object> perfDetails = new HashMap<>();
                perfDetails.put("participantId", lookupRequest.getParticipantId());
                perfDetails.put("environment", lookupRequest.getEnvironment());
                perfDetails.put("successful", lookupResponse.isSuccessful());
                
                StructuredLogger.logPerformanceMetric("Peppol lookup", processingTime, perfDetails);

                // Record success/failure metrics
                String errorCode = null;
                String errorCategory = null;
                
                if (lookupResponse.isSuccessful()) {
                    metricsCollector.recordSuccess();
                } else {
                    // Determine error category from response
                    errorCategory = determineErrorCategory(lookupResponse);
                    errorCode = determineErrorCode(lookupResponse);
                    metricsCollector.recordFailure(errorCategory);
                }
                

                // Record memory usage
                metricsCollector.recordMemoryUsage();
                
                // Add CloudWatch metrics
                addCloudWatchMetrics(context, lookupRequest, lookupResponse);

                // Return response
                int statusCode = lookupResponse.isSuccessful() ? 200 : 404;

                String responseBody = objectMapper.writeValueAsString(lookupResponse);
                
                // Log completion
                Map<String, Object> completionDetails = new HashMap<>();
                completionDetails.put("participantId", lookupRequest.getParticipantId());
                completionDetails.put("successful", lookupResponse.isSuccessful());
                completionDetails.put("statusCode", statusCode);
                completionDetails.put("processingTimeMs", processingTime);
                
                StructuredLogger.logBusinessEvent("Peppol lookup completed", completionDetails);
                
                return createResponse(statusCode, responseBody, CORS_HEADERS);

            } catch (Exception e) {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("errorType", "PROCESSING_ERROR");
                errorDetails.put("errorClass", e.getClass().getSimpleName());
                
                // Record failure metric
                metricsCollector.recordFailure("SYSTEM");

                
                StructuredLogger.logError("Unexpected error processing request", e, errorDetails);
                return createErrorResponse(500, "Internal server error: " + e.getMessage(), lambdaRequestId);
                
            } finally {
                AWSXRay.endSubsegment();
                // Flush metrics before ending
                metricsCollector.flushMetrics();
            }
            
        } finally {
            // Clean up correlation IDs
            CorrelationIdUtils.clearAll();
        }
    }

    /**
     * Parses the incoming API Gateway request into a LookupRequest
     */
    private LookupRequest parseRequest(APIGatewayProxyRequestEvent input, String requestId) {
        try {
            String body = input.getBody();
            if (body == null || body.trim().isEmpty()) {
                logger.warn("Empty request body for request: {}", requestId);
                return null;
            }

            LookupRequest request = objectMapper.readValue(body, LookupRequest.class);
            
            // Basic validation
            if (request.getParticipantId() == null || request.getParticipantId().trim().isEmpty()) {
                logger.warn("Missing participant ID in request: {}", requestId);
                return null;
            }
            
            if (request.getDocumentTypeId() == null || request.getDocumentTypeId().trim().isEmpty()) {
                logger.warn("Missing document type ID in request: {}", requestId);
                return null;
            }
            
            // Set default environment if not provided
            if (request.getEnvironment() == null || request.getEnvironment().trim().isEmpty()) {
                request.setEnvironment("production");
                logger.debug("Using default environment 'production' for request: {}", requestId);
            }

            logger.debug("Successfully parsed request: {} - Participant: {} - Document Type: {} - Environment: {}", 
                        requestId, request.getParticipantId(), request.getDocumentTypeId(), request.getEnvironment());
            
            return request;

        } catch (Exception e) {
            logger.error("Failed to parse request: {}", requestId, e);
            return null;
        }
    }

    /**
     * Determines the error category based on the lookup response
     */
    private String determineErrorCategory(LookupResponse response) {
        if (response.getErrors() == null || response.getErrors().isEmpty()) {
            return "UNKNOWN";
        }
        
        // Use the category from the first error
        var category = response.getErrors().get(0).getCategory();
        return category != null ? category.name() : "UNKNOWN";
    }
    
    /**
     * Determines the error code based on the lookup response
     */
    private String determineErrorCode(LookupResponse response) {
        if (response.getErrors() == null || response.getErrors().isEmpty()) {
            return null;
        }
        
        // Use the code from the first error
        return response.getErrors().get(0).getCode();
    }
    
    /**
     * Creates validation metadata for X-Ray tracing
     */
    private Map<String, Object> createValidationMetadata(ValidationResults validationResults) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Certificate validation
        metadata.put("certificateValid", validationResults.isCertificateValid());
        metadata.put("certificateNotExpired", validationResults.isCertificateNotExpired());
        metadata.put("certificateNotRevoked", validationResults.isCertificateNotRevoked());
        metadata.put("certificateFromOpenPeppol", validationResults.isCertificateFromOpenPeppol());
        metadata.put("certificateChainValid", validationResults.isCertificateChainValid());
        
        // XML signature validation
        metadata.put("smpSignatureValid", validationResults.isSmpSignatureValid());
        metadata.put("xmlCanonicalizationValid", validationResults.isXmlCanonicalizationValid());
        metadata.put("signatureAlgorithmValid", validationResults.isSignatureAlgorithmValid());
        
        // Endpoint validation
        metadata.put("endpointAccessible", validationResults.isEndpointAccessible());
        metadata.put("transportProfileSupported", validationResults.isTransportProfileSupported());
        metadata.put("serviceAvailable", validationResults.isServiceAvailable());
        
        // DNS validation
        metadata.put("smlDnsResolutionSuccessful", validationResults.isSmlDnsResolutionSuccessful());
        metadata.put("smpAccessible", validationResults.isSmpAccessible());
        
        // Overall compliance
        metadata.put("peppolCompliant", validationResults.isPeppolCompliant());
        
        return metadata;
    }
    
    /**
     * Adds CloudWatch custom metrics for monitoring
     */
    private void addCloudWatchMetrics(Context context, LookupRequest request, LookupResponse response) {
        try {
            // Add custom metrics here if needed
            // For now, we'll rely on CloudWatch Logs and X-Ray tracing
            logger.info("METRIC: ParticipantLookup - Success: {} - Time: {}ms - Environment: {}", 
                       response.isSuccessful(), response.getTotalProcessingTimeMs(), request.getEnvironment());
            
        } catch (Exception e) {
            logger.warn("Failed to add CloudWatch metrics", e);
        }
    }

    /**
     * Creates a standard API Gateway response
     */
    private APIGatewayProxyResponseEvent createResponse(int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);
        response.setHeaders(headers != null ? headers : CORS_HEADERS);
        return response;
    }

    /**
     * Creates an error response with proper formatting
     */
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorMessage, String requestId) {
        try {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("error", errorMessage);
            errorBody.put("requestId", requestId);
            errorBody.put("timestamp", java.time.Instant.now());
            
            String responseBody = objectMapper.writeValueAsString(errorBody);
            return createResponse(statusCode, responseBody, CORS_HEADERS);
            
        } catch (Exception e) {
            logger.error("Failed to create error response for request: {}", requestId, e);
            
            // Fallback to simple error response
            String fallbackBody = String.format(
                "{\"success\":false,\"error\":\"%s\",\"requestId\":\"%s\"}", 
                errorMessage.replace("\"", "\\\""), requestId);
            
            return createResponse(statusCode, fallbackBody, CORS_HEADERS);
        }
    }
}