/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.mturk.AmazonMTurk;
import com.amazonaws.services.mturk.AmazonMTurkClientBuilder;
import com.amazonaws.services.mturk.model.Assignment;
import com.amazonaws.services.mturk.model.Comparator;
import com.amazonaws.services.mturk.model.CreateHITRequest;
import com.amazonaws.services.mturk.model.CreateHITResult;
import com.amazonaws.services.mturk.model.GetAssignmentRequest;
import com.amazonaws.services.mturk.model.GetHITRequest;
import com.amazonaws.services.mturk.model.GetHITResult;
import com.amazonaws.services.mturk.model.HIT;
import com.amazonaws.services.mturk.model.ListAssignmentsForHITRequest;
import com.amazonaws.services.mturk.model.ListAssignmentsForHITResult;
import com.amazonaws.services.mturk.model.Locale;
import com.amazonaws.services.mturk.model.QualificationRequirement;

/* 
 * Before connecting to MTurk, set up your AWS account and IAM settings as described here:
 * https://blog.mturk.com/how-to-use-iam-to-control-api-access-to-your-mturk-account-76fe2c2e66e2
 * 
 * Configure your AWS credentials as described here:
 * http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
 * 
 * V�rifier votre .aws/credentials
 *
 */

public class CreateHITSample {

	private static final String QUESTION_XML_FILE_NAME = "my_question.xml";

	private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
	private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";
	private static final String SIGNING_REGION = "us-east-1";

	public static void main(final String[] argv) throws IOException {
		/* 
		Use the Amazon Mechanical Turk Sandbox to publish test Human Intelligence Tasks (HITs) without paying any money.
		Sign up for a Sandbox account at https://requestersandbox.mturk.com/ with the same credentials as your main MTurk account
		
		Switch to getProdClient() in production. 
		Uncomment line 60, 61, & 66 below to create your HIT in production.
			
		*/
		Scanner in = new Scanner(System.in);
		
		
		
		final CreateHITSample sandboxApp = new CreateHITSample(getSandboxClient());
		final HITInfo hitInfo = sandboxApp.createHIT(QUESTION_XML_FILE_NAME);
		
		// final CreateHITSample prodApp = new CreateHITSample(getProdClient());
		// final HITInfo hitInfo = prodApp.createHIT(QUESTION_XML_FILE_NAME);
				
		System.out.println("Your HIT has been created. You can see it at this link:");
		
		System.out.println("https://workersandbox.mturk.com/mturk/preview?groupId=" + hitInfo.getHITTypeId());	
		// System.out.println("https://www.mturk.com/mturk/preview?groupId=" + hitInfo.getHITTypeId());
		
		System.out.println("Your HIT ID is: " + hitInfo.getHITId());
		
		System.out.println("Remplissez le HIT, et appuyer sur enter ");
	

		in.nextLine();
		
		sandboxApp.getGitResult(hitInfo.getHITId());
	}

	private void getGitResult(String hITId) {
		// TODO Auto-generated method stub
		 GetHITResult hitResult = client.getHIT(new GetHITRequest().withHITId(hITId));
		 
		HIT hit = hitResult.getHIT();
		String status = hit.getHITStatus();
		
		ListAssignmentsForHITRequest listHITRequest = new ListAssignmentsForHITRequest();
		listHITRequest.setHITId(hit.getHITId());
		

		ListAssignmentsForHITResult listHITResult = client.listAssignmentsForHIT(listHITRequest);
		
		List<Assignment> assignmentList = listHITResult.getAssignments();
		System.out.println("The number of submitted assignments is " + assignmentList.size());
		
		System.out.println(assignmentList.get(0).getAnswer());
	}

	private final AmazonMTurk client;

	private CreateHITSample(final AmazonMTurk client) {
		this.client = client;
	}

	private static AmazonMTurk getSandboxClient() {
		AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
		builder.setEndpointConfiguration(new EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION));
		return builder.build();
	}
	
	private static AmazonMTurk getProdClient() {
		AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
		builder.setEndpointConfiguration(new EndpointConfiguration(PROD_ENDPOINT, SIGNING_REGION));
		return builder.build();
	}

	private static final class HITInfo {
		private final String hitId;
		private final String hitTypeId;

		private HITInfo(final String hitId, final String hitTypeId) {
			this.hitId = hitId;
			this.hitTypeId = hitTypeId;
		}

		private String getHITId() {
			return this.hitId;
		}

		private String getHITTypeId() {
			return this.hitTypeId;
		}
	}

	private HITInfo createHIT(final String questionXmlFile) throws IOException {

		// QualificationRequirement: Locale IN (US, CA)
		QualificationRequirement localeRequirement = new QualificationRequirement();
		localeRequirement.setQualificationTypeId("00000000000000000071");
		localeRequirement.setComparator(Comparator.In);
		List<Locale> localeValues = new ArrayList<>();
		localeValues.add(new Locale().withCountry("US"));
		localeValues.add(new Locale().withCountry("CA"));
		localeRequirement.setLocaleValues(localeValues);
		localeRequirement.setRequiredToPreview(true);

		// Read the question XML into a String
		String questionSample = new String(Files.readAllBytes(Paths.get(questionXmlFile)));

		CreateHITRequest request = new CreateHITRequest();
		request.setMaxAssignments(10);
		request.setLifetimeInSeconds(6000L);
		request.setAssignmentDurationInSeconds(6000L);
		// Reward is a USD dollar amount - USD$0.20 in the example below
		request.setReward("0.20");
		request.setTitle("Deontologist or consequentialist ?");
		request.setKeywords("question, answer, research");
		request.setDescription("Answer a simple question");
		request.setQuestion(questionSample);
		//request.setQualificationRequirements(Collections.singletonList(localeRequirement));

		CreateHITResult result = client.createHIT(request);
		return new HITInfo(result.getHIT().getHITId(), result.getHIT().getHITTypeId());
	}
}