/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
 */

package com.ingby.socbox.bischeck.servers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * Nagios server integration over http based NRDP protocol. <br>
 * The message has the following structure when sent from Bischeck to NRDP. <br>
 * <code>
 * <?xml version='1.0'?>"<br>
 * &nbsp;&nbsp;<checkresults>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<checkresult type=\"service\" checktype=\"1\">"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<hostname>YOUR_HOSTNAME</hostname>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<servicename>YOUR_SERVICENAME</servicename>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<state>0</state>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<output>OK|perfdata=1.00;5;10;0</output>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</checkresult>"<br>
 * &nbsp;&nbsp;</checkresults>"<br>
 * <code>
 */

public final class NRDPBatchServer extends ServerBatchAbstract<List<ServiceTO>> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(NRDPBatchServer.class);

    private static Map<String, NRDPBatchServer> servers = new HashMap<String, NRDPBatchServer>();

    private NagiosUtil nagutil = new NagiosUtil();

    private final String urlstr;
    private final String cmd;
    private final Integer connectionTimeout;
    private URL url;

    private NRDPBatchServer(String instanceName, Properties prop) {
        super(instanceName, prop);
        Properties defaultproperties = getServerProperties();
        String hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));

        Integer port = Integer.parseInt(prop.getProperty("port",
                defaultproperties.getProperty("port")));

        String password = prop.getProperty("password",
                defaultproperties.getProperty("password"));

        String path = prop.getProperty("path",
                defaultproperties.getProperty("path"));

        Boolean ssl = Boolean.valueOf(prop.getProperty("ssl",
                defaultproperties.getProperty("ssl")));

        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

        String protocol = "http://";
        if (ssl) {
            protocol = "https://";
        }

        urlstr = protocol + hostAddress + ":" + port + "/" + path + "/";
        cmd = "token=" + password + "&cmd=submitcheck&XMLDATA=";
        try {
            url = new URL(urlstr);
            LOGGER.debug("URL {}", urlstr);
        } catch (MalformedURLException e) {
            LOGGER.error("{} - The url {} is not correctly formated",
                    instanceName, urlstr, e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Retrieve the Server object. The method is invoked from class
     * ServerExecutor execute method. The created Server object is placed in the
     * class internal Server object list.
     * 
     * @param name
     *            the name of the configuration in server.xml like
     *            {@code &lt;server name="my"&gt;}
     * @return Server object
     */
    synchronized public static ServerInf<ServiceTO> getInstance(
            String instanceName) {

        if (!servers.containsKey(instanceName)) {
            Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(instanceName);
            servers.put(instanceName, new NRDPBatchServer(instanceName, prop));
        }
        return servers.get(instanceName);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param instanceName
     *            of the server instance
     */
    synchronized public static void unregister(String instanceName) {
        getInstance(instanceName).unregister();
    }

    @Override
    synchronized public void unregister() {
        LOGGER.info("{} - Unregister called for", instanceName);
        super.unregister();
        servers.remove(instanceName);
    }

    @Override
    public void send(List<ServiceTO> serviceToList) throws ServerException {

        String xml = xmlNRDPFormat(serviceToList);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(ServerUtil.log(instanceName, serviceToList, xml));
        }
        
        connectAndSend(xml);
    }

    private void connectAndSend(String xml) throws ServerException {

        HttpURLConnection conn = null;
        OutputStreamWriter wr = null;

        try {
            LOGGER.debug("{} - Url: {}", instanceName, urlstr);
            String payload = cmd + xml;
            conn = createHTTPConnection(payload);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(payload);
            wr.flush();

            /*
             * Look for status != 0 by building a DOM to parse
             * <status>0</status> <message>OK</message>
             */

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder dBuilder = null;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                LOGGER.error("{} - Could not get a doc builder", instanceName,
                        e);
                return;
            }

            /*
             * Getting the value for status and message tags
             */
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));) {

                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                InputStream is = new ByteArrayInputStream(sb.toString()
                        .getBytes("UTF-8"));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("NRDP return string - {}",
                            convertStreamToString(is));
                    is.reset();
                }

                Document doc = null;

                doc = dBuilder.parse(is);

                doc.getDocumentElement().normalize();
                String rootNode = doc.getDocumentElement().getNodeName();
                NodeList responselist = doc.getElementsByTagName(rootNode);
                String result = (String) ((Element) responselist.item(0))
                        .getElementsByTagName("status").item(0).getChildNodes()
                        .item(0).getNodeValue().trim();

                LOGGER.debug("NRDP return status is: {}", result);

                if (!"0".equals(result)) {
                    String message = (String) ((Element) responselist.item(0))
                            .getElementsByTagName("message").item(0)
                            .getChildNodes().item(0).getNodeValue().trim();
                    LOGGER.error(
                            "{} - nrdp returned message \"{}\" for xml: {}",
                            instanceName, message, xml);
                }
            } catch (SAXException e) {
                LOGGER.error("{} - Could not parse response xml", instanceName,
                        e);
            }

        } catch (IOException e) {
            LOGGER.error(
                    "{} - Network error - check nrdp server and that service is started",
                    instanceName, e);
            throw new ServerException(e);
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException ignore) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private HttpURLConnection createHTTPConnection(String payload)
            throws IOException {

        LOGGER.debug("{} - Message: {}", instanceName, payload);
        HttpURLConnection conn;

        conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);

        conn.setRequestMethod("POST");

        conn.setConnectTimeout(connectionTimeout);
        conn.setRequestProperty("Content-Length",
                "" + Integer.toString(payload.getBytes().length));

        conn.setRequestProperty("User-Agent", "bischeck");
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8");
        return conn;
    }

    /**
     * Format the xml to be sent
     * 
     * @param level
     *            the Nagios state level
     * @param hostname
     * @param servicename
     * @param serviceOutput
     *            the performance string
     * @return the formated xml
     */
    private String xmlNRDPFormat(List<ServiceTO> serviceToList) {
        StringBuilder strbuf = new StringBuilder();

        strbuf.append("<?xml version='1.0' encoding='utf-8'?>");
        strbuf.append("<checkresults>");

        for (ServiceTO serviceTo : serviceToList) {
            NAGIOSSTAT level = serviceTo.getLevel();

            String hostname = serviceTo.getHostName();
            String servicename = serviceTo.getServiceName();
            String serviceOutput = nagutil.createNagiosMessage(serviceTo);

            // Check encoding and character set and how it works out
            strbuf.append("<checkresult type='service'>");
            strbuf.append("<hostname>").append(hostname).append("</hostname>");
            strbuf.append("<servicename>").append(servicename)
                    .append("</servicename>");
            strbuf.append("<state>").append(level.val()).append("</state>");
            strbuf.append("<output>")
                    .append(StringEscapeUtils.escapeHtml(serviceOutput))
                    .append("</output>");
            strbuf.append("</checkresult>");
        }
        strbuf.append("</checkresults>");

        String utfenc = null;
        try {
            utfenc = URLEncoder.encode(strbuf.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("{} - Unsupported encoding of xml: {}", instanceName,
                    strbuf.toString(), e);
        }
        return utfenc;
    }

    @SuppressWarnings("resource")
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress", "localhost");
        defaultproperties.setProperty("port", "80");
        defaultproperties.setProperty("path", "nrdp");
        defaultproperties.setProperty("password", "");
        defaultproperties.setProperty("ssl", "false");
        defaultproperties.setProperty("connectionTimeout", "5000");

        return defaultproperties;
    }
}
