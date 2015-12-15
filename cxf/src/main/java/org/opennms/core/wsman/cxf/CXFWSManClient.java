/*
 * Copyright 2015, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package org.opennms.core.wsman.cxf;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.opennms.core.wsman.WSManClient;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.WSManException;
import org.opennms.core.wsman.WSManVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;
import org.xmlsoap.schemas.ws._2004._09.transfer.TransferElement;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import schemas.dmtf.org.wbem.wsman.v1.AnyListType;
import schemas.dmtf.org.wbem.wsman.v1.AttributableEmpty;
import schemas.dmtf.org.wbem.wsman.v1.AttributablePositiveInteger;
import schemas.dmtf.org.wbem.wsman.v1.ObjectFactory;

public class CXFWSManClient implements WSManClient {
    private final static Logger LOG = LoggerFactory.getLogger(CXFWSManClient.class);

    private final WSManEndpoint m_endpoint;

    public CXFWSManClient(WSManEndpoint endpoint) {
        m_endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    }

    public <ProxyServiceType> ProxyServiceType createProxyFor(Class<ProxyServiceType> serviceClass,
            String resourceUri,
            Map<String, String> outTransformMap, Map<String, String> inTransformMap) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(serviceClass);
        factory.setAddress(m_endpoint.getUrl().toExternalForm());

        // Force the client to use SOAP v1.2, as per:
        // R13.1-1: A service shall at least receive and send SOAP 1.2 SOAP Envelopes.
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        ProxyServiceType proxyService = factory.create(serviceClass);

        Client cxfClient = ClientProxy.getClient(proxyService);
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
        AddResourceURIInterceptor interceptor = new AddResourceURIInterceptor(resourceUri);
        cxfClient.getOutInterceptors().add(interceptor);

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

        if (!inTransformMap.isEmpty()) {
            final TransformInInterceptor transformInInterceptor = new TransformInInterceptor();
            transformInInterceptor.setInTransformElements(inTransformMap);
            cxfClient.getInInterceptors().add(transformInInterceptor);
        }

        // Remove the action attribute from the Content-Type header. Reasoning:
        // By default, CXF will add the action to the Content-Type header, generating something like:
        // Content-Type: application/soap+xml; action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate"
        // Windows Server 2008 barfs on the action=".*" attribute and none of the other servers
        // seem to care of it's there or not, so we remove it.
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put("Content-Type", Collections.singletonList("application/soap+xml;charset=UTF-8"));
        requestContext.put(Message.PROTOCOL_HEADERS, headers);

        return proxyService;
    }

    public EnumerationOperations getEnumerator(String resourceUri) {
        // Relocate the Filter element to the WS-Man namespace
        // Our WSDls generate it one package but the servers expect it to be in the other
        Map<String, String> outTransformMap = Maps.newHashMap();
        outTransformMap.put("{http://schemas.xmlsoap.org/ws/2004/09/enumeration}Filter", "{http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd}Filter");

        return createProxyFor(EnumerationOperations.class, resourceUri,
                outTransformMap, Maps.newHashMap());
    }

    private TransferOperations getTransferer(Map<String, String> selectors, String resourceUri) {
        // FIXME: This could break
        String elementType = resourceUri.substring(resourceUri.lastIndexOf("/") + 1);

        // Modify the incoming response to use a generic element instead
        // of the one provided
        Map<String, String> inTransformMap = Maps.newHashMap();
        inTransformMap.put(String.format("{%s}%s", resourceUri, elementType),
                "{http://schemas.xmlsoap.org/ws/2004/09/transfer}TransferElement");

        TransferOperations transferer = createProxyFor(TransferOperations.class, resourceUri,
                Maps.newHashMap(), inTransformMap);
        Client cxfClient = ClientProxy.getClient(transferer);

        // Add WS-Man ResourceURI to the header
        AddSelectorSetInterceptor interceptor = new AddSelectorSetInterceptor(selectors);
        cxfClient.getOutInterceptors().add(interceptor);

        return transferer;
    }

    private EnumerateResponse enumerate(String dialect, String filter, String resourceUri, boolean optimized) {
        FilterType filterType = new FilterType();
        filterType.setDialect(dialect);
        filterType.getContent().add(filter);
        Enumerate enumerate = new Enumerate();
        enumerate.setFilter(filterType);

        ObjectFactory factory = new ObjectFactory();

        if (optimized) {
            // Request an optimized response
            JAXBElement<AttributableEmpty> optimizeEnumeration = factory.createOptimizeEnumeration(new AttributableEmpty());
            enumerate.getAny().add(optimizeEnumeration);
        }

        if (m_endpoint.getMaxElements() != null) {
            AttributablePositiveInteger maxElementsValue = new AttributablePositiveInteger();
            maxElementsValue.setValue(BigInteger.valueOf(m_endpoint.getMaxElements()));
            JAXBElement<AttributablePositiveInteger> maxElements = factory.createMaxElements(maxElementsValue);
            enumerate.getAny().add(maxElements);
        }

        return getEnumerator(resourceUri).enumerate(enumerate);
    }

    private EnumerateResponse enumerate(String resourceUri, boolean optimized) {
        Enumerate enumerate = new Enumerate();

        ObjectFactory factory = new ObjectFactory();
        
        if (optimized) {
            // Request an optimized response
            JAXBElement<AttributableEmpty> optimizeEnumeration = factory.createOptimizeEnumeration(new AttributableEmpty());
            enumerate.getAny().add(optimizeEnumeration);
        }

        if (m_endpoint.getMaxElements() != null) {
            AttributablePositiveInteger maxElementsValue = new AttributablePositiveInteger();
            maxElementsValue.setValue(BigInteger.valueOf(m_endpoint.getMaxElements()));
            JAXBElement<AttributablePositiveInteger> maxElements = factory.createMaxElements(maxElementsValue);
            enumerate.getAny().add(maxElements);
        }

        return getEnumerator(resourceUri).enumerate(enumerate);
    }

    @Override
    public String enumerate(String resourceUri) {
        EnumerateResponse enumerateResponse = enumerate(resourceUri, false);
        if (enumerateResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        return (String)enumerateResponse.getEnumerationContext().getContent().get(0);
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
    public List<Node> pull(String contextId, String resourceUri, boolean recursive) {
        EnumerationContextType enumContext = new EnumerationContextType();
        enumContext.getContent().add(contextId);
        Pull pull = new Pull();
        pull.setEnumerationContext(enumContext);

        PullResponse pullResponse = getEnumerator(resourceUri).pull(pull);
        if (pullResponse == null) {
            throw new WSManException(String.format("Pull failed for context id: %s. See logs for details.", contextId));
        }

        final List<Node> nodes = pullResponse.getItems().getAny().stream()
                .filter(o -> o instanceof org.w3c.dom.Node)
                .map(o -> (Node)o)
                .collect(Collectors.toList());

        if (recursive && pullResponse.getEndOfSequence() == null) {
            // Recurse, since we haven't reached the end-of-sequence yet
            nodes.addAll(pull(contextId, resourceUri, recursive));
        }

        return nodes;
    }

    @Override
    public List<Node> enumerateAndPull(String resourceUri, boolean recursive) {
        EnumerateResponse enumerateResponse = enumerate(resourceUri, true);
        if (enumerateResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        boolean endOfSequence = false;
        List<Node> nodes = Lists.newArrayList();
        // FIXME: This is nasty
        for (Object object : enumerateResponse.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> el = (JAXBElement<?>)object;
                if ("Items".equals(el.getName().getLocalPart())) {
                    Object value = el.getValue();
                    if (value instanceof AnyListType) {
                        AnyListType itemList = (AnyListType)value;
                        nodes = itemList.getAny().stream()
                                .filter(o -> o instanceof org.w3c.dom.Node)
                                .map(o -> (Node)o)
                                .collect(Collectors.toList());
                    }
                } else if ("EndOfSequence".equals(el.getName().getLocalPart())) {
                    endOfSequence = true;
                }
            } else if (object instanceof Node) {
                Node node = (Node)object;
                // NOTE: Can be in wsen or wsman namespaces
                if ("EndOfSequence".equals(node.getLocalName())) {
                    endOfSequence = true;
                }
            }
        }


        if (!endOfSequence) {
            String contextId = (String)enumerateResponse.getEnumerationContext().getContent().get(0);
            nodes.addAll(pull(contextId, resourceUri, recursive));
        }
        return nodes;
    }

    @Override
    public List<Node> enumerateAndPullUsingFilter(String dialect, String filter, String resourceUri, boolean recursive) {
        EnumerateResponse enumerateResponse = enumerate(dialect, filter, resourceUri, true);
        if (enumerateResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        boolean endOfSequence = false;
        List<Node> nodes = Lists.newArrayList();
        // FIXME: This is nasty
        for (Object object : enumerateResponse.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> el = (JAXBElement<?>)object;
                if ("Items".equals(el.getName().getLocalPart())) {
                    Object value = el.getValue();
                    if (value instanceof AnyListType) {
                        AnyListType itemList = (AnyListType)value;
                        nodes = itemList.getAny().stream()
                                .filter(o -> o instanceof org.w3c.dom.Node)
                                .map(o -> (Node)o)
                                .collect(Collectors.toList());
                    }
                } else if ("EndOfSequence".equals(el.getName().getLocalPart())) {
                    endOfSequence = true;
                }
            } else if (object instanceof Node) {
                Node node = (Node)object;
                // NOTE: Can be in wsen or wsman namespaces
                if ("EndOfSequence".equals(node.getLocalName())) {
                    endOfSequence = true;
                }
            }
        }

        if (!endOfSequence) {
            String contextId = (String)enumerateResponse.getEnumerationContext().getContent().get(0);
            nodes.addAll(pull(contextId, resourceUri, recursive));
        }
        return nodes;
    }

    @Override
    public Node get(Map<String, String> selectors, String resourceUri) {
        TransferOperations transferer = getTransferer(selectors, resourceUri);
        TransferElement transferElement = transferer.get();
        if (transferElement == null) {
            throw new WSManException("Get failed. See logs for details.");
        }

        // FIXME: Convert the transfer element back to it's original type
        // Convert the TransferElement to a generic node
        DOMResult res = new DOMResult();
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(transferElement.getClass());
            context.createMarshaller().marshal(transferElement, res);
            return res.getNode().getFirstChild();
        } catch (JAXBException e) {
            throw new WSManException("XML serialization failed.", e);
        }
    }
}
