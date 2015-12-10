package org.opennms;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Action;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.Addressing;

import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@BindingType(value="http://www.w3.org/2003/05/soap/bindings/HTTP/")
@Addressing(required = false, enabled = false)
@WebService(targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")
public interface EnumerationOperations {

        @WebResult(name = "EnumerateResponse", 
            targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/enumeration", 
            partName = "body")
        @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate",
            output = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetResponse")
        @WebMethod(operationName = "Enumerate")
        public EnumerateResponse enumerate(Enumerate enumerate);

        @WebResult(name = "PullResponse", 
            targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/enumeration", 
            partName = "body")
        @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull",
            output = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse")
        @WebMethod(operationName = "Pull")
        public PullResponse pull(Pull pull);
}
