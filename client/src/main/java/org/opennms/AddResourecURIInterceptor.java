package org.opennms;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.phase.Phase;

import com.wsman.AttributableURI;
import com.wsman.ObjectFactory;

public class AddResourecURIInterceptor extends AbstractSoapInterceptor {

    private final String resourceUri;

    public AddResourecURIInterceptor(String resourceUri) {
        super(Phase.POST_LOGICAL);
        addAfter(SoapPreProtocolOutInterceptor.class.getName());
        this.resourceUri = resourceUri;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
       List<Header> headers = message.getHeaders();

        AttributableURI uri = new AttributableURI();
        uri.setValue(resourceUri);
        ObjectFactory f = new ObjectFactory();
        JAXBElement<AttributableURI> resourceURI = f.createResourceURI(uri);

        Header header;
        try {
            header = new Header(resourceURI.getName(), resourceURI, new JAXBDataBinding(AttributableURI.class));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        headers.add(header);
        message.put(Header.HEADER_LIST, headers);
    }
}
