package org.opennms.core.wsman.cxf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.opennms.core.wsman.WSManClient;
import org.opennms.core.wsman.WSManConstants;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.WSManEndpoint.WSManVersion;
import org.opennms.core.wsman.WSManException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import com.google.common.collect.Maps;

import schemas.dmtf.org.wbem.wsman.v1.AnyListType;
import schemas.dmtf.org.wbem.wsman.v1.AttributableEmpty;
import schemas.dmtf.org.wbem.wsman.v1.ObjectFactory;

public class CXFWSManClient implements WSManClient {
    private final static Logger LOG = LoggerFactory.getLogger(CXFWSManClient.class);

    private final WSManEndpoint m_endpoint;

    public CXFWSManClient(WSManEndpoint endpoint) {
        m_endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    }

    public EnumerationOperations getEnumerator(String resourceUri) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(EnumerationOperations.class);
        factory.setAddress(m_endpoint.getUrl().toExternalForm());

        // Force the client to use SOAP v1.2, as per:
        // R13.1-1: A service shall at least receive and send SOAP 1.2 SOAP Envelopes.
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        EnumerationOperations enumerator = factory.create(EnumerationOperations.class);

        Client cxfClient = ClientProxy.getClient(enumerator);
        Map<String, Object> requestContext = cxfClient.getRequestContext();

        // Add static name-space mappings to make visualizing the XML clearer
        Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing");
        nsMap.put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration");
        nsMap.put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd");
        cxfClient.getRequestContext().put("soap.env.ns.map", nsMap);

        if (!m_endpoint.isStrictSSL()) {
            LOG.debug("Disabling strict SSL checking.");
            // Accept all certs
            HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
            TrustManager[] simpleTrustManager = new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };
            TLSClientParameters tlsParams = new TLSClientParameters();
            tlsParams.setTrustManagers(simpleTrustManager);
            tlsParams.setDisableCNCheck(true);
            http.setTlsClientParameters(tlsParams);
        }

        // Setup auth
        if (m_endpoint.isBasicAuth()) {
            LOG.debug("Enabling basic authentication.");
            HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
            http.setAuthSupplier(new DefaultBasicAuthSupplier());
            http.getAuthorization().setUserName(m_endpoint.getUsername());
            http.getAuthorization().setPassword(m_endpoint.getPassword());

            requestContext.put(BindingProvider.USERNAME_PROPERTY, m_endpoint.getUsername());
            requestContext.put(BindingProvider.PASSWORD_PROPERTY, m_endpoint.getPassword());
        }

        // Set reply-to
        AddressingProperties maps = new AddressingProperties();
        EndpointReferenceType ref = new EndpointReferenceType();
        AttributedURIType add = new AttributedURIType();
        add.setValue("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous");
        ref.setAddress(add);
        maps.setReplyTo(ref);
        maps.setFaultTo(ref);
        requestContext.put("javax.xml.ws.addressing.context", maps);

        // Add WS-Man ResourceURI to the header
        AddResourecURIInterceptor interceptor = new AddResourecURIInterceptor(WSManConstants.CIM_ALL_AVAILABLE_CLASSES);
        cxfClient.getOutInterceptors().add(interceptor);

        // Relocate the Filter element to the WS-Man namespace
        // Our WSDls generate it one package but the servers expect it to be in the other
        Map<String, String> outTransformMap = Maps.newHashMap();
        outTransformMap.put("{http://schemas.xmlsoap.org/ws/2004/09/enumeration}Filter", "{http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd}Filter");

        if (m_endpoint.getServerVersion() == WSManVersion.WSMAN_1_0) {
            // WS-Man 1.0 does not support the W3C WS-Addressing, so we need to change the namespace
            // "http://www.w3.org/2005/08/addressing" becomes "http://schemas.xmlsoap.org/ws/2004/08/addressing"
            outTransformMap.put("{http://www.w3.org/2005/08/addressing}*", "{http://schemas.xmlsoap.org/ws/2004/08/addressing}*");
        }

        if (!outTransformMap.isEmpty()) {
            final TransformOutInterceptor transformOutInterceptor = new TransformOutInterceptor();
            transformOutInterceptor.setOutTransformElements(outTransformMap);
            cxfClient.getOutInterceptors().add(transformOutInterceptor);
        }

        return enumerator;
    }

    private EnumerateResponse enumerate(String dialect, String filter, String resourceUri, boolean optimized) {
        FilterType filterType = new FilterType();
        filterType.setDialect(dialect);
        filterType.getContent().add(filter);
        Enumerate enumerate = new Enumerate();
        enumerate.setFilter(filterType);

        if (optimized) {
            // Request an optimized response
            JAXBElement<AttributableEmpty> optimizeEnumeration = new ObjectFactory().createOptimizeEnumeration(new AttributableEmpty());
            enumerate.getAny().add(optimizeEnumeration);
        }

        return getEnumerator(resourceUri).enumerate(enumerate);
    }

    @Override
    public String enumerateWithFilter(String dialect, String filter, String resourceUri) {
        EnumerateResponse enumerateResponse = enumerate(dialect, filter, resourceUri, false);
        if (enumerateResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        return (String)enumerateResponse.getEnumerationContext().getContent().get(0);
    }

    @Override
    public List<Node> pull(String contextId, String resourceUri) {
        EnumerationContextType enumContext = new EnumerationContextType();
        enumContext.getContent().add(contextId);
        Pull pull = new Pull();
        pull.setEnumerationContext(enumContext);

        PullResponse pullResponse = getEnumerator(resourceUri).pull(pull);
        if (pullResponse == null) {
            throw new WSManException(String.format("Pull failed for context id: %s. See logs for details.", contextId));
        }

        ItemListType itemList = pullResponse.getItems();
        return itemList.getAny().stream()
            .filter(o -> o instanceof org.w3c.dom.Node)
            .map(o -> (Node)o)
            .collect(Collectors.toList());
    }

    @Override
    public List<Node> enumerateAndPullUsingFilter(String dialect, String filter, String resourceUri) {
        EnumerateResponse enumerateResponse = enumerate(dialect, filter, resourceUri, true);
        if (enumerateResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        // FIXME: This is nasty and returns the first list, which could be anything
        for (Object object : enumerateResponse.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> el = (JAXBElement<?>)object;
                Object value = el.getValue();
                if (value instanceof AnyListType) {
                    AnyListType itemList = (AnyListType)value;
                    return itemList.getAny().stream()
                            .filter(o -> o instanceof org.w3c.dom.Node)
                            .map(o -> (Node)o)
                            .collect(Collectors.toList());
                }
            }
        }

        // Fallback to pulling
        String contextId = (String)enumerateResponse.getEnumerationContext().getContent().get(0);
        return pull(contextId, resourceUri);
    }
}
