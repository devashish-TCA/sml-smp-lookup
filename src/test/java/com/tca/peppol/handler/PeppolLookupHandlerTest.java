package com.tca.peppol.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.service.PeppolLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PeppolLookupHandler AWS Lambda function
 */
@ExtendWith(MockitoExtension.class)
class PeppolLookupHandlerTest {

    @Mock
    private PeppolLookupService mockPeppolLookupService;

    @Mock
    private Context mockContext;

    private PeppolLookupHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new PeppolLookupHandler(mockPeppolLookupService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testHandleRequest_OptionsRequest() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");
        
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-123");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Methods");
        
        // Verify service was not called for OPTIONS request
        verifyNoInteractions(mockPeppolLookupService);
    }

    @Test
    void testHandleRequest_PostRequestWithValidBody() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        
        Map<String, Object> requestBody = Map.of(
            "participantId", "9915:test",
            "environment", "test",
            "documentTypeId", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"
        );
        
        request.setBody(objectMapper.writeValueAsString(requestBody));

        LookupResponse mockResponse = new LookupResponse();
        mockResponse.setParticipantId("9915:test");
        mockResponse.setSuccessful(true);
        mockResponse.setEnvironment("test");
        mockResponse.setTimestamp(Instant.now());
        mockResponse.setTotalProcessingTimeMs(750L);
        
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-123");
        when(mockPeppolLookupService.performLookup(any())).thenReturn(mockResponse);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        
        verify(mockPeppolLookupService).performLookup(any());
    }

    @Test
    void testHandleRequest_PostRequestEmptyBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setBody("");
        
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-123");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request format");
        
        verifyNoInteractions(mockPeppolLookupService);
    }

    @Test
    void testHandleRequest_ServiceException() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        
        Map<String, Object> requestBody = Map.of(
            "participantId", "9915:test",
            "documentTypeId", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"
        );
        
        request.setBody(objectMapper.writeValueAsString(requestBody));

        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-123");
        when(mockPeppolLookupService.performLookup(any())).thenThrow(new RuntimeException("Service error"));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Internal server error");
        assertThat(response.getBody()).contains("Service error");
    }

    @Test
    void testDefaultConstructor() {
        // Test that default constructor works
        PeppolLookupHandler defaultHandler = new PeppolLookupHandler();
        assertThat(defaultHandler).isNotNull();
    }
}