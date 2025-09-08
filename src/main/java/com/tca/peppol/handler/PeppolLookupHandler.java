package com.tca.peppol.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tca.peppol.dagger.ApplicationComponent;
// import com.tca.peppol.dagger.DaggerApplicationComponent; // Will be available after Dagger generates the class
import com.tca.peppol.service.PeppolLookupService;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.model.response.ValidationResults;
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

    // Dagger component - created once and reused
    // private static final ApplicationComponent component = DaggerApplicationComponent.builder().build();
    private static final ApplicationComponent component = null; // Will be initialized after Dagger generates the class
    
    private final ObjectMapper objectMapper;
    private final PeppolLookupService peppolLookupService;
    
    // CORS headers for API Gateway
    private static final Map<String, String> CORS_HEADERS = Map.of(
        "Access-Control-Allow-Origin", "*",
        "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
        "Access-Control-Allow-Methods", "GET,POST,OPTIONS",
        "Content-Type", "application/json"
    );

    /**
     * Default constructor - gets dependencies from Dagger
     */
    public PeppolLookupHandler() {
        this.objectMapper = component.objectMapper();
        this.peppolLookupService = component.peppolLookupService();

        logger.info("PeppolLookupHandler initialized with Dagger dependency injection");
    }

    /**
     * Constructor for testing with custom service
     */
    public PeppolLookupHandler(PeppolLookupService peppolLookupService) {
        this.objectMapper = createObjectMapper();
        this.peppolLookupService = peppolLookupService;

        logger.info("PeppolLookupHandler initialized with custom service");
    }

    /**
     * Creates and configures the ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String lambdaRequestId = context.getAwsRequestId();

            // Log request start with structured logging
            logger.info("Lambda request started: method={}, path={}, userAgent={}",
                    input.getHttpMethod(),
                    input.getPath(),
                    input.getHeaders() != null ? input.getHeaders().get("User-Agent") : "unknown");

            // Handle CORS preflight requests
            if ("OPTIONS".equals(input.getHttpMethod())) {
                logger.info("CORS preflight request handled: method=OPTIONS");
                return createResponse(200, null, CORS_HEADERS);
            }

            try {
                // Parse request
                LookupRequest lookupRequest = parseRequest(input, lambdaRequestId);
                if (lookupRequest == null) {
                    logger.error("Invalid request format: errorType=INVALID_REQUEST_FORMAT, httpMethod={}", input.getHttpMethod());
                    return createErrorResponse(400, "Invalid request format", lambdaRequestId);
                }

                logger.info("Peppol lookup started: participantId={}, documentTypeId={}, processId={}, environment={}",
                        lookupRequest.getParticipantId(),
                        lookupRequest.getDocumentTypeId(),
                        lookupRequest.getProcessId(),
                        lookupRequest.getEnvironment());

                LookupResponse lookupResponse = peppolLookupService.performLookup(lookupRequest);
                
                lookupResponse.setRequestId(lambdaRequestId);

                // Return response
                int statusCode = lookupResponse.isSuccessful() ? 200 : 404;

                String responseBody = objectMapper.writeValueAsString(lookupResponse);

                logger.info("Peppol lookup completed: participantId={}, successful={}, statusCode={}",
                        lookupRequest.getParticipantId(),
                        lookupResponse.isSuccessful(),
                        statusCode);
                
                return createResponse(statusCode, responseBody, CORS_HEADERS);

            } catch (Exception e) {
                logger.error("Unexpected error processing request: errorType=PROCESSING_ERROR, errorClass={}",
                        e.getClass().getSimpleName(), e);
                return createErrorResponse(500, "Internal server error: " + e.getMessage(), lambdaRequestId);
                
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
                request.setEnvironment("dev");
                logger.debug("Using default environment 'dev' for request: {}", requestId);
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