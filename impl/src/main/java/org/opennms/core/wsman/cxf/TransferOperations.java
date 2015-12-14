package org.opennms.core.wsman.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.soap.Addressing;

import org.xmlsoap.schemas.ws._2004._09.transfer.ObjectFactory;
import org.xmlsoap.schemas.ws._2004._09.transfer.TransferElement;

/**
 * This port type defines a resource that may be read, written, and deleted.
 *
 * See org.xmlsoap.schemas.ws._2004._09.transfer.Resource for generated class.
 *
 * @author jwhite
 */
@WebService(targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer", name = "Resource")
@XmlSeeAlso({ObjectFactory.class, org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(required = false, enabled = true)
public interface TransferOperations {

    @WebMethod(operationName = "Get")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse")
    @WebResult(name = "TransferElement", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer", partName = "Body")
    public TransferElement get();

    @WebMethod(operationName = "Put")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse")
    public void put(
        @WebParam(partName = "Body", mode = WebParam.Mode.INOUT, name = "TransferElement", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer")
        javax.xml.ws.Holder<TransferElement> body
    );

    @WebMethod(operationName = "Delete")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse")
    public void delete();
}
