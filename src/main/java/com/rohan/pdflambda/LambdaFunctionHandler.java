package com.rohan.pdflambda;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {

    private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

    public LambdaFunctionHandler() {}

    // Test purpose only.
    LambdaFunctionHandler(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
    	context.getLogger().log("105");
    	context.getLogger().log("Received event: " + event);

        // Get the object from the event and show its content type
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        try {
            S3Object response = s3.getObject(new GetObjectRequest(bucket, key));            
            String contentType = response.getObjectMetadata().getContentType();
            String lastModified = String.valueOf(response.getObjectMetadata().getLastModified());
            String contentLength = String.valueOf(response.getObjectMetadata().getContentLength());
            String versionId = response.getObjectMetadata().getVersionId();
            String eTag = response.getObjectMetadata().getETag();
            String region = event.getRecords().get(0).getAwsRegion();
            
            String email = StringUtils.substringBefore(key, "_");
            byte[] decodedEmail = Base64.getDecoder().decode(email); // we won't have an '=' pad but still can decode it
            String emailString = new String(decodedEmail);
            context.getLogger().log("DECODED email address: "+emailString);
            
            context.getLogger().log("CONTENT TYPE: " + contentType);
            context.getLogger().log("LAST MODIFIED: " + lastModified);
            context.getLogger().log("CONTENT LEN: " + contentLength);
            context.getLogger().log("VERSION ID: " + versionId);
            context.getLogger().log("ETAG: " + eTag);
            context.getLogger().log("REGION: " + region);
            context.getLogger().log("URL: " + "https://s3.us-east-1.amazonaws.com/"+bucket+"/"+response.getKey());
            sendMail("https://s3.us-east-1.amazonaws.com/"+bucket+"/"+response.getKey(), emailString);
            return contentType;
        } catch (Exception e) {
            e.printStackTrace();
            context.getLogger().log(String.format(
                "Error getting object %s from bucket %s. Make sure they exist and"
                + " your bucket is in the same region as this function.", key, bucket));
            throw e;
        }
    }
    
    private boolean sendMail(String url, String to) {
    
        final String FROM = "r.truscott@gmail.com";  // Replace with your "From" address. This address must be verified.
        //String TO = "r.truscott@gmail.com"; // Replace with a "To" address. If you have not yet requested
        String TO = to;
                                                          // production access, this address must be verified.
        final String BODY = "This email was sent through Amazon SES by using the AWS SDK for Java.\nHere's the link\n"+url;
        final String SUBJECT = "PDF Chart is now available (AWS SDK for Java)";

        
        if (!TO.contains("@")) {
        	System.out.println("not valid "+TO);
        	TO="r.truscott@gmail.com";
        }
    
        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(new String[]{TO});

        // Create the subject and body of the message.
        Content subject = new Content().withData(SUBJECT);
        Content textBody = new Content().withData(BODY);
        Body body = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination).withMessage(message);

        try {
            System.out.println("Attempting to send an email through Amazon SES by using the AWS SDK for Java...");

            /*
             * The ProfileCredentialsProvider will return your [default]
             * credential profile by reading from the credentials file located at
             * (C:\\Users\\rohan\\.aws\\credentials).
             *
             * TransferManager manages a pool of threads, so we create a
             * single instance and share it throughout our application.
             */
            
            String userKey =  System.getenv("AWS_ACCESS_KEY_ID1");
            String userPass = System.getenv("AWS_SECRET_ACCESS_KEY1");
            
            AWSCredentials credentials = new BasicAWSCredentials(userKey, userPass);
            
            AmazonSimpleEmailServiceClient client =
                    new AmazonSimpleEmailServiceClient(
                            new BasicAWSCredentials(userKey, userPass));
            
            if (userKey!=null&&userPass!=null) {
            	System.out.println("userKey  "+userKey.substring(0, 4));
            	System.out.println("userPass "+userPass.substring(0, 4));
            } else {
            	System.out.println("Fark. Can't read the creddys");
            }
            
//            ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
//            try {
//                credentialsProvider.getCredentials();
//            } catch (Exception e) {
//                throw new AmazonClientException(
//                        "Cannot load the credentials from the credential profiles file. " +
//                        "Please make sure that your credentials file is at the correct " +
//                        "location (C:\\Users\\rohan\\.aws\\credentials), and is in valid format.",
//                        e);
//            }

            // Instantiate an Amazon SES client, which will make the service call with the supplied AWS credentials.
//            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
//                .withCredentials(credentialsProvider)
                // Choose the AWS region of the Amazon SES endpoint you want to connect to. Note that your production
                // access status, sending limits, and Amazon SES identity-related settings are specific to a given
                // AWS region, so be sure to select an AWS region in which you set up Amazon SES. Here, we are using
                // the US East (N. Virginia) region. Examples of other regions that Amazon SES supports are US_WEST_2
                // and EU_WEST_1. For a complete list, see http://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html
//                .withRegion("us-east-1")
//                .build();

            // Send the email.
            client.sendEmail(request);
            System.out.println("Email sent!");

        } catch (Exception ex) {
            System.out.println("The email was not sent.");
            System.out.println("Error message: " + ex.getMessage());
        }
        return true;
    }
}