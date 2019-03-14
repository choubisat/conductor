/**
 * 
 */
package com.netflix.conductor.client.sample;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskResult.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author choubisa
 *
 */
public class ReviewTask implements Worker {

	private String taskDefName;

	private static final Map<String,String> headers;
	static 
	{
		headers = new HashMap<String,String>();
		headers.put("Content-Type","application/json");
		headers.put("x-api-key","redhawkadmin2");
		headers.put("x-ims-clientid","eureka_service");
		headers.put("x-user-auth","Bearer eyJ4NXUiOiJpbXNfbmExLXN0ZzEta2V5LTEuY2VyIiwiYWxnIjoiUlMyNTYifQ.eyJpZCI6IjE1NDc0NDc2ODAwMDNfOTQ4NTg2YmMtZWFiMS00NTMyLWJlYjYtN2YyMjNkZDkwMGVmX3VlMSIsImNsaWVudF9pZCI6ImRjLXN0YWdlLXZpcmdvd2ViIiwidXNlcl9pZCI6Ijc3MTc2QzFFNUJGNTA5MjYwQTQ5NDIwOEBBZG9iZUlEIiwidHlwZSI6ImFjY2Vzc190b2tlbiIsImFzIjoiaW1zLW5hMS1zdGcxIiwiZmciOiJUREVDVjZSNFg3NzM3NzdYNzdQNU9USUFOST09PT09PSIsInNpZCI6IjE1NDcxMTIxNjA1NTBfOGI3Y2FiNzItYTQxZi00YWU5LWFkZmYtMjk0YmJjMGQ3MDc5X3VlMSIsIm1vaSI6ImU2Y2NjOTAwIiwiYyI6InE0dndiQ3Q0Z2haWTJPMndwV3QxRGc9PSIsImV4cGlyZXNfaW4iOiI4NjQwMDAwMCIsInNjb3BlIjoiQWRvYmVJRCxvcGVuaWQsRENBUEksYWRkaXRpb25hbF9pbmZvLmFjY291bnRfdHlwZSx1cGRhdGVfcHJvZmlsZS5maXJzdF9uYW1lLHVwZGF0ZV9wcm9maWxlLmxhc3RfbmFtZSxhZ3JlZW1lbnRfc2VuZCxhZ3JlZW1lbnRfc2lnbixzaWduX2xpYnJhcnlfd3JpdGUsc2lnbl91c2VyX3JlYWQsc2lnbl91c2VyX3dyaXRlLGFncmVlbWVudF9yZWFkLGFncmVlbWVudF93cml0ZSx3aWRnZXRfcmVhZCx3aWRnZXRfd3JpdGUsd29ya2Zsb3dfcmVhZCx3b3JrZmxvd193cml0ZSxzaWduX2xpYnJhcnlfcmVhZCxzaWduX3VzZXJfbG9naW4sZWUuZGN3ZWIiLCJjcmVhdGVkX2F0IjoiMTU0NzQ0NzY4MDAwMyJ9.ZXnGmeKjDDOdOWUcObcbPxwZdqMRuVA0cWindVS5d8__n2yirLpY4MGgeSP-MiMS75x3Info1it08O7nTTfZbMHpLhHqmiwK8CZTyvmrttpfARmiP-wefji68dFvgSbDibyUPftVqXIKEwtRyw-GU_yqZIxxa1XT-ZkZlp-PqcAbvTdcWJ4law32qBo3i3wzBCwAVul1GxfKa0D63JGh_4LWm3gCBzvwo9dOaECCQKWHf8exuI3Y-Des0wglQjE6s9LPNyEUXobKPa6jx6KC8BBbIfeTak-T4JkR1LkBmlJtln7pykPaeGIfSCKBfA5whMtayEI1aerf6EWaqcvumw");	
	}
	private static final String url = "http://reviewacp-dev-va6.dev.cloud.adobe.io/api/reviews/";


	/**
	 * 
	 */
	public ReviewTask(String taskDefName) {
		this.taskDefName = taskDefName;
	}

	@Override
	public TaskResult execute(Task task) {

		System.out.printf("Executing %s%n", taskDefName);
		TaskResult result = new TaskResult(task);
		String file = null;
		try {

			if (task.getInputData().containsKey(CommonKeys.keyFileName))
			{
				file = task.getInputData().get(CommonKeys.keyFileName).toString();
			}
			else
			{
				throw new InvalidParameterException("Task does not have input key " + CommonKeys.keyFileName);
			}
			System.out.printf("Preparing request to create review for file %s%n", file);
			HttpPost request = new HttpPost(url);		
			HttpClient client = HttpClientBuilder.create().build();
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}

			String json = "{\"review_info\":{\"intro_message\":\"It would be great to get your feedback.\",\"intro_subject\":\"Please review.\",\"deadline_date\":\"2019-12-28T13:01:33Z\",\"reminder_dates\":[\"2019-12-28T13:01:33Z\"]},\"resource\":{\"id\":\"test-sharing-id\"},\"participants\":[{\"email\":\"choubisa@adobe.com\"}]}";

			//Thread.sleep(30000);
			StringEntity entity = new StringEntity(json);
			request.setEntity(entity);
			HttpResponse response = client.execute(request);
			System.out.printf("response code = %d",response.getStatusLine().getStatusCode());
			HttpEntity httpEntity = response.getEntity();

			String respRes = "";
	        if (httpEntity != null) {

	            // A Simple JSON Response Read
	            InputStream instream = httpEntity.getContent();
	            respRes = convertStreamToString(instream);
	            // now you have the string representation of the HTML request
	            System.out.println("RESPONSE: " + respRes);
	            instream.close();

	        }
	        // Headers
	        org.apache.http.Header[] headers = response.getAllHeaders();
	        for (int i = 0; i < headers.length; i++) {
	            System.out.println(headers[i]);
	        }
	        result.getOutputData().put(CommonKeys.keyMessage, "Review Created for " + file + " Response for request "
					+ response.getStatusLine().getStatusCode());
			result.getOutputData().put(CommonKeys.keyReviewId, respRes);
			result.setStatus(Status.COMPLETED);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.printf("response Error, exception %s", e.getMessage());
			e.printStackTrace();
			result.setStatus(Status.FAILED);
		}
		return result;
	}
	
	private static String convertStreamToString(InputStream is) {

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return sb.toString();
	}

	@Override
	public String getTaskDefName() {
		// TODO Auto-generated method stub
		return taskDefName;
	}

}
