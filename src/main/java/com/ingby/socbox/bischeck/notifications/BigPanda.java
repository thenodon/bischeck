package com.ingby.socbox.bischeck.notifications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.servers.MessageServerInf;
import com.ingby.socbox.bischeck.service.ServiceTO;

public final class BigPanda implements Notifier, MessageServerInf {
    private final static Logger LOGGER = LoggerFactory
            .getLogger(BigPanda.class);

    private static Map<String, BigPanda> notificator = new HashMap<String, BigPanda>();

    private final int connectionTimeout;
    private final URL url;
    private final String instanceName;
    private final boolean sendMessage;
    private final ServiceKeyRouter skr;

    synchronized public static BigPanda getInstance(final String name) {

        if (!notificator.containsKey(name)) {
            notificator.put(name, new BigPanda(name));
        }
        return notificator.get(name);
    }

    private BigPanda(final String name) {

        instanceName = name;
        final Properties defaultproperties = getNotificationProperties();
        final Properties prop = ConfigurationManager.getInstance()
                .getServerProperiesByName(name);

        final String urlName = prop.getProperty("url",
                defaultproperties.getProperty("url"));

        try {
            url = new URL(urlName);
        } catch (MalformedURLException e) {
            LOGGER.error("{} - The url {} is not correctly formated",
                    instanceName, urlName, e);
            throw new IllegalArgumentException(e);
        }

        final String serviceKey = prop.getProperty("app_key",
                defaultproperties.getProperty("app_key"));

        final String defaultServiceKey = prop.getProperty("default_app_key");

        skr = new ServiceKeyRouter(serviceKey, defaultServiceKey);

        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

        sendMessage = Boolean.parseBoolean(prop.getProperty("send",
                defaultproperties.getProperty("send")));

    }

    /**
     * Return properties with default values
     * 
     * @return default properties
     */
    public static Properties getNotificationProperties() {
        final Properties defaultproperties = new Properties();

        defaultproperties.setProperty("url",
                "https://api.bigpanda.io/data/v2/alerts");
        defaultproperties.setProperty("app_key", "");
        defaultproperties.setProperty("default_app_key", "");
        defaultproperties.setProperty("connectionTimeout", "5000");
        defaultproperties.setProperty("send", "true");

        return defaultproperties;
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param name
     *            of the server instance
     */
    synchronized public static void unregister(final String name) {
        notificator.remove(name);
    }

    @Override
    public void sendAlert(final ServiceTO serviceTo) {
        try {
            final String message = send(serviceTo);
            LOGGER.info("Alert message to {} : {}", instanceName,
                    message.toString());
        } catch (IOException e) {
            LOGGER.error("Sedning trigger message to {} failed.", instanceName,
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.error(
                    "Service for {} do not have a app key defined. Will not be sent by instance {}.",
                    Util.fullQouteHostServiceName(serviceTo.getHostName(),
                            serviceTo.getServiceName()), instanceName, e);
        }
    }

    @Override
    public void sendResolve(final ServiceTO serviceTo) {
        try {
            String message = send(serviceTo);
            LOGGER.info("Resolve message to {} : {}", instanceName,
                    message.toString());
        } catch (IOException e) {
            LOGGER.error("Sending resolve message to {} failed.", instanceName,
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.error(
                    "Service for {} do not have a service key defined. Will not be sent by instance {}.",
                    Util.fullQouteHostServiceName(serviceTo.getHostName(),
                            serviceTo.getServiceName()), instanceName, e);
        }
    }

    private String send(final ServiceTO serviceTo) throws IOException,
            IllegalArgumentException {

        final String key = skr.getServiceKey(serviceTo.getHostName(),
                serviceTo.getServiceName());
        if (key == null) {
            throw new IllegalArgumentException();
        }

        final Long bptimestamp = unixEpoch(System.currentTimeMillis());
        final Writer message = new StringWriter();

        NagiosUtil nu = new NagiosUtil(false);

        new JSONBuilder(message).object().key("app_key").value(key)
                .key("status")
                .value(serviceTo.getLevel().toString().toLowerCase())
                .key("host").value(serviceTo.getHostName())
                .key("timestamp")
                .value(bptimestamp)
                // Need this in notificationData
                .key("description").value(nu.createNagiosMessage(serviceTo))
                .key("check")
                .value(serviceTo.getServiceName())
                // .key("cluster")
                // .value(notificationData.get("host"))
                .key("incident_key").value(serviceTo.getIncidentKey())
                .endObject();

        if (sendMessage) {
            sendMessage(message);
        }

        return message.toString();
    }

    private Long unixEpoch(final long currentTimeMillis) {
        return currentTimeMillis / 1000L;
    }

    /**
     * Send the message to BigPanda
     * 
     * @param message
     *            the message to be POST
     * @return json object or null if post failed
     * @throws IOException
     *             if connection can not be created, can not send the message or
     *             can not read response
     */
    private JSONObject sendMessage(final Writer message) throws IOException {
        final String payload = message.toString();
        LOGGER.debug("Message is : {}", payload);
        HttpURLConnection conn = null;

        final String timerName = instanceName + "_sendTimer";
        final Timer timer = MetricsManager.getTimer(BigPanda.class, timerName);
        final Timer.Context context = timer.time();

        JSONObject json = null;

        try {
            conn = createHTTPConnection(payload);
            if (postHTTP(conn, payload)) {
                json = responseHTTP(conn);
            }
        } finally {
            final long duration = context.stop() / 1000000;
            LOGGER.debug("BigPanda for {} send execute: {} ms", instanceName,
                    duration);
        }

        return json;

    }

    private JSONObject responseHTTP(final HttpURLConnection conn)
            throws IOException {
        StringBuilder sb = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                conn.getInputStream()))) {

            sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        LOGGER.debug("HTTPS repsonse : {}", sb);

        JSONObject json = null;
        if (sb.toString().isEmpty()) {
            LOGGER.error("HTTPS response for instance {} returned no data",
                    instanceName);
        } else {
            json = (JSONObject) JSONSerializer.toJSON(sb.toString());
            if (json == null) {
                LOGGER.error(
                        "BigPanda returned null json object for message {} for instance {}",
                        sb.toString(), instanceName);
            } else if (!json.has("response")) {
                LOGGER.error(
                        "BigPanda returned faulty json message for {} for instance {}",
                        sb.toString(), instanceName);
            }
        }

        return json;
    }

    private boolean postHTTP(final HttpURLConnection conn, final String payload)
            throws IOException {

        try (OutputStreamWriter wr = new OutputStreamWriter(
                conn.getOutputStream())) {

            wr.write(payload);
            wr.flush();
            final int returnStatus = conn.getResponseCode();
            if (returnStatus != HttpURLConnection.HTTP_OK) {
                if (returnStatus == HttpURLConnection.HTTP_BAD_REQUEST) {
                    LOGGER.error(
                            "BigPanda responded with {} for instance {}, check Bischeck configuration",
                            returnStatus, instanceName);
                } else if (returnStatus == HttpURLConnection.HTTP_FORBIDDEN) {
                    LOGGER.error(
                            "BigPanda responded with {} for instance {}, probably making to many API calls to BigPanda",
                            returnStatus, instanceName);
                } else if (returnStatus >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    LOGGER.error(
                            "BigPanda responded with {} for instance {}, check BigPanda server status and vaildate your account settings",
                            returnStatus, instanceName);
                } else {
                    LOGGER.error(
                            "BigPanda responded with {} for instance {}, this is a error not defined by the protocol",
                            returnStatus, instanceName);
                }
                return false;
            }
        }

        return true;
    }

    private HttpURLConnection createHTTPConnection(final String payload)
            throws IOException {

        LOGGER.debug("{} - Message: {}", instanceName, payload);
        HttpsURLConnection conn;

        conn = (HttpsURLConnection) url.openConnection();

        conn.setDoOutput(true);

        conn.setRequestMethod("POST");

        conn.setConnectTimeout(connectionTimeout);
        if (payload != null && payload.getBytes() != null
                && payload.getBytes().length != 0) {
            conn.setRequestProperty("Content-Length",
                    "" + Integer.toString(payload.getBytes().length));
        }
        conn.setRequestProperty("User-Agent", "bischeck");
        // TODO check if this unique per key
        conn.setRequestProperty("Authorization",
                "Bearer 2d1dad9a41fa48e2b3fe8f2d304343b5");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8");
        LOGGER.debug("Open connection for instance {} : {}", instanceName,
                conn.toString());
        return conn;
    }

    @Override
    public void onMessage(final ServiceTO message) {
        if (message.isResolved()) {
            sendResolve(message);
        } else {
            sendAlert(message);
        }
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void unregister() {
        // TODO Auto-generated method stub

    }

}
