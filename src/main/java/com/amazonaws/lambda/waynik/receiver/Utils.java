package com.amazonaws.lambda.waynik.receiver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

public class Utils {

	private static String WAYNIK_EVENTS_SECRET_KEY = "secret";

	private static String AWS_SNS_KEY = "";
	private static String AWS_SNS_SECRET = "/8";
	private static String AWS_REGION_CODE = "us-west-2";

	private static String AWS_SES_KEY = "";
	private static String AWS_SES_SECRET = "";

	/**
     * needed for apple iOS using weird values for booleans.
     * @param value
     * @return
     */
    public static boolean convertToBoolean(String value) throws Exception
    {
    	if (
    		value == null
    		|| "false".equalsIgnoreCase(value)
			|| "off".equalsIgnoreCase(value)
			|| "no".equalsIgnoreCase(value)
			|| "0".equalsIgnoreCase(value)
		) {
			return false;
		}
    	return true;
    }

    public static String getStackTrace(Throwable aThrowable) {
	    Writer result = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(result);
	    aThrowable.printStackTrace(printWriter);
	    return result.toString();
    }

    public static PublishResult sendSnsRequest(String topic, String json) {
        AmazonSNS snsClient = AmazonSNSClient.builder()
				.withRegion(AWS_REGION_CODE)
				.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_SNS_KEY, AWS_SNS_SECRET)))
				.build();
		PublishRequest publishRequest = new PublishRequest(topic, json);
		PublishResult publishResult = snsClient.publish(publishRequest);
		//print MessageId of message published to SNS topic
		return publishResult;
    }

    public static String getCountry(JSONObject checkin) throws Exception
	{
		GeoApiContext context = new GeoApiContext.Builder()
		    .apiKey("AIzaSyB2mi7rSEn4zhhPs21oacNp7WN4FB5AG2Y")
		    .build();
		GeocodingResult[] results =  GeocodingApi.reverseGeocode(context, new LatLng(Double.parseDouble((String)checkin.get("latitude")), Double.parseDouble((String)checkin.get("longitude")))).await();

		for (AddressComponent component : results[0].addressComponents) {
			if (component.types[0] == AddressComponentType.COUNTRY) {
				return component.longName;
			}
		}
		return "";
	}

    public static void sendEmail(List<String> toAddresses, String subject, String htmlBody, String textBody) throws Exception
    {

    	String from = "Waynik <development@waynik.com>";

    	AmazonSimpleEmailService client =
  	          AmazonSimpleEmailServiceClientBuilder.standard()
  	          // Replace US_WEST_2 with the AWS Region you're using for
  	          // Amazon SES.
  	            .withRegion(Regions.US_WEST_2)
  	            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_SES_KEY, AWS_SES_SECRET)))
  	            .build();
  	      SendEmailRequest request = new SendEmailRequest()
  	          .withDestination(
  	              new Destination().withToAddresses(toAddresses))
  	          .withMessage(new Message()
  	              .withBody(new Body()
  	                  .withHtml(new Content()
  	                      .withCharset("UTF-8").withData(htmlBody))
  	                  .withText(new Content()
  	                      .withCharset("UTF-8").withData(textBody)))
  	              .withSubject(new Content()
  	                  .withCharset("UTF-8").withData(subject)))
  	          .withSource(from);
  	          // Comment or remove the next line if you are not using a
  	          // configuration set
  	          //.withConfigurationSetName(CONFIGSET);
      client.sendEmail(request);
    }

    public static String getJsonPayload(int userId, String message) {
        Gson gson = new Gson();
        Map<String, Object> payload = new HashMap<>();
    	payload.put("apiKey", WAYNIK_EVENTS_SECRET_KEY);
		payload.put("userId", Integer.toString(userId));
		payload.put("message", message);

		return gson.toJson(payload);
    }


}
