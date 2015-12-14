# Overview

Prototype code for a pure Java WS-Man collector. 

## TODO
* Better exception handling
** When basic authenticaiton fails
* Pre-emptive authentication when basic auth is enabled

## WS-Man Specifications

The WS-Management specificatons are maintained by the DMTF and made available here: https://www.dmtf.org/standards/wsman

### Notes

* Enumeration context: a session
* CQL: CIM Query Language
* Different versions of WS-MAN include 1.0, 1.1 and 1.2, slight variations
..* 1.0 Service Requires WSMA addressing i.e. http://schemas.xmlsoap.org/ws/2004/08/addressing/fault
..* 1.1 and 1.2 Services suppor WSA-Rec (WS-Addressing W3C Recommendation)
* ResourceURI often acts as a table or a "class,"
* Optional timeout header OperationTimeout. See 6.1 of spec.
* 8.2.3 Optimized enumeration for small results sets
..* Could be useful when collecting

## iDrac WS-Man Specifications

Master page is at http://delltechcenter.com/lc

WSDLs for iDrac6 http://en.community.dell.com/dell-groups/dtcmedia/m/mediagallery/20193729

General WSMAN info http://en.community.dell.com/techcenter/systems-management/w/wiki/4374.how-to-build-and-execute-wsman-method-commands

## Request/Response Examples

### Enumeration request

```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration" xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
   <s:Header>
      <wsa:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action>
      <wsa:To s:mustUnderstand="true">https://127.0.0.1:4443/wsman</wsa:To>
      <wsman:ResourceURI s:mustUnderstand="true">http://schemas.dmtf.org/wbem/wscim/1/*</wsman:ResourceURI>
      <wsa:MessageID s:mustUnderstand="true">uuid:f6e752bc-2668-1668-8002-303180ea2ae8</wsa:MessageID>
      <wsa:ReplyTo>
         <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
      </wsa:ReplyTo>
   </s:Header>
   <s:Body>
      <wsen:Enumerate>
         <wsman:Filter Dialect="http://schemas.microsoft.com/wbem/wsman/1/WQL">select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0</wsman:Filter>
      </wsen:Enumerate>
   </s:Body>
</s:Envelope>
```

### Enumeration response


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration">
   <s:Header>
      <wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>
      <wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>
      <wsa:RelatesTo>uuid:f6e752bc-2668-1668-8002-303180ea2ae8</wsa:RelatesTo>
      <wsa:MessageID>uuid:c6610461-2664-1664-8020-c115cfb5fe14</wsa:MessageID>
   </s:Header>
   <s:Body>
      <wsen:EnumerateResponse>
         <wsen:EnumerationContext>c6595ee1-2664-1664-801f-c115cfb5fe14</wsen:EnumerationContext>
      </wsen:EnumerateResponse>
   </s:Body>
</s:Envelope>
```

### Pull Request


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration" xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
   <s:Header>
      <wsa:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull</wsa:Action>
      <wsa:To s:mustUnderstand="true">https://127.0.0.1:4443/wsman</wsa:To>
      <wsman:ResourceURI s:mustUnderstand="true">http://schemas.dmtf.org/wbem/wscim/1/*</wsman:ResourceURI>
      <wsa:MessageID s:mustUnderstand="true">uuid:f715a46e-2668-1668-8003-303180ea2ae8</wsa:MessageID>
      <wsa:ReplyTo>
         <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
      </wsa:ReplyTo>
   </s:Header>
   <s:Body>
      <wsen:Pull>
         <wsen:EnumerationContext>c6595ee1-2664-1664-801f-c115cfb5fe14</wsen:EnumerationContext>
      </wsen:Pull>
   </s:Body>
</s:Envelope>
```

### Pull Response


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:n1="http://schemas.dell.com/wbem/wscim/1/cim-schema/2/DCIM_PowerSupplyView" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration">
   <s:Header>
      <wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>
      <wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse</wsa:Action>
      <wsa:RelatesTo>uuid:f715a46e-2668-1668-8003-303180ea2ae8</wsa:RelatesTo>
      <wsa:MessageID>uuid:c664ae53-2664-1664-8021-c115cfb5fe14</wsa:MessageID>
   </s:Header>
   <s:Body>
      <wsen:PullResponse>
         <wsen:Items>
            <n1:DCIM_PowerSupplyView>
               <n1:FirmwareVersion>04.15.00</n1:FirmwareVersion>
               <n1:InputVoltage>120</n1:InputVoltage>
               <n1:InstanceID>PSU.Slot.1</n1:InstanceID>
               <n1:PrimaryStatus>1</n1:PrimaryStatus>
               <n1:RedundancyStatus>0</n1:RedundancyStatus>
               <n1:TotalOutputPower>502</n1:TotalOutputPower>
            </n1:DCIM_PowerSupplyView>
         </wsen:Items>
         <wsen:EndOfSequence />
      </wsen:PullResponse>
   </s:Body>
</s:Envelope>
```

### Get Request

```
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
   <s:Header>
      <wsa:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</wsa:Action>
      <wsa:To s:mustUnderstand="true">https://127.0.0.1:4443/wsman</wsa:To>
      <wsman:ResourceURI s:mustUnderstand="true">http://schemas.dell.com/wbem/wscim/1/cim-schema/2/root/dcim/DCIM_ComputerSystem</wsman:ResourceURI>
      <wsa:MessageID s:mustUnderstand="true">uuid:c400f551-26de-16de-8002-303180ea2ae8</wsa:MessageID>
      <wsa:ReplyTo>
         <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
      </wsa:ReplyTo>
      <wsman:SelectorSet>
         <wsman:Selector Name="CreationClassName">DCIM_ComputerSystem</wsman:Selector>
         <wsman:Selector Name="Name">srv:system</wsman:Selector>
      </wsman:SelectorSet>
   </s:Header>
   <s:Body />
</s:Envelope>
```

### Get Response

```
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:n1="http://schemas.dell.com/wbem/wscim/1/cim-schema/2/DCIM_ComputerSystem" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
   <s:Header>
      <wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>
      <wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse</wsa:Action>
      <wsa:RelatesTo>uuid:c400f551-26de-16de-8002-303180ea2ae8</wsa:RelatesTo>
      <wsa:MessageID>uuid:937cf719-26da-16da-8107-c115cfb5fe14</wsa:MessageID>
   </s:Header>
   <s:Body>
      <n1:DCIM_ComputerSystem>
         <n1:AvailableRequestedStates xsi:nil="true" />
         <n1:Caption xsi:nil="true" />
         <n1:CommunicationStatus xsi:nil="true" />
         <n1:CreationClassName>DCIM_ComputerSystem</n1:CreationClassName>
         <n1:Dedicated>0</n1:Dedicated>
         <n1:Description xsi:nil="true" />
         <n1:DetailedStatus xsi:nil="true" />
         <n1:ElementName>Computer System</n1:ElementName>
         <n1:EnabledDefault>2</n1:EnabledDefault>
         <n1:EnabledState>2</n1:EnabledState>
         <n1:HealthState>5</n1:HealthState>
         <n1:IdentifyingDescriptions>CIM:GUID</n1:IdentifyingDescriptions>
         <n1:IdentifyingDescriptions>CIM:Tag</n1:IdentifyingDescriptions>
         <n1:IdentifyingDescriptions>DCIM:ServiceTag</n1:IdentifyingDescriptions>
         <n1:InstallDate xsi:nil="true" />
         <n1:Name>srv:system</n1:Name>
         <n1:NameFormat xsi:nil="true" />
         <n1:OperatingStatus xsi:nil="true" />
         <n1:OperationalStatus>2</n1:OperationalStatus>
         <n1:OperationalStatus>3</n1:OperationalStatus>
         <n1:OtherDedicatedDescriptions xsi:nil="true" />
         <n1:OtherEnabledState xsi:nil="true" />
         <n1:OtherIdentifyingInfo>ANONYMIZED01</n1:OtherIdentifyingInfo>
         <n1:OtherIdentifyingInfo>mainsystemchassis</n1:OtherIdentifyingInfo>
         <n1:OtherIdentifyingInfo>ANONYMIZED02</n1:OtherIdentifyingInfo>
         <n1:PowerManagementCapabilities xsi:nil="true" />
         <n1:PrimaryOwnerContact xsi:nil="true" />
         <n1:PrimaryOwnerName xsi:nil="true" />
         <n1:PrimaryStatus>1</n1:PrimaryStatus>
         <n1:RequestedState>0</n1:RequestedState>
         <n1:ResetCapability xsi:nil="true" />
         <n1:Roles xsi:nil="true" />
         <n1:Status xsi:nil="true" />
         <n1:StatusDescriptions xsi:nil="true" />
         <n1:TimeOfLastStateChange xsi:nil="true" />
         <n1:TransitioningToState>12</n1:TransitioningToState>
      </n1:DCIM_ComputerSystem>
   </s:Body>
</s:Envelope>
```
