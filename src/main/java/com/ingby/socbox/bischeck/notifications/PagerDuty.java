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

public final class PagerDuty implements Notifier, MessageServerInf {
    private final static Logger LOGGER = LoggerFactory
            .getLogger(PagerDuty.class);

    private static HashMap<String, PagerDuty> notificator = new HashMap<String, PagerDuty>();

    private String serviceKey;
    private int connectionTimeout;
    private URL url;
    private final String instanceName;
    private boolean sendMessage;
    private ServiceKeyRouter skr;

    private String defaultServiceKey;


    synchronized public static PagerDuty getInstance(String name) {

        if (!notificator.containsKey(name)) {
            notificator.put(name, new PagerDuty(name));
        }
        return notificator.get(name);
    }

    private PagerDuty(String name) {

        instanceName = name;
        Properties defaultproperties = getNotificationProperties();
        Properties prop = ConfigurationManager.getInstance()
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

        serviceKey = prop.getProperty("service_key",
                defaultproperties.getProperty("service_key"));

        defaultServiceKey = prop.getProperty("default_service_key");

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
        Properties defaultproperties = new Properties();

        defaultproperties
                .setProperty("url",
                        "https://events.pagerduty.com/generic/2010-04-15/create_event.json");
        defaultproperties.setProperty("service_key", "");
        defaultproperties.setProperty("default_service_key", "");
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
    public void sendAlert(ServiceTO serviceTo) {
        Writer message = new StringWriter();

        final String key = skr.getServiceKey(serviceTo.getHostName(),
                serviceTo.getServiceName());
        if (key == null) {
            LOGGER.error(
                    "Service for {} do not have a service key defined. Will not be sent by instance {}.",
                    Util.fullQouteHostServiceName(serviceTo.getHostName(),
                            serviceTo.getServiceName()), instanceName);
            return;
        }

        NagiosUtil nu = new NagiosUtil(false);
        new JSONBuilder(message)
                .object()
                .key("service_key")
                .value(key)
                .key("incident_key")
                .value(serviceTo.getIncidentKey())
                .key("event_type")
                .value("trigger")
                .key("description")
                .value(nu.createNagiosMessage(serviceTo, true))
                .key("details")
                .object()
                .key("servicedef")
                .value(serviceTo.getHostName() + "-"
                        + serviceTo.getServiceName()).key("state")
                .value(serviceTo.getLevel().toString()).endObject().endObject();

        JSONObject json = null;
        try {
            if (sendMessage) {
                json = sendMessage(message);
            }
        } catch (IOException e) {
            LOGGER.error("Sedning trigger message to {} failed.", instanceName,
                    e);
        }

        LOGGER.info("Alert message to {} : {}", instanceName,
                message.toString());

        success(json, serviceTo.getIncidentKey());
    }

    @Override
    public void sendResolve(ServiceTO serviceTo) {
        Writer message = new StringWriter();

        String key = skr.getServiceKey(serviceTo.getHostName(),
                serviceTo.getServiceName());
        if (key == null) {
            LOGGER.error(
                    "Service for {} do not have a service key defined. Will not be sent by instance {}.",
                    Util.fullQouteHostServiceName(serviceTo.getHostName(),
                            serviceTo.getServiceName()), instanceName);
            return;
        }

        NagiosUtil nu = new NagiosUtil(false);

        new JSONBuilder(message).object().key("service_key").value(key)
                .key("incident_key").value(serviceTo.getIncidentKey())
                .key("event_type").value("resolve").key("description")
                .value(nu.createNagiosMessage(serviceTo, true)).endObject();

        JSONObject json = null;
        try {
            if (sendMessage) {
                json = sendMessage(message);
            }
        } catch (IOException e) {
            LOGGER.error("Sedning resolve message to {} failed.", instanceName,
                    e);
        }
        LOGGER.info("Resolve message to {} : {}", instanceName,
                message.toString());

        success(json, serviceTo.getIncidentKey());
    }

    private JSONObject sendMessage(final Writer message) throws IOException {
        String payload = message.toString();
        HttpURLConnection conn = null;

        final String timerName = instanceName + "_sendTimer";
        final Timer timer = MetricsManager.getTimer(PagerDuty.class, timerName);
        final Timer.Context context = timer.time();

        JSONObject json = null;

        try {
            conn = createHTTPConnection(payload);

            if (!postHTTP(conn, payload)) {
                return null;
            }

            json = responseHTTP(conn);

        } finally {
            long duration = context.stop() / 1000000;
            LOGGER.debug("PagerDuty for {} send execute: {} ms", instanceName,
                    duration);
        }

        return json;

    }

    private boolean success(JSONObject json, String incidentKey) {
        if (json != null) {

            if ("success".equals(json.getString("status"))
                    && incidentKey.equals(json.getString("incident_key"))) {
                return true;
            }

            if (!incidentKey.equals(json.getString("incident_key"))) {
                LOGGER.error(
                        "Response incident key, {}, do not match expected, {} for instance {}",
                        json.getString("incident_key"), incidentKey,
                        instanceName);
                return false;
            }
        }
        return false;
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

        JSONObject json = null;
        if (sb.toString().isEmpty()) {
            LOGGER.error("HTTPS response for instance {} returned no data",
                    instanceName);
        } else {
            json = (JSONObject) JSONSerializer.toJSON(sb.toString());
            if (json == null) {
                LOGGER.error(
                        "PagerDuty returned null json object for message {} for instance {}",
                        sb.toString(), instanceName);
            } else if (json.size() != 3 || !json.has("status")
                    || !json.has("incident_key")) {
                LOGGER.error(
                        "PagerDuty returned faulty json message for {} for instance {}",
                        sb.toString(), instanceName);
            }
        }

        return json;
    }

    private boolean postHTTP(HttpURLConnection conn, final String payload)
            throws IOException {

        try (OutputStreamWriter wr = new OutputStreamWriter(
                conn.getOutputStream())) {

            wr.write(payload);
            wr.flush();
            int returnStatus = conn.getResponseCode();
            if (returnStatus != HttpURLConnection.HTTP_OK) {
                if (returnStatus == HttpURLConnection.HTTP_BAD_REQUEST) {
                    LOGGER.error(
                            "PagerDuty responded with {} for instance {}, check Bischeck configuration",
                            returnStatus, instanceName);
                    return false;
                } else if (returnStatus == HttpURLConnection.HTTP_FORBIDDEN) {
                    LOGGER.error(
                            "PagerDuty responded with {} for instance {}, probably making to many API calls to PagerDuty",
                            returnStatus, instanceName);
                    return false;
                } else if (returnStatus >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    LOGGER.error(
                            "PagerDuty responded with {} for instance {}, check PagerDuty server status and vaildate your account settings",
                            returnStatus, instanceName);
                    return false;
                }
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
        conn.setRequestProperty("Content-Length",
                "" + Integer.toString(payload.getBytes().length));

        conn.setRequestProperty("User-Agent", "bischeck");
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
    public void onMessage(ServiceTO message) {
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
