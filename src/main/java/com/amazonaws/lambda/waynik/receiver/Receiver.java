package com.amazonaws.lambda.waynik.receiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Receiver implements RequestStreamHandler {

	JSONParser parser = new JSONParser();
	LambdaLogger logger;

	private String TOPIC_EMERGENCY = "arn:::emergency";
	private String TOPIC_FIRST_CHECKIN = "arn:::first_checkin";
	private String TOPIC_ENTERING_NEW_COUNTRY = "arn:::entering_new_country";

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ProxyWithStream");

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();

        String responseCode = "200";

        try {
            JSONObject event = (JSONObject)parser.parse(reader);

            if (event.get("queryStringParameters") == null) {
             throw new Exception("no query params sent");
            }

            JSONObject requestData = (JSONObject)event.get("queryStringParameters");
            String email = (String)requestData.get("email");
            String token = (String)requestData.get("token");

            UserModel userModel = new UserModel();
            User user = userModel.getByEmail(email, token);

			requestData.put("user_id", Integer.toString(user.getId()));

			// must be done before this checkin is created.
			sendEmergencyEventIfNeeded(requestData, user);

			requestData.put("country", Utils.getCountry(requestData));

            CheckinModel checkinModel = new CheckinModel();
            int checkinId = checkinModel.create(requestData);

            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "data received successfully");
            responseBody.put("data", requestData);
            responseBody.put("id", checkinId);

            JSONObject headerJson = new JSONObject();
            // headerJson.put("x-custom-response-header", "my custom response header value");

            responseJson.put("statusCode", responseCode);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

            sendFirstCheckinNotificationsIfNeeded(user);
            sendCountryChangedNotificationsIfNeeded(user);


        } catch(Exception ex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", ex);
            responseJson.put("stackTrace", Utils.getStackTrace(ex));

        }

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }

    private void sendEmergencyEventIfNeeded(JSONObject thisCheckin, User user) throws Exception {
    	CheckinModel checkinModel = new CheckinModel();
    	boolean lastCheckinWasEmergency = checkinModel.wasLastCheckinEmergencyForUser(user.getId());

    	if (!lastCheckinWasEmergency && Utils.convertToBoolean((String)thisCheckin.get("emergency"))) {
    		Utils.sendSnsRequest(TOPIC_EMERGENCY, Utils.getJsonPayload(user.getId(), "We received your emergency request and we are working on it!"));
    	}
    }

    private void sendFirstCheckinNotificationsIfNeeded(User user) throws Exception
    {
    	if (user.hasCheckins()) {
    		return;
    	}

    	sendFirstCheckinEmail(user);
		sendFirstCheckinEvent(user);

    }

    private void sendFirstCheckinEmail(User user) throws Exception
    {
    	String to = user.getEmail();

    	// The configuration set to use for this email. If you do not want to use a
    	// configuration set, comment the following variable and the
    	// .withConfigurationSetName(CONFIGSET); argument below.
    	//static final String CONFIGSET = "ConfigSet";

    	String subject = "Waynik received your first check-in!";

    	// The HTML body for the email.
    	String htmlBody = "<p>Congratulations! We have received your first Waynik location check-in and your set-up is complete. Time to get back out there and start exploring...</p>"
			+ "<p><strong>"
			+ "Quick Tips:</strong></p>"
			+ "<ol><li><strong>How Waynik Works</strong><br />"
			+ "Waynik provides a 24/7 self-monitoring location service done discretely in the background of your mobile device and only requires extremely limited amounts of cellular connectivity to stay up to date."
			+ "</li><li>"
			+ "<strong>Keep the Waynik App Running</strong><br />"
			+ "For best results, keep the Waynik application running in the background of your phone (don't kill the application)."
			+ "</li><li>"
			+ "<strong>Activate Application in an Emergency</strong><br />"
			+ "In an emergency, activate the alert button within app (screenshot below) or by calling the Waynik emergency phone line +1 (202) 643-1047"
			+ "<br /><img width='400' src='https://www.waynik.com/admin/images/first-checkin-email-1.png' />"
			+ "</li><li>"
			+ "<strong>Emergency Response</strong><br />"
			+ "In an emergency, Waynik responders execute a pre-defined escalation process based on your unique registration details and seek to communicate your real-time location to the appropriate emergency responders anywhere in the world."
			+ "</li></ol>";

    	String textBody = "Congratulations! We have received your first Waynik location check-in and your set-up is complete. Time to get back out there and start exploring..."
			+ "\n\nQuick Tips:"
			+ "\n1. How Waynik Works"
			+ "\nWaynik provides a 24/7 self-monitoring location service done discretely in the background of your mobile device and only requires extremely limited amounts of cellular connectivity to stay up to date."
			+ "\n\n2. Keep the Waynik App Running"
			+ "\nFor best results, keep the Waynik application running in the background of your phone (don't kill the application)."
			+ "\n\n3. Activate Application in an Emergency"
			+ "\nIn an emergency, activate the alert button within app (screenshot below) or by calling the Waynik emergency phone line +1 (202) 643-1047"
			+ "\n\nScreenshot: https://www.waynik.com/admin/images/first-checkin-email-1.png"
			+ "\n\n4. Emergency Response"
			+ "\nIn an emergency, Waynik responders execute a pre-defined escalation process based on your unique registration details and seek to communicate your real-time location to the appropriate emergency responders anywhere in the world.";

    	List<String> toAddresses = new ArrayList<>();
        toAddresses.add(to);

    	Utils.sendEmail(toAddresses, subject, htmlBody, textBody);
    }

    private void sendFirstCheckinEvent(User user)
    {
    	Utils.sendSnsRequest(TOPIC_FIRST_CHECKIN, Utils.getJsonPayload(user.getId(), "Hooray, we got your first update!"));
    }

    private void sendCountryChangedNotificationsIfNeeded(User user) throws Exception
    {
    	// check country changed since last checkin? if first checkin, send as well.
    	CheckinModel checkinModel = new CheckinModel();

    	Checkin mostRecentCheckin;
    	try {
    		mostRecentCheckin = checkinModel.getMostRecentForUser(user);
    	} catch (Exception ex) {
    		logger.log("checkinModel getMostRecentForUser lookup error. Error message: "
    		          + ex.getMessage());
    		return;
    	}

    	Checkin precedingCheckin;
    	try {
    		precedingCheckin = checkinModel.getSecondMostRecentForUser(user);
    	} catch (Exception ex) {
    		precedingCheckin = null;

    	}

    	// Check that there are two countries present, that we didn't botch the country lookup.
    	if (
			precedingCheckin != null &&
			(mostRecentCheckin.getCountry() == null || precedingCheckin.getCountry() == null)
    	) {
    		return;
    	}

    	boolean isFirstCheckin = precedingCheckin == null;
    	boolean countriesAreDifferent = precedingCheckin != null && !mostRecentCheckin.getCountry().equals(precedingCheckin.getCountry());
    	if (
			isFirstCheckin
			|| countriesAreDifferent
    	) {
    		sendCountryChangedEvent(user, mostRecentCheckin);
    		sendCountryChangedEmailToMichael(user, mostRecentCheckin);
    	}

    }

    private void sendCountryChangedEmailToMichael(User user, Checkin mostRecentCheckin) throws Exception
    {
    	// send email to Michael to send an email to the user with this info!

    	List<String> toAddresses = new ArrayList<>();
    	toAddresses.add("mbell@waynik.com");
    	toAddresses.add("dan.degreef@gmail.com");

    	// The configuration set to use for this email. If you do not want to use a
    	// configuration set, comment the following variable and the
    	// .withConfigurationSetName(CONFIGSET); argument below.
    	//static final String CONFIGSET = "ConfigSet";

    	String subject = "Waynik user " + user.getName() + " has entered a new country";

    	// The HTML body for the email.
    	String htmlBody = "Hey! " + user.getName() + " has entered " + mostRecentCheckin.getCountry()
				+ ". Please send them information about this country. Their email is " + user.getEmail();

    	String textBody = htmlBody;

    	Utils.sendEmail(toAddresses, subject, htmlBody, textBody);
    }

    private void sendCountryChangedEvent(User user, Checkin mostRecentCheckin) throws Exception
    {
    	String country = mostRecentCheckin.getCountry();
		country = CountryFormatter.format(country);
    	String message = "Welcome to " + country + "! Keep an eye out for an email from us with some helpful information.";

    	Utils.sendSnsRequest(TOPIC_ENTERING_NEW_COUNTRY, Utils.getJsonPayload(user.getId(), message));
    }
}
