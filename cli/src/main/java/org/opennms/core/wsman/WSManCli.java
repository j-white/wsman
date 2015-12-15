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
 */
package org.opennms.core.wsman;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.MapOptionHandler;
import org.opennms.core.wsman.cxf.CXFWSManClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WSManCli {
    private static Logger LOG = LoggerFactory.getLogger(WSManCli.class);

    @Option(name="-r", usage="remote url", metaVar="url", required=true)
    private String remoteUrl;

    @Option(name="-u", usage="username")
    private String username;

    @Option(name="-p", usage="password")
    private String password;

    @Option(name="-strictSSL", usage="ssl certificate verification")
    private boolean strictSSL;

    public static enum WSManOperation {
        GET,
        ENUM
    }

    @Option(name="-o", usage="operation")
    WSManOperation operation = WSManOperation.ENUM;

    @Option(name="-resourceUri", usage="resource uri")
    private String resourceUri = WSManConstants.CIM_ALL_AVAILABLE_CLASSES;

    @Option(name="-w", usage="server version")
    WSManVersion serverVersion = WSManVersion.WSMAN_1_2;

    @Option(name="-s", handler=MapOptionHandler.class)
    Map<String,String> selectors;

    @Argument
    private List<String> arguments = new ArrayList<String>();

    private WSManClientFactory clientFactory = new CXFWSManClientFactory();
    
    public static void main(String[] args) {
        new WSManCli().doMain(args);
    }

    public void doMain(String[] args) {
        ParserProperties parserProperties = ParserProperties.defaults()
                .withUsageWidth(120);

        CmdLineParser parser = new CmdLineParser(this, parserProperties);

        try {
            parser.parseArgument(args);
        } catch( CmdLineException e ) {
            System.err.println("java -jar wsman4j.jar [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            e.printStackTrace();
            return;
        }

        URL url;
        try {
            url = new URL(remoteUrl);
        } catch (MalformedURLException e) {
            LOG.error("Invalid URL: {}", remoteUrl, e);
            return;
        }

        WSManEndpoint.Builder builder = new WSManEndpoint.Builder(url)
                .withStrictSSL(strictSSL)
                .withServerVersion(serverVersion);
        if (username != null && password != null) {
            builder.withBasicAuth(username, password);
        }
        WSManEndpoint endpoint = builder.build();
        LOG.info("Using endpoint: {}", endpoint);
        WSManClient client = clientFactory.getClient(endpoint);
        
        if (operation == WSManOperation.ENUM) {
            
            if (arguments.isEmpty()) {
                LOG.info("Enumerating and pulling on '{}'...", resourceUri);
                List<Node> nodes = client.enumerateAndPull(resourceUri, true);
                LOG.info("Succesfully pulled {} nodes.", nodes.size());

                // Dump the list of nodes to stdout
                for (Node node : nodes) {
                    dumpNodeToStdout(node);
                }
            } else {
                for (String wql : arguments) {
                    LOG.info("Enumerating and pulling on '{}' with '{}'...", resourceUri, wql);
                    List<Node> nodes = client.enumerateAndPullUsingFilter(WSManConstants.XML_NS_WQL_DIALECT, wql, resourceUri, true);
                    LOG.info("Succesfully pulled {} nodes.", nodes.size());

                    // Dump the list of nodes to stdout
                    for (Node node : nodes) {
                        dumpNodeToStdout(node);
                    }
                }
            }
        } else if (operation == WSManOperation.GET) {
            LOG.info("Issuing a GET on '{}' with selectors {}", resourceUri, selectors);
            Node node = client.get(selectors, resourceUri);
            LOG.info("GET successful.");

            // Dump the node to stdout
            dumpNodeToStdout(node);
        }
    }
    
    private static void dumpNodeToStdout(Node node) {
        System.out.printf("%s (%s)\n", node.getLocalName(), node.getNamespaceURI());
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            
            if (child.getLocalName() == null) {
                continue;
            }

            System.out.printf("\t%s = %s\n", child.getLocalName(), child.getTextContent());
        }
    }
}

