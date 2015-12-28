package com.testFaceBook1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.facebook.api.FacebookException;
//import com.facebook.api.FacebookRestClient;
import com.facebook.api.ProfileField;

/**
 * Servlet implementation class for Servlet: Log
 *
 */
 public class Log extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
	static private	String apiKey = null;
	static private String secretKey = null;

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public Log() {
		super();
	}

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		apiKey = getServletConfig().getInitParameter("API_KEY");
		secretKey = getServletConfig().getInitParameter("SECRET_KEY");
		int uid = 0;


		while (request.getAttributeNames().hasMoreElements()){
			System.out.println("At: " + request.getAttributeNames().nextElement().toString());
		}

		Enumeration<Object> a = request.getParameterNames();
		while (a.hasMoreElements()){
			System.out.println("PA: " + a.nextElement().toString());
		}


		FacebookRestClient frc = null;
		HttpSession session = request.getSession();
		String sessionKey = null;//(String) session.getAttribute("facebookSession");
		String token = request.getParameter("auth_token");



		frc = doLogin(response, request, apiKey, secretKey, token, session);




		if(frc!=null){

			/*USAMOS API FACEBOOK*/
			uid=frc.getUserId();


			try {
				getFacebookInfo(request, frc);//get profile info
				get_Friends_Uid(frc, request, response);//get friends uid

			} catch (FacebookException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
	}

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	//--------------------------------------------->Logeo y autenticacion

	protected FacebookRestClient doLogin(HttpServletResponse response, ServletRequest request, String apiKey, String secretKey, String token, HttpSession session) throws IOException{
		FacebookRestClient frc = null;
		String sessionKey = null;


		System.out.println("auth_token: "+token);

			if (sessionKey != null && sessionKey.length() > 0) {

			frc = new FacebookRestClient(apiKey, secretKey, sessionKey);


			} else if (token != null) {

			session.setAttribute("auth_token", token);
			frc = new FacebookRestClient(apiKey, secretKey);
			frc.setIsDesktop(false);

			try{
				sessionKey = frc.auth_getSession(token);
				session.setAttribute("facebookSession", sessionKey);

				frc.setDebug(true);

				}catch(FacebookException e){
				e.printStackTrace();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
			response.sendRedirect("http://www.facebook.com/login.php?api_key=" + apiKey + "&v=1.0");
			return null;
			}
		return frc;
	}




	//--------------------------------------------->Usos de facebook


	protected void 	get_Friends_Uid(FacebookRestClient client, HttpServletRequest req, HttpServletResponse res)
	throws IOException, FacebookException, ServletException {

		Document root = (Document)client.friends_get();
		NodeList list = root.getElementsByTagName("uid");

		client.printDom(root, "Debug en loginSucessfull |");//TEST

		ArrayList friends = new ArrayList();

		for(int i=0; i<list.getLength(); ++i) {
			String uid = list.item(i).getFirstChild().getNodeValue();
			friends.add(uid);

		}

		req.setAttribute("friends", friends);
		System.out.print(friends);
		getServletContext().getRequestDispatcher("/main_page.jsp").forward(req, res);

	}




	 protected static boolean getFacebookInfo(
	         HttpServletRequest request,
	         FacebookRestClient facebook)
	   {

		   try {
			   Integer userID = 0;
		         try{System.out.println("*0*-----------getFacebookinfo------------*0*");
		        	 userID = facebook.getUserId();
		        	 System.out.println(userID);
		         }catch(Exception e){
		        	 System.out.
		        	 println("**-----------error trying getFacebookinfo" +
		        	 		"------------**");}

		         Collection<Integer> users = new ArrayList<Integer>();
		         users.add(userID);


		         System.out.println("**-----------try getFacebookinfo------------**");

		         EnumSet<ProfileField> fields = EnumSet.of (
		            com.facebook.api.ProfileField.NAME,
		            com.facebook.api.ProfileField.PIC);

		         Document d = facebook.users_getInfo(users, fields);

		        // facebook.printDom(d, "Debug en get info |");//TEST


		         String name =
		            d.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();//.getLocalName();
		         String picture =
		            d.getElementsByTagName("pic").item(0).getFirstChild().getNodeValue();//.getLocalName();



		         request.setAttribute("uid", userID);
		         request.setAttribute("profile_name", name);
		         request.setAttribute("profile_picture_url", picture);

		      } catch (FacebookException e) {

		    	  System.out.println("**-----------error en GFI------------**");
		         HttpSession session = request.getSession();
		         session.setAttribute("facebookSession", null);
		         return false;

		      } catch (IOException e) {

		         e.printStackTrace();
		         return false;
		      }
		      return true;

	   }
}
