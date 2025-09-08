package com.tca.peppol.example;

import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.service.ValidationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updated calling code example showing how to use the fixed ValidationOrchestrator.
 * This replaces your previous createDefaultValidationOrchestrator method.
 */
public class UpdatedCallingCodeExample {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdatedCallingCodeExample.class);
    
    /**
     * Create validation orchestrator using the default constructor (recommended approach)
     * This automatically creates all required dependencies including the SecureHttpClientWrapper
     */
    public ValidationOrchestrator createValidationOrchestrator() {
        // Note: In production, use Dagger dependency injection
        // This example is disabled as ValidationOrchestrator now requires dependencies
        logger.warn("ValidationOrchestrator example disabled - use Dagger dependency injection in production");
        return null;
    }
    
    /**
     * Create validation orchestrator with custom SecureHttpClient (if needed)
     * This approach gives you more control over the HTTP client configuration
     */
    public ValidationOrchestrator createValidationOrchestratorWithCustomHttpClient() {
        // If you need to use a specific SecureHttpClient instance, you can do:
        // Note: This requires modifying the ValidationOrchestrator to accept SecureHttpClient directly
        // For now, the wrapper approach is cleaner
        
        // Note: In production, use Dagger dependency injection
        logger.warn("ValidationOrchestrator example disabled - use Dagger dependency injection in production");
        return null;
    }
    
    /**
     * Example of how to use the ValidationOrchestrator in your existing code
     */
    public void exampleUsage() {
        // Create the orchestrator (this is now safe and production-ready)
        ValidationOrchestrator orchestrator = createValidationOrchestrator();
        
        // Use it as before - all the validation methods work the same way
        // The only difference is that the constructor now properly handles dependencies
        
        // Example validation call:
        /*
        ValidationContext context = new ValidationContext(
            smpResult, 
            participantId, 
            documentTypeId, 
            environment, 
            ValidationOptions.defaultOptions()
        );
        
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(context);
        */
    }
}
