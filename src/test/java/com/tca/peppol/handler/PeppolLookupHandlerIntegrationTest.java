package com.tca.peppol.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PeppolLookupHandler AWS Lambda function
 * These tests use the real implementation without mocking to avoid Java version compatibility issues
 */
class PeppolLookupHandlerIntegrationTest {

    private PeppolLookupHandler handler;
    private ObjectMapper objectMapper;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        handler = new PeppolLookupHandler();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testContext = new TestContext();
    }

    @Test
    void testHandleRequest_OptionsRequest() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Methods");
    }

    @Test
    void testHandleRequest_PostRequestEmptyBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setBody("");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
    }

    @Test
    void testHandleRequest_PostRequestNullBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setBody(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
    }

    @Test
    void testHandleRequest_PostRequestInvalidJson() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setBody("invalid json");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
    }

    @Test
    void testHandleRequest_PostRequestMissingParticipantId() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        
        Map<String, Object> requestBody = Map.of(
            "documentTypeId", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"
        );
        
        request.setBody(objectMapper.writeValueAsString(requestBody));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
    }

    @Test
    void testHandleRequest_PostRequestMissingDocumentTypeId() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        
        Map<String, Object> requestBody = Map.of(
            "participantId", "9915:test"
        );
        
        request.setBody(objectMapper.writeValueAsString(requestBody));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
    }

    @Test
    void testDefaultConstructor() {
        // Test that default constructor works
        PeppolLookupHandler defaultHandler = new PeppolLookupHandler();
        assertThat(defaultHandler).isNotNull();
    }

    /**
     * Simple test implementation of Context for testing
     */
    private static class TestContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "test-request-id-123";
        }

        @Override
        public String getLogGroupName() {
            return "test-log-group";
        }

        @Override
        public String getLogStreamName() {
            return "test-log-stream";
        }

        @Override
        public String getFunctionName() {
            return "test-function";
        }

        @Override
        public String getFunctionVersion() {
            return "1.0";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:us-east-1:123456789012:function:test-function";
        }

        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 30000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 512;
        }

        @Override
        public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() {
            return new com.amazonaws.services.lambda.runtime.LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println(message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println(new String(message));
                }
            };
        }
    }
}