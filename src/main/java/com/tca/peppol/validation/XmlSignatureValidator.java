package com.tca.peppol.validation;

import com.tca.peppol.util.XmlSecurityException;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * W3C XML Signature validator that provides comprehensive validation of XML digital signatures
 * according to W3C XML Signature specification with Canonical XML 1.0 processing.
 * 
 * This validator implements critical security requirements for Peppol SMP XML signature validation:
 * - W3C XML Signature specification compliance
 * - Canonical XML 1.0 processing (CRITICAL requirement)
 * - Strong signature algorithm validation (RSA-SHA256 or stronger)
 * - Comprehensive signature reference validation
 * - Key info validation and certificate matching
 * 
 * Security Features:
 * - Rejects weak algorithms (MD5, SHA1)
 * - Validates all signature references and transforms
 * - Ensures proper canonicalization
 * - Verifies certificate matching with SMP signing certificate
 */
public class XmlSignatureValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(XmlSignatureValidator.class);
    
    // XML Signature namespace and XPath expressions
    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String SIGNATURE_XPATH = "//ds:Signature";
    private static final String SIGNATURE_METHOD_XPATH = "ds:SignedInfo/ds:SignatureMethod/@Algorithm";
    private static final String CANONICALIZATION_METHOD_XPATH = "ds:SignedInfo/ds:CanonicalizationMethod/@Algorithm";
    private static final String REFERENCE_XPATH = "ds:SignedInfo/ds:Reference";
    private static final String KEY_INFO_XPATH = "ds:KeyInfo";
    
    // Approved signature algorithms (RSA-SHA256 or stronger)
    private static final Set<String> APPROVED_SIGNATURE_ALGORITHMS = new HashSet<>(Arrays.asList(
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384,
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512,
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256_MGF1,
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384_MGF1,
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512_MGF1
    ));
    
    // Rejected weak signature algorithms
    private static final Set<String> REJECTED_SIGNATURE_ALGORITHMS = new HashSet<>(Arrays.asList(
        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1,
        "http://www.w3.org/2000/09/xmldsig#rsa-md5", // MD5 URI
        "http://www.w3.org/2000/09/xmldsig#rsa-sha1" // Alternative SHA1 URI
    ));
    
    // Approved canonicalization algorithms (Canonical XML 1.0 - CRITICAL)
    private static final Set<String> APPROVED_CANONICALIZATION_ALGORITHMS = new HashSet<>(Arrays.asList(
        Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS,
        Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS,
        Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS,
        Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS
    ));
    
    private final XPath xpath;
    
    static {
        // Initialize Apache XML Security library
        if (!Init.isInitialized()) {
            Init.init();
            logger.debug("Apache XML Security library initialized");
        }
    }
    
    /**
     * Creates a new XML signature validator.
     */
    public XmlSignatureValidator() {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        this.xpath = xpathFactory.newXPath();
        
        // Set up namespace context for XML Signature namespace
        this.xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("ds".equals(prefix)) {
                    return XMLDSIG_NS;
                }
                return null;
            }
            
            @Override
            public String getPrefix(String namespaceURI) {
                if (XMLDSIG_NS.equals(namespaceURI)) {
                    return "ds";
                }
                return null;
            }
            
            @Override
            public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                if (XMLDSIG_NS.equals(namespaceURI)) {
                    return Arrays.asList("ds").iterator();
                }
                return null;
            }
        });
    }
    
    /**
     * Validates XML signature in the provided document according to W3C XML Signature specification.
     * 
     * @param xmlDocument The XML document containing the signature to validate
     * @param expectedCertificate The expected signing certificate to match against
     * @return XmlSignatureValidationResult containing validation results
     * @throws XmlSecurityException if validation fails due to security violations
     */
    public XmlSignatureValidationResult validateXmlSignature(Document xmlDocument, X509Certificate expectedCertificate) 
            throws XmlSecurityException {
        
        if (xmlDocument == null) {
            throw new XmlSecurityException("XML document cannot be null");
        }
        
        logger.debug("Starting XML signature validation");
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract signature elements from the document
            List<Element> signatureElements = extractSignatureElements(xmlDocument);
            
            if (signatureElements.isEmpty()) {
                logger.warn("No XML signatures found in document");
                return XmlSignatureValidationResult.builder()
                    .signaturePresent(false)
                    .valid(false)
                    .errorMessage("No XML signatures found in document")
                    .build();
            }
            
            // Validate each signature (typically there should be only one)
            XmlSignatureValidationResult result = null;
            for (Element signatureElement : signatureElements) {
                result = validateSingleSignature(signatureElement, expectedCertificate);
                
                // If any signature is valid, consider the document valid
                if (result.isValid()) {
                    break;
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("XML signature validation completed in {} ms, result: {}", 
                        processingTime, result != null ? result.isValid() : false);
            
            return result != null ? result : XmlSignatureValidationResult.builder()
                .signaturePresent(true)
                .valid(false)
                .errorMessage("All signatures failed validation")
                .build();
                
        } catch (Exception e) {
            logger.error("XML signature validation failed with exception", e);
            throw new XmlSecurityException("XML signature validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates a single XML signature element.
     * 
     * @param signatureElement The signature element to validate
     * @param expectedCertificate The expected signing certificate
     * @return XmlSignatureValidationResult containing validation results
     * @throws XmlSecurityException if validation fails
     */
    private XmlSignatureValidationResult validateSingleSignature(Element signatureElement, X509Certificate expectedCertificate) 
            throws XmlSecurityException {
        
        XmlSignatureValidationResult.Builder resultBuilder = XmlSignatureValidationResult.builder()
            .signaturePresent(true);
        
        try {
            // Create XMLSignature object from the element
            XMLSignature xmlSignature;
            try {
                xmlSignature = new XMLSignature(signatureElement, null);
            } catch (XMLSignatureException e) {
                // Handle cases where the signature algorithm is not supported/recognized
                String errorMsg = "XML signature processing failed: " + e.getMessage();
                logger.error(errorMsg, e);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // Validate signature algorithm (CRITICAL: reject weak algorithms)
            String signatureAlgorithm = xmlSignature.getSignedInfo().getSignatureMethodURI();
            boolean algorithmValid = validateSignatureAlgorithm(signatureAlgorithm);
            resultBuilder.signatureAlgorithmValid(algorithmValid);
            
            if (!algorithmValid) {
                String errorMsg = "Invalid or weak signature algorithm: " + signatureAlgorithm;
                logger.error(errorMsg);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // Validate canonicalization method (CRITICAL: Canonical XML 1.0)
            String canonicalizationAlgorithm = xmlSignature.getSignedInfo().getCanonicalizationMethodURI();
            boolean canonicalizationValid = validateCanonicalizationAlgorithm(canonicalizationAlgorithm);
            resultBuilder.canonicalizationValid(canonicalizationValid);
            
            if (!canonicalizationValid) {
                String errorMsg = "Invalid canonicalization algorithm: " + canonicalizationAlgorithm;
                logger.error(errorMsg);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // Validate signature references
            boolean referencesValid = validateSignatureReferences(xmlSignature);
            resultBuilder.referencesValid(referencesValid);
            
            if (!referencesValid) {
                String errorMsg = "Signature reference validation failed";
                logger.error(errorMsg);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // Validate key info and extract certificate
            X509Certificate signingCertificate = validateKeyInfo(xmlSignature);
            boolean keyInfoValid = (signingCertificate != null);
            resultBuilder.keyInfoValid(keyInfoValid);
            
            if (!keyInfoValid) {
                String errorMsg = "Key info validation failed or certificate not found";
                logger.error(errorMsg);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // Validate certificate matching if expected certificate is provided
            boolean certificateMatches = true;
            if (expectedCertificate != null) {
                certificateMatches = validateCertificateMatching(signingCertificate, expectedCertificate);
                resultBuilder.certificateMatches(certificateMatches);
                
                if (!certificateMatches) {
                    String errorMsg = "Signing certificate does not match expected certificate";
                    logger.error(errorMsg);
                    return resultBuilder.valid(false).errorMessage(errorMsg).build();
                }
            }
            
            // Perform cryptographic signature verification
            boolean signatureValid = xmlSignature.checkSignatureValue(signingCertificate.getPublicKey());
            resultBuilder.cryptographicSignatureValid(signatureValid);
            
            if (!signatureValid) {
                String errorMsg = "Cryptographic signature verification failed";
                logger.error(errorMsg);
                return resultBuilder.valid(false).errorMessage(errorMsg).build();
            }
            
            // All validations passed
            logger.debug("XML signature validation successful");
            return resultBuilder
                .valid(true)
                .signingCertificate(signingCertificate)
                .signatureAlgorithm(signatureAlgorithm)
                .canonicalizationAlgorithm(canonicalizationAlgorithm)
                .build();
                
        } catch (XMLSignatureException e) {
            logger.error("XML signature processing failed", e);
            throw new XmlSecurityException("XML signature processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during signature validation", e);
            throw new XmlSecurityException("Signature validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts all signature elements from the XML document.
     * 
     * @param document The XML document to search
     * @return List of signature elements
     * @throws XmlSecurityException if extraction fails
     */
    private List<Element> extractSignatureElements(Document document) throws XmlSecurityException {
        try {
            XPathExpression signatureExpr = xpath.compile(SIGNATURE_XPATH);
            NodeList signatureNodes = (NodeList) signatureExpr.evaluate(document, XPathConstants.NODESET);
            
            List<Element> signatureElements = new ArrayList<>();
            for (int i = 0; i < signatureNodes.getLength(); i++) {
                if (signatureNodes.item(i) instanceof Element) {
                    signatureElements.add((Element) signatureNodes.item(i));
                }
            }
            
            logger.debug("Found {} signature elements in document", signatureElements.size());
            return signatureElements;
            
        } catch (XPathExpressionException e) {
            logger.error("Failed to extract signature elements", e);
            throw new XmlSecurityException("Failed to extract signature elements: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates the signature algorithm, ensuring it's strong enough and not on the rejected list.
     * 
     * @param algorithm The signature algorithm URI
     * @return true if algorithm is approved, false otherwise
     */
    private boolean validateSignatureAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            logger.error("Signature algorithm is null or empty");
            return false;
        }
        
        // Check if algorithm is explicitly rejected (weak algorithms)
        if (REJECTED_SIGNATURE_ALGORITHMS.contains(algorithm)) {
            logger.error("Signature algorithm {} is explicitly rejected (weak algorithm)", algorithm);
            return false;
        }
        
        // Check if algorithm is in approved list
        boolean approved = APPROVED_SIGNATURE_ALGORITHMS.contains(algorithm);
        
        if (!approved) {
            logger.warn("Signature algorithm {} is not in approved list", algorithm);
        } else {
            logger.debug("Signature algorithm {} is approved", algorithm);
        }
        
        return approved;
    }
    
    /**
     * Validates the canonicalization algorithm, ensuring Canonical XML 1.0 compliance.
     * 
     * @param algorithm The canonicalization algorithm URI
     * @return true if algorithm is approved, false otherwise
     */
    private boolean validateCanonicalizationAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            logger.error("Canonicalization algorithm is null or empty");
            return false;
        }
        
        boolean approved = APPROVED_CANONICALIZATION_ALGORITHMS.contains(algorithm);
        
        if (!approved) {
            logger.error("Canonicalization algorithm {} is not approved (must be Canonical XML 1.0)", algorithm);
        } else {
            logger.debug("Canonicalization algorithm {} is approved", algorithm);
        }
        
        return approved;
    }
    
    /**
     * Validates all signature references and their transforms.
     * 
     * @param xmlSignature The XML signature to validate
     * @return true if all references are valid, false otherwise
     */
    private boolean validateSignatureReferences(XMLSignature xmlSignature) {
        try {
            int referenceCount = xmlSignature.getSignedInfo().getLength();
            
            if (referenceCount == 0) {
                logger.error("No signature references found");
                return false;
            }
            
            logger.debug("Validating {} signature references", referenceCount);
            
            for (int i = 0; i < referenceCount; i++) {
                try {
                    // Validate each reference by checking its digest value
                    org.apache.xml.security.signature.Reference reference = xmlSignature.getSignedInfo().item(i);
                    boolean referenceValid = reference.verify();
                    
                    if (!referenceValid) {
                        logger.error("Signature reference {} validation failed", i);
                        return false;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error validating signature reference {}: {}", i, e.getMessage());
                    return false;
                }
            }
            
            logger.debug("All signature references validated successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Error during signature reference validation", e);
            return false;
        }
    }
    
    /**
     * Validates the key info and extracts the signing certificate.
     * 
     * @param xmlSignature The XML signature containing key info
     * @return The signing certificate if found and valid, null otherwise
     */
    private X509Certificate validateKeyInfo(XMLSignature xmlSignature) {
        try {
            if (xmlSignature.getKeyInfo() == null) {
                logger.error("No KeyInfo element found in signature");
                return null;
            }
            
            // Try to extract X509 certificate from KeyInfo
            X509Certificate certificate = xmlSignature.getKeyInfo().getX509Certificate();
            
            if (certificate == null) {
                logger.error("No X509 certificate found in KeyInfo");
                return null;
            }
            
            // Validate certificate is not expired
            try {
                certificate.checkValidity();
                logger.debug("Certificate validity check passed");
            } catch (Exception e) {
                logger.error("Certificate validity check failed: {}", e.getMessage());
                return null;
            }
            
            // Additional key info validation could be added here
            logger.debug("Key info validation successful, certificate extracted");
            return certificate;
            
        } catch (Exception e) {
            logger.error("Error during key info validation", e);
            return null;
        }
    }
    
    /**
     * Validates that the signing certificate matches the expected certificate.
     * 
     * @param signingCertificate The certificate extracted from the signature
     * @param expectedCertificate The expected signing certificate
     * @return true if certificates match, false otherwise
     */
    private boolean validateCertificateMatching(X509Certificate signingCertificate, X509Certificate expectedCertificate) {
        if (signingCertificate == null || expectedCertificate == null) {
            logger.error("Cannot compare certificates: one or both are null");
            return false;
        }
        
        try {
            // Compare certificates by their encoded form (most reliable method)
            boolean matches = Arrays.equals(signingCertificate.getEncoded(), expectedCertificate.getEncoded());
            
            if (matches) {
                logger.debug("Signing certificate matches expected certificate");
            } else {
                logger.error("Signing certificate does not match expected certificate");
                logger.debug("Signing cert subject: {}", signingCertificate.getSubjectX500Principal());
                logger.debug("Expected cert subject: {}", expectedCertificate.getSubjectX500Principal());
            }
            
            return matches;
            
        } catch (Exception e) {
            logger.error("Error comparing certificates", e);
            return false;
        }
    }
    
    /**
     * Performs canonical XML 1.0 processing on the provided XML element.
     * This method is exposed for testing and debugging purposes.
     * 
     * @param element The XML element to canonicalize
     * @param algorithm The canonicalization algorithm to use
     * @return Canonicalized XML as byte array
     * @throws XmlSecurityException if canonicalization fails
     */
    public byte[] canonicalizeXml(Element element, String algorithm) throws XmlSecurityException {
        if (element == null) {
            throw new XmlSecurityException("Element cannot be null");
        }
        
        if (!validateCanonicalizationAlgorithm(algorithm)) {
            throw new XmlSecurityException("Invalid canonicalization algorithm: " + algorithm);
        }
        
        try {
            Canonicalizer canonicalizer = Canonicalizer.getInstance(algorithm);
            
            // Use ByteArrayOutputStream to capture the canonicalized output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            canonicalizer.canonicalizeSubtree(element, baos);
            byte[] canonicalizedXml = baos.toByteArray();
            
            logger.debug("XML canonicalization successful using algorithm: {}", algorithm);
            return canonicalizedXml;
            
        } catch (Exception e) {
            logger.error("XML canonicalization failed", e);
            throw new XmlSecurityException("XML canonicalization failed: " + e.getMessage(), e);
        }
    }
}