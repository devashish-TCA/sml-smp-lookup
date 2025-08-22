package com.tca.peppol.model.internal;

import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;
import org.w3c.dom.Document;
import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * Internal model representing the result of an SMP (Service Metadata Publisher) query.
 * Contains endpoint information, certificate data, service dates, XML document,
 * and timing information from the SMP query operation.
 */
public class SmpResult {

    private String endpointUrl;
    private String transportProfile;
    private X509Certificate certificate;
    private Instant serviceActivationDate;
    private Instant serviceExpirationDate;
    private Document xmlDocument;
    private String xmlSignature;
    private long queryTimeMs;
    private boolean successful;
    private String errorMessage;
    private int httpStatusCode;
    private String rawXmlContent;
    private String smpQueryUrl;
    
    // Enhanced fields for peppol-commons integration
    private String participantId;
    private String documentTypeId;
    private ServiceGroupType serviceGroup;
    private ServiceMetadataType serviceMetadata;
    private SignedServiceMetadataType signedServiceMetadata;
    private Exception exception;
    private long responseTimeMs;

    // Default constructor
    public SmpResult() {}

    // Constructor for successful result
    public SmpResult(String endpointUrl, String transportProfile, X509Certificate certificate,
                    Instant serviceActivationDate, Instant serviceExpirationDate, 
                    Document xmlDocument, long queryTimeMs) {
        this.endpointUrl = endpointUrl;
        this.transportProfile = transportProfile;
        this.certificate = certificate;
        this.serviceActivationDate = serviceActivationDate;
        this.serviceExpirationDate = serviceExpirationDate;
        this.xmlDocument = xmlDocument;
        this.queryTimeMs = queryTimeMs;
        this.successful = true;
    }

    // Constructor for failed result
    public SmpResult(String smpQueryUrl, long queryTimeMs, String errorMessage, int httpStatusCode) {
        this.smpQueryUrl = smpQueryUrl;
        this.queryTimeMs = queryTimeMs;
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.successful = false;
    }

    // Getters and setters
    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getTransportProfile() {
        return transportProfile;
    }

    public void setTransportProfile(String transportProfile) {
        this.transportProfile = transportProfile;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public Instant getServiceActivationDate() {
        return serviceActivationDate;
    }

    public void setServiceActivationDate(Instant serviceActivationDate) {
        this.serviceActivationDate = serviceActivationDate;
    }

    public Instant getServiceExpirationDate() {
        return serviceExpirationDate;
    }

    public void setServiceExpirationDate(Instant serviceExpirationDate) {
        this.serviceExpirationDate = serviceExpirationDate;
    }

    public Document getXmlDocument() {
        return xmlDocument;
    }

    public void setXmlDocument(Document xmlDocument) {
        this.xmlDocument = xmlDocument;
    }

    public String getXmlSignature() {
        return xmlSignature;
    }

    public void setXmlSignature(String xmlSignature) {
        this.xmlSignature = xmlSignature;
    }

    public long getQueryTimeMs() {
        return queryTimeMs;
    }

    public void setQueryTimeMs(long queryTimeMs) {
        this.queryTimeMs = queryTimeMs;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getRawXmlContent() {
        return rawXmlContent;
    }

    public void setRawXmlContent(String rawXmlContent) {
        this.rawXmlContent = rawXmlContent;
    }

    public String getSmpQueryUrl() {
        return smpQueryUrl;
    }

    public void setSmpQueryUrl(String smpQueryUrl) {
        this.smpQueryUrl = smpQueryUrl;
    }

    // Enhanced getters and setters for peppol-commons integration
    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getDocumentTypeId() {
        return documentTypeId;
    }

    public void setDocumentTypeId(String documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public ServiceGroupType getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(ServiceGroupType serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public ServiceMetadataType getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(ServiceMetadataType serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    public SignedServiceMetadataType getSignedServiceMetadata() {
        return signedServiceMetadata;
    }

    public void setSignedServiceMetadata(SignedServiceMetadataType signedServiceMetadata) {
        this.signedServiceMetadata = signedServiceMetadata;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    /**
     * Check if the SMP query was successful and contains valid endpoint information
     */
    public boolean hasValidEndpoint() {
        return successful && endpointUrl != null && !endpointUrl.trim().isEmpty() && 
               transportProfile != null && !transportProfile.trim().isEmpty();
    }

    /**
     * Check if the SMP result contains a valid certificate
     */
    public boolean hasValidCertificate() {
        return certificate != null;
    }

    /**
     * Check if the service is currently active based on activation and expiration dates
     */
    public boolean isServiceActive() {
        Instant now = Instant.now();
        boolean afterActivation = serviceActivationDate == null || now.isAfter(serviceActivationDate);
        boolean beforeExpiration = serviceExpirationDate == null || now.isBefore(serviceExpirationDate);
        return afterActivation && beforeExpiration;
    }

    /**
     * Check if the SMP result contains XML signature information
     */
    public boolean hasXmlSignature() {
        return xmlSignature != null && !xmlSignature.trim().isEmpty();
    }

    /**
     * Create a successful SMP result
     */
    public static SmpResult success(String endpointUrl, String transportProfile, X509Certificate certificate,
                                  Instant serviceActivationDate, Instant serviceExpirationDate, 
                                  Document xmlDocument, long queryTimeMs) {
        return new SmpResult(endpointUrl, transportProfile, certificate, 
                           serviceActivationDate, serviceExpirationDate, xmlDocument, queryTimeMs);
    }

    /**
     * Create a failed SMP result
     */
    public static SmpResult failure(String smpQueryUrl, long queryTimeMs, String errorMessage, int httpStatusCode) {
        return new SmpResult(smpQueryUrl, queryTimeMs, errorMessage, httpStatusCode);
    }

    /**
     * Create a builder for SmpResult
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for SmpResult
     */
    public static class Builder {
        private SmpResult result = new SmpResult();

        public Builder endpointUrl(String endpointUrl) {
            result.endpointUrl = endpointUrl;
            return this;
        }

        public Builder transportProfile(String transportProfile) {
            result.transportProfile = transportProfile;
            return this;
        }

        public Builder certificate(X509Certificate certificate) {
            result.certificate = certificate;
            return this;
        }

        public Builder serviceActivationDate(Instant serviceActivationDate) {
            result.serviceActivationDate = serviceActivationDate;
            return this;
        }

        public Builder serviceExpirationDate(Instant serviceExpirationDate) {
            result.serviceExpirationDate = serviceExpirationDate;
            return this;
        }

        public Builder xmlDocument(Document xmlDocument) {
            result.xmlDocument = xmlDocument;
            return this;
        }

        public Builder xmlSignature(String xmlSignature) {
            result.xmlSignature = xmlSignature;
            return this;
        }

        public Builder queryTimeMs(long queryTimeMs) {
            result.queryTimeMs = queryTimeMs;
            return this;
        }

        public Builder successful(boolean successful) {
            result.successful = successful;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }

        public Builder httpStatusCode(int httpStatusCode) {
            result.httpStatusCode = httpStatusCode;
            return this;
        }

        public Builder rawXmlContent(String rawXmlContent) {
            result.rawXmlContent = rawXmlContent;
            return this;
        }

        public Builder smpQueryUrl(String smpQueryUrl) {
            result.smpQueryUrl = smpQueryUrl;
            return this;
        }

        public Builder participantId(String participantId) {
            result.participantId = participantId;
            return this;
        }

        public Builder documentTypeId(String documentTypeId) {
            result.documentTypeId = documentTypeId;
            return this;
        }

        public Builder serviceGroup(ServiceGroupType serviceGroup) {
            result.serviceGroup = serviceGroup;
            return this;
        }

        public Builder serviceMetadata(ServiceMetadataType serviceMetadata) {
            result.serviceMetadata = serviceMetadata;
            return this;
        }

        public Builder signedServiceMetadata(SignedServiceMetadataType signedServiceMetadata) {
            result.signedServiceMetadata = signedServiceMetadata;
            return this;
        }

        public Builder exception(Exception exception) {
            result.exception = exception;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            result.responseTimeMs = responseTimeMs;
            return this;
        }

        public SmpResult build() {
            // Set successful to true if we have basic required fields
            if (result.successful == false && result.endpointUrl != null && result.transportProfile != null) {
                result.successful = true;
            }
            return result;
        }
    }

    @Override
    public String toString() {
        return "SmpResult{" +
                "endpointUrl='" + endpointUrl + '\'' +
                ", transportProfile='" + transportProfile + '\'' +
                ", certificate=" + (certificate != null ? certificate.getSubjectX500Principal().getName() : "null") +
                ", serviceActivationDate=" + serviceActivationDate +
                ", serviceExpirationDate=" + serviceExpirationDate +
                ", queryTimeMs=" + queryTimeMs +
                ", successful=" + successful +
                ", errorMessage='" + errorMessage + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                ", smpQueryUrl='" + smpQueryUrl + '\'' +
                '}';
    }
}