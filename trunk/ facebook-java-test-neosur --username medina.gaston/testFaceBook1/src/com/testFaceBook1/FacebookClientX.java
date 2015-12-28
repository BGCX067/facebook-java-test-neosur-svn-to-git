package com.testFaceBook1;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.facebook.api.FacebookException;
//import com.facebook.api.FacebookRestClient;

/**
 * facebook rest client extension.
 * 'X' in the class name means extension.
 *
 * this class is used as an extension
 * to FacebookRestClient.
 * it provides a few extra functionalities
 * that makes development a little easier.
 *
 *
 * @author sung solomon wu
 *
 *
 * this class uses Facebook's Facebook API
 * (Facebook's Facebook API is owned by
 * Facebook, but is free to use or modify).
 * Please see Facebook API's documentation
 * about their copyright policy.
 */
public class FacebookClientX {

	public static final String FRIENDS_GET_RESPONSE = "friends_get_response";
	public static final String FRIENDS_GET_APP_USERS_RESPONSE = "friends_getAppUsers_response";
	public static final String UID = "uid";

	/**
	 * the FacebookRestClient object encapsulated by
	 * this object.
	 */
	private final FacebookRestClient originalClient;

	/**
	 * constructor.
	 * passover the parameter to the encapsulated FacebookRestClient obj.
	 * @param apiKey
	 * @param secret
	 */
	public FacebookClientX(String apiKey, String secret) {
		originalClient = new FacebookRestClient(apiKey, secret, null);
	}

	public FacebookClientX(String apiKey, String secret, String sessionKey) {
		originalClient = new FacebookRestClient(apiKey, secret, sessionKey);
	}

	public FacebookClientX(String serverAddr, String apiKey, String secret,
			String sessionKey) throws MalformedURLException {
		originalClient = new FacebookRestClient(serverAddr, apiKey, secret, sessionKey);
	}

	public FacebookClientX(URL serverUrl, String apiKey, String secret, String sessionKey) {
		originalClient = new FacebookRestClient(serverUrl, apiKey, secret, sessionKey);
	}


	/**
	 * constructor.
	 * @param originalClient - the facebook client
	 * that will be encapsulated by this class.
	 */
	public FacebookClientX(FacebookRestClient originalClient) {
		this.originalClient = originalClient;
	}

	public FacebookRestClient getFacebookRestClient() {
		return this.originalClient;
	}

	/**
	 * same as FacebookRestClient.friends_get()
	 * but with different return value.
	 *
	 *
	 * example of the return value of
	 * the original FacebookRestClient.friends_get():
	 *
	 * <code>
	 *   #document
    		friends_get_response
      			uid
        			'202488'
      			uid
        			'203947'
	 * </code>
	 *
	 * @return a list of friends user id.
	 * @throws FacebookException
	 * @throws IOException
	 */
	public List<Integer> friends_get()
		throws FacebookException, IOException {
		Document doc = originalClient.friends_get();

		// parse the document returned into friends.

		// if FacebookRestClient.friends_get() has error, it will
		// throw exception already.  so we do not need to check
		// if the xml document contains an error code returned by
		// facebook api.

		if (originalClient.isDebug()) {
			// for performance reason, we don't check
			// if the response xml document contains "friends_get_response" element
			NodeList response = doc.getElementsByTagName(FRIENDS_GET_RESPONSE);
			// if the response xml document does not have one and only one "friends_get_response" element, then
			// there is a bug on facebook server.
			assert (response.getLength() == 1) : "should contain only 1 " + FRIENDS_GET_RESPONSE + " element.  instead found " + response.getLength();
		}

		List<Integer> results = getUids(doc);
		if (originalClient.isDebug()) {
			System.out.println("friends_get return=" + results);
		}

		return results;
	}

	public List<Integer> friends_getAppUsers() throws FacebookException, IOException {
		Document doc = originalClient.friends_getAppUsers();

		if (originalClient.isDebug()) {
			// for performance reason, we don't check
			// if the response xml document contains "friends_get_response" element
			NodeList response = doc.getElementsByTagName(FRIENDS_GET_APP_USERS_RESPONSE);
			// if the response xml document does not have one and only one "friends_get_response" element, then
			// there is a bug on facebook server.
			assert (response.getLength() == 1) : "should contain only 1 " + FRIENDS_GET_RESPONSE + " element.  instead found " + response.getLength();
		}

		List<Integer> results = getUids(doc);
		if (originalClient.isDebug()) {
			System.out.println("friends_get return=" + results);
		}

		return results;
	}

	/**
	 * get the <uid> element from the
	 * response document.
	 * @param doc
	 * @return
	 */
	private List<Integer> getUids(Document doc) {
		List<Integer> results = new ArrayList<Integer>();
		NodeList uids = doc.getElementsByTagName(UID);
		int length = uids.getLength();
		for (int i = 0; i < length; i++) {
			results.add(new Integer(uids.item(i).getFirstChild().getNodeValue()));
		}
		return results;
	}
}
