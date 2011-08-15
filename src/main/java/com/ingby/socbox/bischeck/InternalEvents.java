package com.ingby.socbox.bischeck;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.Level;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.service.Service;

public class InternalEvents {
	
	static Logger  logger = Logger.getLogger(InternalEvents.class);
	
	public static void send(String nagiosservice, Level level, Service service, String exception) {
	
		logger.info("Send internal service notification:" + level + " " + service.getServiceName() +" - " + service.getConnectionUrl() + " - " + exception);

		NagiosSettings settings = ServerConfig.getNagiosConnection();
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);

		MessagePayload payload = new MessagePayloadBuilder()
		.withHostname(ServerConfig.getProperties().getProperty("bischeckserver"))
		.withLevel(level)
		.withServiceName(nagiosservice)
		.withMessage(level + " " + service.getServiceName() +" - " + service.getConnectionUrl() + " - " + exception)
		.create();

		
		try {
			long start = TimeMeasure.start();
			sender.send(payload);
			long duration = TimeMeasure.stop(start);
			logger.info("Nsca send execute: " + duration + " ms");
		} catch (NagiosException ne) {
			logger.warn("Nsca server error - " + ne);
		} catch (IOException ne) {
			logger.error("Network error - check nsca server and that service is started - " + ne);
		}

	}

}
