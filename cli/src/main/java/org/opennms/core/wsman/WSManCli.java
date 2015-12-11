package org.opennms.core.wsman;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.opennms.core.wsman.WSManEndpoint.WSManVersion;
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

    @Option(name="-w", usage="server version")
    WSManVersion serverVersion = WSManVersion.WSMAN_1_2;

    @Option(name="-resourceUri", usage="resource uri")
    private String resourceUri = WSManConstants.CIM_ALL_AVAILABLE_CLASSES;

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
            if( arguments.isEmpty() ) {
                throw new CmdLineException(parser, Messages.NO_ARGUMENT);
            }
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

        for (String wql : arguments) {
            LOG.info("Enumerating and pulling on '{}' with '{}'...", resourceUri, wql);
            List<Node> nodes = client.enumerateAndPullUsingFilter(WSManConstants.XML_NS_WQL_DIALECT, wql, resourceUri);
            LOG.info("Succesfully pulled {} nodes.", nodes.size());

            // Dump the list of nodes to stdout
            for (Node node : nodes) {
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
    }
}

