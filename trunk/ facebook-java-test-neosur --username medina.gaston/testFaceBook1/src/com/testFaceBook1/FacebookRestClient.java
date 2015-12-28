package com.testFaceBook1;
/*
+---------------------------------------------------------------------------+
| Facebook Development Platform Java Client                                 |
+---------------------------------------------------------------------------+
| Copyright (c) 2007 Facebook, Inc.                                         |
| All rights reserved.                                                      |
|                                                                           |
| Redistribution and use in source and binary forms, with or without        |
| modification, are permitted provided that the following conditions        |
| are met:                                                                  |
|                                                                           |
| 1. Redistributions of source code must retain the above copyright         |
|    notice, this list of conditions and the following disclaimer.          |
| 2. Redistributions in binary form must reproduce the above copyright      |
|    notice, this list of conditions and the following disclaimer in the    |
|    documentation and/or other materials provided with the distribution.   |
|                                                                           |
| THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR      |
| IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES |
| OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   |
| IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,          |
| INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT  |
| NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, |
| DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY     |
| THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT       |
| (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF  |
| THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.         |
+---------------------------------------------------------------------------+
| For help with this library, contact developers-help@facebook.com          |
+---------------------------------------------------------------------------+
*/

//package com.facebook.api;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.facebook.api.FacebookException;
import com.facebook.api.FacebookMethod;
import com.facebook.api.FacebookSignatureUtil;
import com.facebook.api.PhotoTag;
import com.facebook.api.ProfileField;
import com.facebook.api.schema.Album;
import com.facebook.api.schema.EventMembers;
import com.facebook.api.schema.EventsGetResponse;
import com.facebook.api.schema.FacebookApiException;
import com.facebook.api.schema.FriendsAreFriendsResponse;
import com.facebook.api.schema.GroupMembers;
import com.facebook.api.schema.GroupsGetResponse;
import com.facebook.api.schema.Notifications;
import com.facebook.api.schema.Photo;
import com.facebook.api.schema.PhotosGetResponse;
import com.facebook.api.schema.User;
import com.facebook.api.schema.UsersGetInfoResponse;




/* todo:
* fix some of the things on the original client
* provided by facebook
* 1. add javadoc
* 2. for non-constant variables, do not use all-cap variable names
* 3. why is _debug variable Boolean instead of boolean.  is there a specific reason?
* 4. do not call System.exit()
* 5. should we change method names to ones without underscore?
*
*/

/**
* changes made from the original FacebookRestClient
* provided by Facebook.
* original FacebookRestClient is downloaded from
* http://developers.facebook.com/clientlibs/facebook_java_client.zip
* (downloaded on end of June 2007.  the site said it was updated on May 28, 2007).
*
* 1. change FB_SERVER value from "api.f8.facebook.com/restserver.php"
* to "api.facebook.com/restserver.php".  the f8 value is temporary.
* 2. add getUserId()
* 3. add callMethodJaxb() to parse the xml doc using jaxb
* instead of org.w3c.Document.
* 4. add methods that return jaxb objects instead of Document.
* events_getJaxb(...)
* events_getMembersJaxb()
* friends_getJaxb()
* friends_getAppUsersJaxb()
* friends_areFriendsJaxb( ... )
* friends_areFriendsJaxb( ... )
* users_getInfoJaxb( ... )
* users_getInfoJaxb( ... )
* photos_getJaxb( ... )  - various parameters.
* photos_createAlbumJaxb(albumName)
* photos_createAlbumJaxb(name, description, location)
* photos_uploadJaxb(...) - various parameters
* photos_addTagJaxb(...)
* groups_get(...)
* groups_getMembers(...)
* notifications_get()
* profile_getFBMLJaxb()
*
* todo:
* methods to be implemented:
*
* fql_queryJaxb(query)
* feed_xxxxxxJaxb()
*
* @author facebook
* @author sung solomon wu
*
* I'm sorry if there are more people to be included in
* author list.  If you are one of the original authors or
* know who the original authors of this class is.  please
* include it or let me (Sung Wu) know to include it.
* I get the original class as part of the Facebook Java API
* published by Facebook and modified it.
*/
public class FacebookRestClient {
public static final String TARGET_API_VERSION = "1.0";
public static final String ERROR_TAG = "error_response";
// presence of f8 here is temporary ... should be removed at launch
// note that f8 does not support https://
public static final String FB_SERVER = "api.facebook.com/restserver.php";
public static final String SERVER_ADDR = "http://" + FB_SERVER;
public static final String HTTPS_SERVER_ADDR = "https://" + FB_SERVER;
public static URL SERVER_URL = null;
public static URL HTTPS_SERVER_URL = null;
static {
  try {
    SERVER_URL = new URL(SERVER_ADDR);
    HTTPS_SERVER_URL = new URL(HTTPS_SERVER_ADDR);
  }
  catch (MalformedURLException e) {
    System.err.println("MalformedURLException: " + e.getMessage());
    System.exit(1);
  }
}

private final String _secret;
private final String _apiKey;
private final URL _serverUrl;

private String _sessionKey; // filled in when session is established
private boolean _isDesktop = false;
private String _sessionSecret; // only used for desktop apps
private int _userId;

public static int NUM_AUTOAPPENDED_PARAMS = 5;
private static boolean DEBUG = false;
private Boolean _debug = null;

private File _uploadFile = null;

protected class Pair<N, V> {
  public N first;
  public V second;

  public Pair(N name, V value) {
    this.first = name;
    this.second = value;
  }
}


public FacebookRestClient(String apiKey, String secret) {
  this(SERVER_URL, apiKey, secret, null);
}

public FacebookRestClient(String apiKey, String secret, String sessionKey) {
  this(SERVER_URL, apiKey, secret, sessionKey);
}


public FacebookRestClient(String serverAddr, String apiKey, String secret,
                          String sessionKey) throws MalformedURLException {
  this(new URL(serverAddr), apiKey, secret, sessionKey);
}

public FacebookRestClient(URL serverUrl, String apiKey, String secret, String sessionKey) {
  _sessionKey = sessionKey;
  _apiKey = apiKey;
  _secret = secret;
  _serverUrl = (null != serverUrl) ? serverUrl : SERVER_URL;
}

public static void setDebugAll(boolean isDebug) {
  FacebookRestClient.DEBUG = isDebug;
}

public void setDebug(boolean isDebug) {
  _debug = isDebug;
}

public boolean isDebug() {
  return (null == _debug) ? FacebookRestClient.DEBUG : _debug.booleanValue();
}

public boolean isDesktop() {
  return this._isDesktop;
}

public void setIsDesktop(boolean isDesktop) {
  this._isDesktop = isDesktop;
}

/**
 * Prints out the DOM tree.
 */
public static void printDom(Node n, String prefix) {
  // should use StringBuffer instead of String for performance reason.
  String outString = prefix;
  if (n.getNodeType() == Node.TEXT_NODE) {
    outString += "'" + n.getNodeValue()/*getTextContent()*/.trim() + "'";
  }
  else {
    outString += n.getNodeName();
  }
  System.out.println(outString);
  NodeList children = n.getChildNodes();
  int length = children.getLength();
  for (int i = 0; i < length; i++) {
    FacebookRestClient.printDom(children.item(i), prefix + "  ");
  }
}

private static CharSequence delimit(Collection iterable) {
  // could add a thread-safe version that uses StringBuffer as well
  if (iterable == null || iterable.isEmpty())
    return null;

  StringBuilder buffer = new StringBuilder();
  boolean notFirst = false;
  for (Object item: iterable) {
    if (notFirst)
      buffer.append(",");
    else
      notFirst = true;
    buffer.append(item.toString());
  }
  return buffer;
}

protected static CharSequence delimit(Collection<Map.Entry<String, CharSequence>> entries,
                                      CharSequence delimiter, CharSequence equals,
                                      boolean doEncode) {
  if (entries == null || entries.isEmpty())
    return null;

  StringBuilder buffer = new StringBuilder();
  boolean notFirst = false;
  for (Map.Entry<String, CharSequence> entry: entries) {
    if (notFirst)
      buffer.append(delimiter);
    else
      notFirst = true;
    CharSequence value = entry.getValue();
    buffer.append(entry.getKey()).append(equals).append(doEncode ? encode(value) : value);
  }
  return buffer;
}

/**
 * Call the specified method, with the given parameters,
 * and return the xml results as jaxb object.
 *
 * @param method the fieldName of the method
 * @param paramPairs a list of arguments to the method
 * @throws Exception with a description of any errors given to us by the server.
 */
protected Object callMethodJaxb(FacebookMethod method,
                              Pair<String, CharSequence>... paramPairs) throws FacebookException,
                                                                               IOException {
  return callMethodJaxb(method, Arrays.asList(paramPairs));
}

/**
 * Call the specified method, with the given parameters,
 * and return result as a jaxb object.
 *
 * @param method the fieldName of the method
 * @param paramPairs a list of arguments to the method
 * @throws Exception with a description of any errors given to us by the server.
 */
protected Object callMethodJaxb(FacebookMethod method,
                              Collection<Pair<String, CharSequence>> paramPairs) throws FacebookException,
                                                                                        IOException {
  HashMap<String, CharSequence> params =
    new HashMap<String, CharSequence>(2 * method.numTotalParams());

  params.put("method", method.methodName());
  params.put("api_key", _apiKey);
  params.put("v", TARGET_API_VERSION);
  if (method.requiresSession()) {
    params.put("call_id", Long.toString(System.currentTimeMillis()));
    params.put("session_key", _sessionKey);
  }
  CharSequence oldVal;
  for (Pair<String, CharSequence> p: paramPairs) {
    oldVal = params.put(p.first, p.second);
    if (oldVal != null)
      System.err.printf("For parameter %s, overwrote old value %s with new value %s.", p.first,
                        oldVal, p.second);
  }

  assert (!params.containsKey("sig"));
  String signature = generateSignature(FacebookSignatureUtil.convert(params.entrySet()), method.requiresSession());
  params.put("sig", signature);

  try {
    boolean doHttps = this.isDesktop() && FacebookMethod.AUTH_GET_SESSION.equals(method);
    InputStream data =
      method.takesFile() ? postFileRequest(method.methodName(), params) : postRequest(method.methodName(),
                                                                                      params,
                                                                                      doHttps,
                                                                                      true);
      Object obj = null;
      JAXBContext jc = JAXBContext.newInstance("com.facebook.api._1");
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      obj = unmarshaller.unmarshal(data);

      // handle error
      // when it is an error response, the parsed
      // result will be a JAXBElement instead
      // of the specific object like com.facebook.api._1.GroupsGetResponse.
      // some of the response document like
      // <auth_createToken_response>, is parsed
      // into JAXBElement even without any error code.
      //
      // we can test this portion of code
      // by going to facebook site and change the facebook app
      // setting to "desktop".
      // since our client's isDesktop is false, we will get
      // 104 "incorrect signature".
      if (obj instanceof JAXBElement) {
              // there might be an error.
              // retrieve error tag
              JAXBElement elem = (JAXBElement)obj;

              if (isDebug()) {
                      System.out.println("type=" + elem.getDeclaredType() + ".  element name=" + elem.getName().toString() + ".  value=" + elem.getValue());
              }

              if (elem.getDeclaredType() == FacebookApiException.class) {
                      // this is an error response.
                      JAXBElement<FacebookApiException> err = (JAXBElement<FacebookApiException>)elem;
                      FacebookApiException fae = err.getValue();
                      //System.out.println("error code=" + fae.getErrorCode() + ".  msg=" + fae.getErrorMsg());
                      throw new FacebookException(fae.getErrorCode(), fae.getErrorMsg());
              }
      }
    return obj;
  }
  catch (JAXBException ex) {
    System.err.println("jaxbexception.  unable to parse the returned xml doc." + ex);
    ex.printStackTrace();
  }

  return null;
}

/**
 * Call the specified method, with the given parameters, and return a DOM tree with the results.
 *
 * @param method the fieldName of the method
 * @param paramPairs a list of arguments to the method
 * @throws Exception with a description of any errors given to us by the server.
 */
protected Document callMethod(FacebookMethod method,
                              Pair<String, CharSequence>... paramPairs) throws FacebookException,
                                                                               IOException {
  return callMethod(method, Arrays.asList(paramPairs));
}


/**
 * Call the specified method, with the given parameters, and return a DOM tree with the results.
 *
 * @param method the fieldName of the method
 * @param paramPairs a list of arguments to the method
 * @throws Exception with a description of any errors given to us by the server.
 */
protected Document callMethod(FacebookMethod method,
                              Collection<Pair<String, CharSequence>> paramPairs) throws FacebookException,
                                                                                        IOException {
  HashMap<String, CharSequence> params =
    new HashMap<String, CharSequence>(2 * method.numTotalParams());

  params.put("method", method.methodName());
  params.put("api_key", _apiKey);
  params.put("v", TARGET_API_VERSION);
  if (method.requiresSession()) {
    params.put("call_id", Long.toString(System.currentTimeMillis()));
    params.put("session_key", _sessionKey);
  }
  CharSequence oldVal;
  for (Pair<String, CharSequence> p: paramPairs) {
    oldVal = params.put(p.first, p.second);
    if (oldVal != null)
      System.err.printf("For parameter %s, overwrote old value %s with new value %s.", p.first,
                        oldVal, p.second);
  }

  assert (!params.containsKey("sig"));
  String signature = generateSignature(FacebookSignatureUtil.convert(params.entrySet()), method.requiresSession());
  params.put("sig", signature);

  try {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    boolean doHttps = this.isDesktop() && FacebookMethod.AUTH_GET_SESSION.equals(method);
    InputStream data =
      method.takesFile() ? postFileRequest(method.methodName(), params) : postRequest(method.methodName(),
                                                                                      params,
                                                                                      doHttps,
                                                                                      true);

    Document doc = builder.parse(data); // original

    // temp - do the following to print the xml format - begin
//      StringBuffer sb = new StringBuffer();
//      int ch = -1;
//
//      while ((ch = data.read()) != -1) {
//            sb.append((char)ch);
//      }
//            System.out.println("returned xml=" + sb.toString());
//
//      Document doc = builder.parse(new StringBufferInputStream(sb.toString()));
    // temp - do the following to print the xml format - end

    //doc.normalizeDocument();
    doc.normalize();
    stripEmptyTextNodes(doc);

    if (isDebug())
      FacebookRestClient.printDom(doc, method.methodName() + "| "); // TEST
    NodeList errors = doc.getElementsByTagName(ERROR_TAG);
    if (errors.getLength() > 0) {
      int errorCode =
        Integer.parseInt(errors.item(0).getFirstChild().getFirstChild().getTextContent());
      String message = errors.item(0).getFirstChild().getNextSibling().getTextContent();
      throw new FacebookException(errorCode, message);
    }


    return doc;
  }
  catch (javax.xml.parsers.ParserConfigurationException ex) {
    System.err.println("huh?" + ex);
  }
  catch (org.xml.sax.SAXException ex) {
    throw new IOException("error parsing xml");
  }
  return null;
}

/**
 * Hack...since DOM reads newlines as textnodes we want to strip out those
 * nodes to make it easier to use the tree.
 */
private static void stripEmptyTextNodes(Node n) {
  NodeList children = n.getChildNodes();
  int length = children.getLength();
  for (int i = 0; i < length; i++) {
    Node c = children.item(i);
    if (!c.hasChildNodes() && c.getNodeType() == Node.TEXT_NODE &&
        c.getNodeValue()/*getTextContent()*/.trim().length() == 0) {
      n.removeChild(c);
      i--;
      length--;
      children = n.getChildNodes();
    }
    else {
      stripEmptyTextNodes(c);
    }
  }
}

private String generateSignature(List<String> params, boolean requiresSession) {
  String secret = (isDesktop() && requiresSession) ? this._sessionSecret : this._secret;
  return FacebookSignatureUtil.generateSignature(params, secret);
}

private static String encode(CharSequence target) {
  String result = target.toString();
  try {
    result = URLEncoder.encode(result, "UTF8");
  }
  catch (UnsupportedEncodingException e) {
    System.err.printf("Unsuccessful attempt to encode '%s' into UTF8", result);
  }
  return result;
}

private InputStream postRequest(CharSequence method, Map<String, CharSequence> params,
                                boolean doHttps, boolean doEncode) throws IOException {
  CharSequence buffer = (null == params) ? "" : delimit(params.entrySet(), "&", "=", doEncode);
  URL serverUrl = (doHttps) ? HTTPS_SERVER_URL : _serverUrl;
  if (isDebug()) {
    System.out.print(method);
    System.out.print(" POST: ");
    System.out.print(serverUrl.toString());
    System.out.print("/");
    System.out.println(buffer);
    System.out.flush();
  }

  HttpURLConnection conn = (HttpURLConnection) serverUrl.openConnection();
  try {
    conn.setRequestMethod("POST");
  }
  catch (ProtocolException ex) {
    System.err.println("huh?" + ex);
  }
  conn.setDoOutput(true);
  conn.connect();
  conn.getOutputStream().write(buffer.toString().getBytes());

  return conn.getInputStream();
}

/**
 * Sets the FBML for a user's profile, including the content for both the profile box
 * and the profile actions.
 * @param userId - the user whose profile FBML to set
 * @param fbmlMarkup - refer to the FBML documentation for a description of the markup and its role in various contexts
 * @return a boolean indicating whether the FBML was successfully set
 */
public boolean profile_setFBML(CharSequence fbmlMarkup, Integer userId) throws FacebookException, IOException {

  return extractBoolean(this.callMethod(FacebookMethod.PROFILE_SET_FBML,
                        new Pair<String, CharSequence>("uid", Integer.toString(userId)),
                        new Pair<String, CharSequence>("markup", fbmlMarkup)));

}

/**
 * Gets the FBML for a user's profile, including the content for both the profile box
 * and the profile actions.
 * @param userId - the user whose profile FBML to set
 * @return a Document containing FBML markup
 */
public Document profile_getFBML(Integer userId) throws FacebookException, IOException {
  return this.callMethod(FacebookMethod.PROFILE_GET_FBML,
                        new Pair<String, CharSequence>("uid", Integer.toString(userId)));

}

/**
 * see profile_getFBML(Integer)
 * @param userId
 * @return
 * @throws FacebookException
 * @throws IOException
 */
public String profile_getFBMLJaxb(Integer userId) throws FacebookException, IOException {
        Object obj = this.callMethodJaxb(FacebookMethod.PROFILE_GET_FBML,
            new Pair<String, CharSequence>("uid", Integer.toString(userId)));
        return ((JAXBElement<String>)obj).getValue();

}

/**
 * Recaches the referenced url.
 * @param url string representing the URL to refresh
 * @return boolean indicating whether the refresh succeeded
 */
public boolean fbml_refreshRefUrl(String url) throws FacebookException, IOException {
  return fbml_refreshRefUrl(new URL(url));
}

/**
 * Recaches the referenced url.
 * @param url the URL to refresh
 * @return boolean indicating whether the refresh succeeded
 */
public boolean fbml_refreshRefUrl(URL url) throws FacebookException, IOException {
  return extractBoolean(this.callMethod(FacebookMethod.FBML_REFRESH_REF_URL,
                                        new Pair<String, CharSequence>("url", url.toString())));
}


/**
 * Recaches the image with the specified imageUrl.
 * @param imageUrl String representing the image URL to refresh
 * @return boolean indicating whether the refresh succeeded
 */
public boolean fbml_refreshImgSrc(String imageUrl) throws FacebookException, IOException {
  return fbml_refreshImgSrc(new URL(imageUrl));
}

/**
 * Recaches the image with the specified imageUrl.
 * @param imageUrl the image URL to refresh
 * @return boolean indicating whether the refresh succeeded
 */
public boolean fbml_refreshImgSrc(URL imageUrl) throws FacebookException, IOException {
  return extractBoolean(this.callMethod(FacebookMethod.FBML_REFRESH_IMG_SRC,
                        new Pair<String, CharSequence>("url", imageUrl.toString())));
}

/**
 * Publish the notification of an action taken by a user to newsfeed.
 * @param title the title of the feed story
 * @param body the body of the feed story
 * @param images (optional) up to four pairs of image URLs and (possibly null) link URLs
 * @param priority
 * @return
 */
public Document feed_publishActionOfUser(CharSequence title, CharSequence body,
                                         Collection<Pair<URL, URL>> images,
                                         Integer priority) throws FacebookException,
                                                                  IOException {
  return feedHandler(FacebookMethod.FEED_PUBLISH_ACTION_OF_USER, title, body, images, priority);
}

/**
 * @see FacebookRestClient#feed_publishActionOfUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishActionOfUser(CharSequence title,
                                         CharSequence body) throws FacebookException,
                                                                   IOException {
  return feed_publishActionOfUser(title, body, null, null);
}

/**
 * @see FacebookRestClient#feed_publishActionOfUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishActionOfUser(CharSequence title, CharSequence body,
                                         Integer priority) throws FacebookException,
                                                                  IOException {
  return feed_publishActionOfUser(title, body, null, priority);
}

/**
 * @see FacebookRestClient#feed_publishActionOfUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishActionOfUser(CharSequence title, CharSequence body,
                                         Collection<Pair<URL, URL>> images) throws FacebookException,
                                                                                   IOException {
  return feed_publishActionOfUser(title, body, images, null);
}


/**
 * Publish a story to the logged-in user's newsfeed.
 * @param title the title of the feed story
 * @param body the body of the feed story
 * @param images (optional) up to four pairs of image URLs and (possibly null) link URLs
 * @param priority
 * @return
 */
public Document feed_publishStoryToUser(CharSequence title, CharSequence body,
                                        Collection<Pair<URL, URL>> images,
                                        Integer priority) throws FacebookException, IOException {
  return feedHandler(FacebookMethod.FEED_PUBLISH_STORY_TO_USER, title, body, images, priority);
}

/**
 * @see FacebookRestClient#feed_publishStoryToUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishStoryToUser(CharSequence title,
                                        CharSequence body) throws FacebookException,
                                                                  IOException {
  return feed_publishStoryToUser(title, body, null, null);
}

/**
 * @see FacebookRestClient#feed_publishStoryToUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishStoryToUser(CharSequence title, CharSequence body,
                                        Integer priority) throws FacebookException, IOException {
  return feed_publishStoryToUser(title, body, null, priority);
}

/**
 * @see FacebookRestClient#feed_publishStoryToUser(CharSequence,CharSequence,Collection,Integer)
 */
public Document feed_publishStoryToUser(CharSequence title, CharSequence body,
                                        Collection<Pair<URL, URL>> images) throws FacebookException,
                                                                                  IOException {
  return feed_publishStoryToUser(title, body, images, null);
}

protected Document feedHandler(FacebookMethod feedMethod, CharSequence title, CharSequence body,
                               Collection<Pair<URL, URL>> images,
                               Integer priority) throws FacebookException, IOException {
  assert (images == null || images.size() <= 4);

  ArrayList<Pair<String, CharSequence>> params =
    new ArrayList<Pair<String, CharSequence>>(feedMethod.numParams());

  params.add(new Pair<String, CharSequence>("title", title));
  params.add(new Pair<String, CharSequence>("body", body));
  if (null != priority)
    params.add(new Pair<String, CharSequence>("priority", priority.toString()));
  if (null != images && !images.isEmpty()) {
    int image_count = 0;
    for (Pair<URL, URL> image: images) {
      ++image_count;
      assert (image.first != null);
      params.add(new Pair<String, CharSequence>(String.format("image_%d", image_count),
                                                image.first.toString()));
      if (image.second != null)
        params.add(new Pair<String, CharSequence>(String.format("image_%d_link", image_count),
                                                  image.second.toString()));
    }
  }
  return this.callMethod(feedMethod, params);
}

/**
 * Returns all visible events according to the filters specified. This may be used to find all events of a user, or to query specific eids.
 * @param eventIds filter by these event ID's (optional)
 * @param userId filter by this user only (optional)
 * @param startTime UTC lower bound (optional)
 * @param endTime UTC upper bound (optional)
 * @return Document of events
 */
public Document events_get(Integer userId, Collection<Long> eventIds, Long startTime,
                           Long endTime) throws FacebookException, IOException {
  ArrayList<Pair<String, CharSequence>> params =
    new ArrayList<Pair<String, CharSequence>>(FacebookMethod.EVENTS_GET.numParams());

  boolean hasUserId = null != userId && 0 != userId;
  boolean hasEventIds = null != eventIds && !eventIds.isEmpty();
  boolean hasStart = null != startTime && 0 != startTime;
  boolean hasEnd = null != endTime && 0 != endTime;

  if (hasUserId)
    params.add(new Pair<String, CharSequence>("uid", Integer.toString(userId)));
  if (hasEventIds)
    params.add(new Pair<String, CharSequence>("eids", delimit(eventIds)));
  if (hasStart)
    params.add(new Pair<String, CharSequence>("start_time", startTime.toString()));
  if (hasEnd)
    params.add(new Pair<String, CharSequence>("end_time", endTime.toString()));
  return this.callMethod(FacebookMethod.EVENTS_GET, params);
}

/**
 * Returns all visible events according to the filters specified. This may be used to find all events of a user, or to query specific eids.
 * @param eventIds filter by these event ID's (optional)
 * @param userId filter by this user only (optional)
 * @param startTime UTC lower bound (optional)
 * @param endTime UTC upper bound (optional)
 * @return Document of events
 */
public EventsGetResponse events_getJaxb(Integer userId, Collection<Long> eventIds, Long startTime,
                           Long endTime) throws FacebookException, IOException {
  ArrayList<Pair<String, CharSequence>> params =
    new ArrayList<Pair<String, CharSequence>>(FacebookMethod.EVENTS_GET.numParams());

  boolean hasUserId = null != userId && 0 != userId;
  boolean hasEventIds = null != eventIds && !eventIds.isEmpty();
  boolean hasStart = null != startTime && 0 != startTime;
  boolean hasEnd = null != endTime && 0 != endTime;

  if (hasUserId)
    params.add(new Pair<String, CharSequence>("uid", Integer.toString(userId)));
  if (hasEventIds)
    params.add(new Pair<String, CharSequence>("eids", delimit(eventIds)));
  if (hasStart)
    params.add(new Pair<String, CharSequence>("start_time", startTime.toString()));
  if (hasEnd)
    params.add(new Pair<String, CharSequence>("end_time", endTime.toString()));
  Object obj = this.callMethodJaxb(FacebookMethod.EVENTS_GET, params);
  if (!(obj instanceof EventsGetResponse)) {
      System.out.println(FacebookMethod.EVENTS_GET + " returned incorrect document.  the result should be type " + EventsGetResponse.class.getName());
  }
  return (EventsGetResponse)obj;
}

/**
 * Retrieves the membership list of an event
 * @param eventId event id
 * @return Document consisting of four membership lists corresponding to RSVP status, with keys
 *  'attending', 'unsure', 'declined', and 'not_replied'
 */
public Document events_getMembers(Number eventId) throws FacebookException, IOException {
  assert (null != eventId);
  return this.callMethod(FacebookMethod.EVENTS_GET_MEMBERS,
                         new Pair<String, CharSequence>("eid", eventId.toString()));
}


public EventMembers events_getMembersJaxb(Number eventId) throws FacebookException, IOException {
  assert (null != eventId);
  JAXBElement<EventMembers> elem = (JAXBElement<EventMembers>)this.callMethodJaxb(FacebookMethod.EVENTS_GET_MEMBERS,
                         new Pair<String, CharSequence>("eid", eventId.toString()));
  return elem.getValue();
}


/**
 * Retrieves the friends of the currently logged in user.
 * @return array of friends
 */
public Document friends_areFriends(int userId1, int userId2) throws FacebookException,
                                                                    IOException {
  return this.callMethod(FacebookMethod.FRIENDS_ARE_FRIENDS,
                         new Pair<String, CharSequence>("uids1", Integer.toString(userId1)),
                         new Pair<String, CharSequence>("uids2", Integer.toString(userId2)));
}

public FriendsAreFriendsResponse friends_areFriendsJaxb(int userId1, int userId2) throws FacebookException,
                                                                    IOException {
  Object obj = this.callMethodJaxb(FacebookMethod.FRIENDS_ARE_FRIENDS,
                         new Pair<String, CharSequence>("uids1", Integer.toString(userId1)),
                         new Pair<String, CharSequence>("uids2", Integer.toString(userId2)));
  assert (obj instanceof FriendsAreFriendsResponse) : "Object should be of type " + FriendsAreFriendsResponse.class.getName() + ".  but instead is " + obj.getClass().getName();
  return (FriendsAreFriendsResponse)obj;
}

public Document friends_areFriends(Collection<Integer> userIds1,
                                   Collection<Integer> userIds2) throws FacebookException,
                                                                        IOException {
  assert (userIds1 != null && userIds2 != null);
  assert (!userIds1.isEmpty() && !userIds2.isEmpty());
  assert (userIds1.size() == userIds2.size());

  return this.callMethod(FacebookMethod.FRIENDS_ARE_FRIENDS,
                         new Pair<String, CharSequence>("uids1", delimit(userIds1)),
                         new Pair<String, CharSequence>("uids2", delimit(userIds2)));
}

public FriendsAreFriendsResponse friends_areFriendsJaxb(Collection<Integer> userIds1,
        Collection<Integer> userIds2) throws FacebookException,
                                             IOException {
        assert (userIds1 != null && userIds2 != null);
        assert (!userIds1.isEmpty() && !userIds2.isEmpty());
        assert (userIds1.size() == userIds2.size());

        Object obj = this.callMethod(FacebookMethod.FRIENDS_ARE_FRIENDS,
                        new Pair<String, CharSequence>("uids1", delimit(userIds1)),
                        new Pair<String, CharSequence>("uids2", delimit(userIds2)));

        assert (obj instanceof FriendsAreFriendsResponse) : "Object should be of type " + FriendsAreFriendsResponse.class.getName() + ".  but instead is " + obj.getClass().getName();
        return (FriendsAreFriendsResponse)obj;
}

/**
 * Retrieves the friends of the currently logged in user.
 * @return array of friends
 */
public Document friends_get() throws FacebookException, IOException {
  return this.callMethod(FacebookMethod.FRIENDS_GET);
}

/**
 * Retrieves the friends of the currently logged in user, who are also users
 * of the calling application.
 * @return array of friends
 */
public Document friends_getAppUsers() throws FacebookException, IOException {
  return this.callMethod(FacebookMethod.FRIENDS_GET_APP_USERS);
}

/**
 * Retrieves the requested info fields for the requested set of users.
 * @param userIds a collection of user IDs for which to fetch info
 * @param fields a set of ProfileFields
 * @return a Document consisting of a list of users, with each user element
 *   containing the requested fields.
 */
public Document users_getInfo(Collection<Integer> userIds,
                              EnumSet<ProfileField> fields) throws FacebookException,
                                                                   IOException {
  // assertions test for invalid params
  assert (userIds != null);
  assert (fields != null);
  assert (!fields.isEmpty());
  Document d = this.callMethod(FacebookMethod.USERS_GET_INFO,
          new Pair<String, CharSequence>("uids", delimit(userIds)),
          new Pair<String, CharSequence>("fields", delimit(fields)));

//TEST****************************************************************
  FacebookRestClient.printDom(d, "Gaston Debug |");
  return d;//this.callMethod(FacebookMethod.USERS_GET_INFO,
          //new Pair<String, CharSequence>("uids", delimit(userIds)),
          //new Pair<String, CharSequence>("fields", delimit(fields)));
}

public UsersGetInfoResponse users_getInfoJaxb(Collection<Integer> userIds,
        EnumSet<ProfileField> fields) throws FacebookException,
                                             IOException {
        // assertions test for invalid params
        assert (userIds != null);
        assert (fields != null);
        assert (!fields.isEmpty());

        return (UsersGetInfoResponse)this.callMethodJaxb(FacebookMethod.USERS_GET_INFO,
                        new Pair<String, CharSequence>("uids", delimit(userIds)),
                        new Pair<String, CharSequence>("fields", delimit(fields)));
}

/**
 * Retrieves the requested info fields for the requested set of users.
 * @param userIds a collection of user IDs for which to fetch info
 * @param fields a set of strings describing the info fields desired, such as "last_name", "sex"
 * @return a Document consisting of a list of users, with each user element
 *   containing the requested fields.
 */
public Document users_getInfo(Collection<Integer> userIds,
                              Set<CharSequence> fields) throws FacebookException, IOException {
  // assertions test for invalid params
  assert (userIds != null);
  assert (fields != null);
  assert (!fields.isEmpty());

  return this.callMethod(FacebookMethod.USERS_GET_INFO,
                         new Pair<String, CharSequence>("uids", delimit(userIds)),
                         new Pair<String, CharSequence>("fields", delimit(fields)));
}

public UsersGetInfoResponse users_getInfoJaxb(Collection<Integer> userIds,
        Set<CharSequence> fields) throws FacebookException, IOException {
        // assertions test for invalid params
        assert (userIds != null);
        assert (fields != null);
        assert (!fields.isEmpty());

        return (UsersGetInfoResponse)this.callMethod(FacebookMethod.USERS_GET_INFO,
                        new Pair<String, CharSequence>("uids", delimit(userIds)),
                        new Pair<String, CharSequence>("fields", delimit(fields)));
}


/**
 * Retrieves the user ID of the user logged in to this API session
 * @return the Facebook user ID of the logged-in user
 */
public int users_getLoggedInUser() throws FacebookException, IOException {
  Document d = this.callMethod(FacebookMethod.USERS_GET_LOGGED_IN_USER);
  return Integer.parseInt(d.getFirstChild().getNodeValue());//getTextContent());
}

/**
 * Retrieves the user object of the user logged in to this API session.
 * by default, retrieves all profile field.
 * @return the Facebook user object.
 */
public User users_getLoggedInUserJaxb() throws FacebookException, IOException {
      EnumSet<ProfileField> fields = EnumSet.allOf(ProfileField.class);
      return users_getLoggedInUserJaxb(fields);
}

/**
 * Retrieves the user object of the user logged in to this API session.
 * @param profileFields - the profile fields to retrieve.
 * @return the Facebook user object.
 */
public User users_getLoggedInUserJaxb(EnumSet<ProfileField> profileFields) throws FacebookException, IOException {
      List<Integer> uids = new ArrayList<Integer>();
      uids.add(users_getLoggedInUser());

      UsersGetInfoResponse ugir = users_getInfoJaxb(uids, profileFields);
      return ugir.getUser().get(0); // since this is the logged in user, should not be possible to get 0.
}

/**
 * Retrieves an indicator of whether the logged-in user has installed the
 * application associated with the _apiKey.
 * @return boolean indicating whether the user has installed the app
 */
public boolean users_isAppAdded() throws FacebookException, IOException {
  return extractBoolean(this.callMethod(FacebookMethod.USERS_IS_APP_ADDED));
}

/**
 * Used to retrieve photo objects using the search parameters (one or more of the
 * parameters must be provided).
 *
 * @param subjId retrieve from photos associated with this user (optional).
 * @param albumId retrieve from photos from this album (optional)
 * @param photoIds retrieve from this list of photos (optional)
 *
 * @return an Document of photo objects.
 */
public Document photos_get(Integer subjId, Long albumId,
                           Collection<Long> photoIds) throws FacebookException, IOException {
      ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_get(subjId, albumId, photoIds);
  return this.callMethod(FacebookMethod.PHOTOS_GET, params);
}

/**
 * see photos_get(Integer subjId, Long albumId,
        Collection<Long> photoIds)
 * @param subjId
 * @param albumId
 * @param photoIds
 * @return
 * @throws FacebookException
 * @throws IOException
 */
public PhotosGetResponse photos_getJaxb(Integer subjId, Long albumId,
        Collection<Long> photoIds) throws FacebookException, IOException {
        ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_get(subjId, albumId, photoIds);
        return (PhotosGetResponse)this.callMethodJaxb(FacebookMethod.PHOTOS_GET, params);
}

/**
 * build the parameters for photos_get() and photos_getJaxb() methods.
 *
 * it is an error to omit all 3 arguments.  they have no defaults.
 * @param subjId
 * @param albumId
 * @param photoIds
 * @return
 */
private ArrayList<Pair<String, CharSequence>> buildParamsPhotos_get(Integer subjId, Long albumId,
        Collection<Long> photoIds) {
  ArrayList<Pair<String, CharSequence>> params =
      new ArrayList<Pair<String, CharSequence>>(FacebookMethod.PHOTOS_GET.numParams());


    boolean hasUserId = null != subjId && 0 != subjId;
    boolean hasAlbumId = null != albumId && 0 != albumId;
    boolean hasPhotoIds = null != photoIds && !photoIds.isEmpty();
    assert (hasUserId || hasAlbumId || hasPhotoIds);

    if (hasUserId)
      params.add(new Pair<String, CharSequence>("subj_id", Integer.toString(subjId)));
    if (hasAlbumId)
      params.add(new Pair<String, CharSequence>("aid", Long.toString(albumId)));
    if (hasPhotoIds)
      params.add(new Pair<String, CharSequence>("pids", delimit(photoIds)));

    return params;
}

public Document photos_get(Long albumId, Collection<Long> photoIds) throws FacebookException,
                                                                           IOException {
  return photos_get(null/*subjId*/, albumId, photoIds);
}

public PhotosGetResponse photos_getJaxb(Long albumId, Collection<Long> photoIds) throws FacebookException, IOException {
        return photos_getJaxb(null/*subjId*/, albumId, photoIds);
}

public Document photos_get(Integer subjId, Collection<Long> photoIds) throws FacebookException,
                                                                             IOException {
  return photos_get(subjId, null/*albumId*/, photoIds);
}

public PhotosGetResponse photos_getJaxb(Integer subjId, Collection<Long> photoIds) throws FacebookException, IOException {
        return photos_getJaxb(subjId, null/*albumId*/, photoIds);
}

public Document photos_get(Integer subjId, Long albumId) throws FacebookException, IOException {
  return photos_get(subjId, albumId, null/*photoIds*/);
}

public PhotosGetResponse photos_getJaxb(Integer subjId, Long albumId) throws FacebookException, IOException {
        return photos_getJaxb(subjId, albumId, null/*photoIds*/);
}

public Document photos_get(Collection<Long> photoIds) throws FacebookException, IOException {
  return photos_get(null/*subjId*/, null/*albumId*/, photoIds);
}

public PhotosGetResponse photos_getJaxb(Collection<Long> photoIds) throws FacebookException, IOException {
  return photos_getJaxb(null/*subjId*/, null/*albumId*/, photoIds);
}

public Document photos_get(Long albumId) throws FacebookException, IOException {
  return photos_get(null/*subjId*/, albumId, null/*photoIds*/);
}

public PhotosGetResponse photos_getJaxb(Long albumId) throws FacebookException, IOException {
        return photos_getJaxb(null/*subjId*/, albumId, null/*photoIds*/);
}

public Document photos_get(Integer subjId) throws FacebookException, IOException {
  return photos_get(subjId, null/*albumId*/, null/*photoIds*/);
}

public PhotosGetResponse photos_getJaxb(Integer subjId) throws FacebookException, IOException {
  return photos_getJaxb(subjId, null/*albumId*/, null/*photoIds*/);
}

/**
 * Retrieves album metadata. Pass a user id and/or a list of album ids to specify the albums
 * to be retrieved (at least one must be provided)
 *
 * @param userId retrieve metadata for albums created the id of the user whose album you wish  (optional).
 * @param albumIds the ids of albums whose metadata is to be retrieved
 * @return album objects.
 */
public Document photos_getAlbums(Integer userId,
                                 Collection<Long> albumIds) throws FacebookException,
                                                                   IOException {
  boolean hasUserId = null != userId && userId != 0;
  boolean hasAlbumIds = null != albumIds && !albumIds.isEmpty();
  assert (hasUserId || hasAlbumIds); // one of the two must be provided

  if (hasUserId)
    return (hasAlbumIds) ?
           this.callMethod(FacebookMethod.PHOTOS_GET_ALBUMS, new Pair<String, CharSequence>("uid",
                                                                                            Integer.toString(userId)),
                           new Pair<String, CharSequence>("aids", delimit(albumIds))) :
           this.callMethod(FacebookMethod.PHOTOS_GET_ALBUMS,
                           new Pair<String, CharSequence>("uid", Integer.toString(userId)));
  else
    return this.callMethod(FacebookMethod.PHOTOS_GET_ALBUMS,
                           new Pair<String, CharSequence>("aids", delimit(albumIds)));
}

public Document photos_getAlbums(Integer userId) throws FacebookException, IOException {
  return photos_getAlbums(userId, null /*albumIds*/);
}

public Document photos_getAlbums(Collection<Long> albumIds) throws FacebookException,
                                                                   IOException {
  return photos_getAlbums(null /*userId*/, albumIds);
}

/**
 * Retrieves the tags for the given set of photos.
 * @param photoIds The list of photos from which to extract photo tags.
 * @return the created album
 */
public Document photos_getTags(Collection<Long> photoIds) throws FacebookException, IOException {
  return this.callMethod(FacebookMethod.PHOTOS_GET_TAGS,
                         new Pair<String, CharSequence>("pids", delimit(photoIds)));
}

/**
 * Creates an album.
 * @param albumName The list of photos from which to extract photo tags.
 * @return the created album
 */
public Document photos_createAlbum(String albumName) throws FacebookException, IOException {
  return this.photos_createAlbum(albumName, null/*description*/, null/*location*/);
}

/**
 * see photos_createAlbum(String albumName)
 */
public Album photos_createAlbumJaxb(String albumName) throws FacebookException, IOException {
  return this.photos_createAlbumJaxb(albumName, null/*description*/, null/*location*/);
}

/**
 * Creates an album.
 * @param name The album name.
 * @param location The album location (optional).
 * @param description The album description (optional).
 * @return an array of photo objects.
 */
public Document photos_createAlbum(String name, String description,
                                   String location) throws FacebookException, IOException {
        ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_createAlbum(name, description, location);
        return this.callMethod(FacebookMethod.PHOTOS_CREATE_ALBUM, params);
}

/**
 * see photos_createAlbum(String name, String description, String location)
 * @param name The album name.
 * @param location The album location (optional).
 * @param description The album description (optional).
 * @return an array of photo objects.
 */
public Album photos_createAlbumJaxb(String name, String description,
                                   String location) throws FacebookException, IOException {
        ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_createAlbum(name, description, location);
        Object obj = this.callMethodJaxb(FacebookMethod.PHOTOS_CREATE_ALBUM, params);
        return ((JAXBElement<Album>)obj).getValue();
}

/**
 * build the parameters for Photos_createAlbum() and Photos_createAlbumJaxb() methods.
 * @param name see photos_createAlbum(...)
 * @param description see photos_createAlbum(...)
 * @param location see photos_createAlbum(...)
 * @return parameter list to be used for photos_createAlbum()
 */
private ArrayList<Pair<String, CharSequence>> buildParamsPhotos_createAlbum(String name, String description,
        String location) {
        assert (null != name && !"".equals(name));
        ArrayList<Pair<String, CharSequence>> params =
                new ArrayList<Pair<String, CharSequence>>(FacebookMethod.PHOTOS_CREATE_ALBUM.numParams());
        params.add(new Pair<String, CharSequence>("name", name));
        if (null != description)
                params.add(new Pair<String, CharSequence>("description", description));
        if (null != location)
                params.add(new Pair<String, CharSequence>("location", location));
        return params;
}

/**
 * Adds several tags to a photo.
 * @param photoId The photo id of the photo to be tagged.
 * @param tags A list of PhotoTags.
 * @return a list of booleans indicating whether the tag was successfully added.
 */
public Document photos_addTags(Long photoId, Collection<PhotoTag> tags)
  throws FacebookException, IOException, JSONException {
  assert (photoId > 0);
  assert (null != tags && !tags.isEmpty());
  JSONWriter tagsJSON = new JSONStringer().array();
  for (PhotoTag tag: tags)
    tagsJSON = null;//tag.jsonify(tagsJSON);
  String tagStr = tagsJSON.endArray().toString();

  return this.callMethod(FacebookMethod.PHOTOS_ADD_TAG,
                         new Pair<String, CharSequence>("pid", photoId.toString()),
                         new Pair<String, CharSequence>("tags", tagStr));
}

/**
 * Adds several tags to a photo.
 * @param photoId The photo id of the photo to be tagged.
 * @param tags A list of PhotoTags.
 * @return boolean indicating whether the tag was successfully added.
 *    some weirdness here.  even when i add 2 tags at the same time,
 *    i still get only one boolean value in the returned document.
 *
 */
public boolean photos_addTagsJaxb(Long photoId, Collection<PhotoTag> tags)
  throws FacebookException, IOException, JSONException {
  assert (photoId > 0);
  assert (null != tags && !tags.isEmpty());
  JSONWriter tagsJSON = new JSONStringer().array();
  for (PhotoTag tag: tags)
    tagsJSON = null;//tag.jsonify(tagsJSON);
  String tagStr = tagsJSON.endArray().toString();


  Object obj = this.callMethodJaxb(FacebookMethod.PHOTOS_ADD_TAG,
                         new Pair<String, CharSequence>("pid", photoId.toString()),
                         new Pair<String, CharSequence>("tags", tagStr));

  System.out.println("type=" + ((JAXBElement)obj).getDeclaredType());
  return ((JAXBElement<Boolean>)obj).getValue().booleanValue();
}

/**
 * Adds a tag to a photo.
 * @param photoId The photo id of the photo to be tagged.
 * @param xPct The horizontal position of the tag, as a percentage from 0 to 100, from the left of the photo.
 * @param yPct The vertical position of the tag, as a percentage from 0 to 100, from the top of the photo.
 * @param taggedUserId The list of photos from which to extract photo tags.
 * @return whether the tag was successfully added.
 */
public boolean photos_addTag(Long photoId, Integer taggedUserId, Double xPct,
                             Double yPct) throws FacebookException, IOException {
  return photos_addTag(photoId, xPct, yPct, taggedUserId, null);
}

/**
 * Adds a tag to a photo.
 * @param photoId The photo id of the photo to be tagged.
 * @param xPct The horizontal position of the tag, as a percentage from 0 to 100, from the left of the photo.
 * @param yPct The list of photos from which to extract photo tags.
 * @param tagText The text of the tag.
 * @return whether the tag was successfully added.
 */
public boolean photos_addTag(Long photoId, CharSequence tagText, Double xPct,
                             Double yPct) throws FacebookException, IOException {
  return photos_addTag(photoId, xPct, yPct, null, tagText);
}

private boolean photos_addTag(Long photoId, Double xPct, Double yPct, Integer taggedUserId,
                              CharSequence tagText) throws FacebookException, IOException {
  assert (null != photoId && !photoId.equals(0));
  assert (null != taggedUserId || null != tagText);
  assert (null != xPct && xPct >= 0 && xPct <= 100);
  assert (null != yPct && yPct >= 0 && yPct <= 100);
  Document d =
    this.callMethod(FacebookMethod.PHOTOS_ADD_TAG, new Pair<String, CharSequence>("pid",
                                                                                  photoId.toString()),
                    new Pair<String, CharSequence>("tag_uid", taggedUserId.toString()),
                    new Pair<String, CharSequence>("x", xPct.toString()),
                    new Pair<String, CharSequence>("y", yPct.toString()));
  return extractBoolean(d);
}

public Document photos_upload(File photo) throws FacebookException, IOException {
  return /* caption */ /* albumId */photos_upload(photo, null, null);
}

public Photo photos_uploadJaxb(File photo) throws FacebookException, IOException {
  return /* caption */ /* albumId */photos_uploadJaxb(photo, null, null);
}

public Document photos_upload(File photo, String caption) throws FacebookException, IOException {
  return /* albumId */photos_upload(photo, caption, null);
}

public Photo photos_uploadJaxb(File photo, String caption) throws FacebookException, IOException {
  return /* albumId */photos_uploadJaxb(photo, caption, null);
}

public Document photos_upload(File photo, Long albumId) throws FacebookException, IOException {
  return /* caption */photos_upload(photo, null, albumId);
}

public Photo photos_uploadJaxb(File photo, Long albumId) throws FacebookException, IOException {
  return /* caption */photos_uploadJaxb(photo, null, albumId);
}

public Document photos_upload(File photo, String caption, Long albumId) throws FacebookException,
                                                                               IOException {
      ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_upload(photo, caption, albumId);
  return callMethod(FacebookMethod.PHOTOS_UPLOAD, params);
}

public Photo photos_uploadJaxb(File photo, String caption, Long albumId) throws FacebookException,
                                                                                                                                                              IOException {
      ArrayList<Pair<String, CharSequence>> params = buildParamsPhotos_upload(photo, caption, albumId);
      Object obj = callMethodJaxb(FacebookMethod.PHOTOS_UPLOAD, params);
      return ((JAXBElement<Photo>)obj).getValue();
}

/**
 * build parameters for photos_upload(...).
 * @param photo - see photos_upload(...)
 * @param caption - see photos_upload(...)
 * @param albumId - see photos_upload(...)
 * @return parameters list to be used for photos_upload(...)
 */
private ArrayList<Pair<String, CharSequence>> buildParamsPhotos_upload(File photo, String caption, Long albumId) {
      ArrayList<Pair<String, CharSequence>> params =
              new ArrayList<Pair<String, CharSequence>>(FacebookMethod.PHOTOS_UPLOAD.numParams());
      assert (photo.exists() && photo.canRead());
      this._uploadFile = photo;
      if (null != albumId) {
              params.add(new Pair<String, CharSequence>("aid", Long.toString(albumId)));
      }
      if (null != caption) {
              params.add(new Pair<String, CharSequence>("caption", caption));
      }
      return params;
}

/**
 * Retrieves the groups associated with a user
 * @param userId Optional: User associated with groups.
 *  A null parameter will default to the session user.
 * @param groupIds Optional: group ids to query.
 *   A null parameter will get all groups for the user.
 * @return array of groups
 */
public Document groups_get(Integer userId, Collection<Long> groupIds) throws FacebookException,
                                                                             IOException {
  boolean hasGroups = (null != groupIds && !groupIds.isEmpty());
  if (null != userId)
    return hasGroups ?
           this.callMethod(FacebookMethod.GROUPS_GET, new Pair<String, CharSequence>("uid",
                                                                                     userId.toString()),
                           new Pair<String, CharSequence>("gids", delimit(groupIds))) :
           this.callMethod(FacebookMethod.GROUPS_GET,
                           new Pair<String, CharSequence>("uid", userId.toString()));
  else
    return hasGroups ?
           this.callMethod(FacebookMethod.GROUPS_GET, new Pair<String, CharSequence>("gids",
                                                                                     delimit(groupIds))) :
           this.callMethod(FacebookMethod.GROUPS_GET);
}

public GroupsGetResponse groups_getJaxb(Integer userId, Collection<Long> groupIds)
throws FacebookException, IOException {
        boolean hasGroups = (null != groupIds && !groupIds.isEmpty());
        if (null != userId) {
                return (GroupsGetResponse)
                              ( hasGroups ?
                                this.callMethodJaxb(FacebookMethod.GROUPS_GET, new Pair<String, CharSequence>("uid",
                                                userId.toString()),
                                                new Pair<String, CharSequence>("gids", delimit(groupIds))) :
                                                        this.callMethodJaxb(FacebookMethod.GROUPS_GET,
                                                                        new Pair<String, CharSequence>("uid", userId.toString())) );
        } else {
                return (GroupsGetResponse)
                              ( hasGroups ?
                                this.callMethodJaxb(FacebookMethod.GROUPS_GET, new Pair<String, CharSequence>("gids",
                                                delimit(groupIds))) :
                                                        this.callMethodJaxb(FacebookMethod.GROUPS_GET) );
        }
}


/**
 * Retrieves the membership list of a group
 * @param groupId the group id
 * @return a Document containing four membership lists of
 *  'members', 'admins', 'officers', and 'not_replied'
 */
public Document groups_getMembers(Number groupId) throws FacebookException, IOException {
  assert (null != groupId);
  return this.callMethod(FacebookMethod.GROUPS_GET_MEMBERS,
                         new Pair<String, CharSequence>("gid", groupId.toString()));
}

public GroupMembers groups_getMembersJaxb(Number groupId) throws FacebookException, IOException {
        assert (null != groupId);
        JAXBElement<GroupMembers> elem = (JAXBElement<GroupMembers>)
              this.callMethodJaxb(FacebookMethod.GROUPS_GET_MEMBERS,
                        new Pair<String, CharSequence>("gid", groupId.toString()));
        return elem.getValue();
}


/**
 * Retrieves the results of a Facebook Query Language query
 * @param query : the FQL query statement
 * @return varies depending on the FQL query
 */
public Document fql_query(CharSequence query) throws FacebookException, IOException {
  assert (null != query);
  return this.callMethod(FacebookMethod.FQL_QUERY,
                         new Pair<String, CharSequence>("query", query));
}

/**
 * Retrieves the outstanding notifications for the session user.
 * @return a Document containing
 *  notification count pairs for 'messages', 'pokes' and 'shares',
 *  a uid list of 'friend_requests', a gid list of 'group_invites',
 *  and an eid list of 'event_invites'
 */
public Document notifications_get() throws FacebookException, IOException {
  return this.callMethod(FacebookMethod.NOTIFICATIONS_GET);
}

/**
 * see notifications_get()
 */
public Notifications notifications_getJaxb() throws FacebookException, IOException {
        Object obj = this.callMethodJaxb(FacebookMethod.NOTIFICATIONS_GET);
        JAXBElement<Notifications> elem = (JAXBElement<Notifications>) obj;
        return elem.getValue();
}


/**
 * Send a request or invitations to the specified users.
 * @param recipientIds the user ids to which the request is to be sent
 * @param type the type of request/invitation - e.g. the word "event" in "1 event invitation."
 * @param content Content of the request/invitation. This should be FBML containing only links and the
 *   special tag &lt;fb:req-choice url="" label="" /&gt; to specify the buttons to be included in the request.
 * @param image URL of an image to show beside the request. It will be resized to be 100 pixels wide.
 * @param isInvite whether this is a "request" or an "invite"
 * @return a URL, possibly null, to which the user should be redirected to finalize
 *    the sending of the message
 */
public URL notifications_sendRequest(Collection<Integer> recipientIds, CharSequence type,
CharSequence content, URL image, boolean isInvite) throws FacebookException, IOException {
  assert (null != recipientIds && !recipientIds.isEmpty());
  assert (null != type);
  assert (null != content);
  assert (null != image);

  Document d =
    this.callMethod(FacebookMethod.NOTIFICATIONS_SEND_REQUEST,
                    new Pair<String, CharSequence>("to_ids", delimit(recipientIds)),
                    new Pair<String, CharSequence>("type", type),
                    new Pair<String, CharSequence>("content", content),
                    new Pair<String, CharSequence>("image", image.toString()),
                    new Pair<String, CharSequence>("invite", isInvite ? "1" : "0"));
  String url = d.getFirstChild().getTextContent();
  return (null == url || "".equals(url)) ? null : new URL(url);
}

/**
 * Send a notification message to the specified users.
 * @param recipientIds the user ids to which the message is to be sent
 * @param subject the subject of the message
 * @param body the body of the message
 * @return a URL, possibly null, to which the user should be redirected to finalize
 *    the sending of the message
 */
public URL notifications_send(Collection<Integer> recipientIds,
                              CharSequence subject,
                              CharSequence body) throws FacebookException, IOException {
  assert (null != recipientIds && !recipientIds.isEmpty());
  assert (null != subject);
  assert (null != body);
  Document d =
    this.callMethod(FacebookMethod.NOTIFICATIONS_SEND,
                    new Pair<String, CharSequence>("to_ids", delimit(recipientIds)),
                    new Pair<String, CharSequence>("subject", subject),
                    new Pair<String, CharSequence>("body", body));
  String url = d.getFirstChild().getTextContent();
  return (null == url || "".equals(url)) ? null : new URL(url);
}

protected static boolean extractBoolean(Document doc) {
  String content = doc.getFirstChild().getTextContent();
  return 1 == Integer.parseInt(content);
}

protected static final String CRLF = "\r\n";
protected static final String PREF = "--";
protected static final int UPLOAD_BUFFER_SIZE = 512;

public InputStream postFileRequest(String methodName,
                                   Map<String, CharSequence> params) throws IOException {
  assert (null != _uploadFile);
  try {
    BufferedInputStream bufin = new BufferedInputStream(new FileInputStream(_uploadFile));

    String boundary = Long.toString(System.currentTimeMillis(), 16);
    URLConnection con = SERVER_URL.openConnection();
    con.setDoInput(true);
    con.setDoOutput(true);
    con.setUseCaches(false);
    con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    con.setRequestProperty("MIME-version", "1.0");

    DataOutputStream out = new DataOutputStream(con.getOutputStream());

    for (Map.Entry<String, CharSequence> entry: params.entrySet()) {
      out.writeBytes(PREF + boundary + CRLF);
      out.writeBytes("Content-disposition: form-data; name=\"" + entry.getKey() + "\"");
      out.writeBytes(CRLF + CRLF);
      out.writeBytes(entry.getValue().toString());
      out.writeBytes(CRLF);
    }

    out.writeBytes(PREF + boundary + CRLF);
    out.writeBytes("Content-disposition: form-data; filename=\"" + _uploadFile.getName() + "\"" +
                   CRLF);
    out.writeBytes("Content-Type: image/jpeg" + CRLF);
    // out.writeBytes("Content-Transfer-Encoding: binary" + CRLF); // not necessary

    // Write the file
    out.writeBytes(CRLF);
    byte b[] = new byte[UPLOAD_BUFFER_SIZE];
    int byteCounter = 0;
    int i;
    while (-1 != (i = bufin.read(b))) {
      byteCounter += i;
      out.write(b, 0, i);
    }
    out.writeBytes(CRLF + PREF + boundary + PREF + CRLF);

    out.flush();
    out.close();

    InputStream is = con.getInputStream();
    return is;
  }
  catch (Exception e) {
    System.out.println("exception: " + e.getMessage());
    e.printStackTrace();
    return null;
  }
}

/**
 * Call this function and store the result, using it to generate the
 * appropriate login url and then to retrieve the session information.
 * @return String the auth_token string
 */
public String auth_createToken() throws FacebookException, IOException {
  Document d = this.callMethod(FacebookMethod.AUTH_CREATE_TOKEN);
  return d.getFirstChild().getTextContent();
}

/**
 * Call this function to retrieve the session information after your user has
 * logged in.
 * @param authToken the token returned by auth_createToken or passed back to your callback_url.
 */
public String auth_getSession(String authToken) throws FacebookException, IOException {
  Document d =
    this.callMethod(FacebookMethod.AUTH_GET_SESSION, new Pair<String, CharSequence>("auth_token",
                                                                                    authToken.toString()));
  FacebookRestClient.printDom(d,  "Gaston Debug | ");
  this._sessionKey =
      d.getElementsByTagName("session_key").item(0).getFirstChild().getNodeValue();//getTextContent();
  this._userId =
      Integer.parseInt(d.getElementsByTagName("uid").item(0).getFirstChild().getNodeValue());//getTextContent());
  if (this._isDesktop)
    this._sessionSecret =
        d.getElementsByTagName("secret").item(0).getFirstChild().getNodeValue();//getTextContent();
  return this._sessionKey;
}

/**
 * note: not part of the original facebook api.
 * can only be used after
 * facebookRestClient.auth_getSession(authToken)
 * @return userid
 */
public int getUserId() { return _userId; }

}
