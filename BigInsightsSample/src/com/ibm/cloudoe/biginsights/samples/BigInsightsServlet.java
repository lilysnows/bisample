package com.ibm.cloudoe.biginsights.samples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * This is a sample Servlet that demonstrates how VCAP_SERVICES environment variable can be leveraged to obtain user
 * credentials and access URLs to BigInsights services. This sample also shows how to connect with BigSQL database and
 * perform various operations.
 * <p>
 * This servlet handles both GET and POST requests in an identical manner. The following operations are supported:
 * <ol>
 * <li>Load data into BigSQL database</li>
 * <li>Query BigSQL database</li>
 * </ol>
 * </p>
 */
@WebServlet("/biginsights")
public class BigInsightsServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  // BEGIN: Operations ===========================
  /** Loads the specified BigSheets output file in TSV format into BigSQL database */
  private static final String OP_LOAD = "load";

  /** Retrieves data from BigSQL database */
  private static final String OP_FETCH = "fetch";
  // END: Operations ===========================

  // BEGIN: param names ===========================
  private static final String PARAM_OPERATION = "operation";
  private static final String PARAM_TABLE_NAME = "tableName";
  private static final String PARAM_TSV_PATH = "tsvPath";
  // END: param names ===========================

  // BEGIN: Environment variables ===========================
  private String userName;
  private String password;
  private String bigSqlUrl;

  // END: Environment variables ===========================

  /**
   * @see HttpServlet#HttpServlet()
   */
  public BigInsightsServlet ()
  {
    super ();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    process (request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    process (request, response);
  }

  /**
   * Processes the HttpRequest and generates a HttpResponse
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  private void process (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    // read the environment variable VCAP_SERVICES to fetch username, password, BigSQLJdbcURL etc
    readEnv ();

    // read the operation type and act accordingly
    String operation = request.getParameter (PARAM_OPERATION);
    try {
      if (OP_LOAD.equals (operation)) {
        loadIntoHive (request);
        response.getWriter ().write ("Loading of data successful.");
      }
      else if (OP_FETCH.equals (operation)) {
        Map<String, String> result = queryBigSQL (request);

        response.getWriter ().write (toJSON (result));
      }
    }
    catch (Exception e) {
      e.printStackTrace ();
      response.getWriter ().write (e.getMessage ());
    }
  }

  private String toJSON (Map<String, String> result)
  {
    List<String> entries = new ArrayList<String> ();

    for (String key : result.keySet ()) {
      String value = result.get (key);
      entries.add (String.format ("{\"key\": \"%s\", \"value\": \"%s\"}", key, value));
    }
    System.out.println (entries.toString ());
    return entries.toString ();
  }

  /**
   * Fetches the url attribute for all blog records pertaining to a specified country.
   * 
   * @param request HttpRequest containing parameters from the user
   * @return List of matches from the BigSQL table
   * @throws Exception
   */
  private Map<String, String> queryBigSQL (HttpServletRequest request) throws Exception
  {
    System.out.println ("queryBigSQL()");
    String bigSqlTable = request.getParameter (PARAM_TABLE_NAME);

    BigSQLJdbcClient client = new BigSQLJdbcClient (userName, password, bigSqlUrl);
    return client.getData (bigSqlTable);
  }

  /**
   * Loads a TSV into Hive. The path of the TSV and the BigSQL table are fetched from request parameter.
   * 
   * @param request HttpRequest containing parameters from the user
   * @throws Exception
   */
  private void loadIntoHive (HttpServletRequest request) throws Exception
  {
    System.out.println ("loadIntoHive");
    String tableName = request.getParameter (PARAM_TABLE_NAME);
    String sheetsOutputFile = request.getParameter (PARAM_TSV_PATH);

    BigSQLJdbcClient client = new BigSQLJdbcClient (userName, password, bigSqlUrl);
    client.loadData (tableName, sheetsOutputFile);

  }

  /**
   * Reads several parameters from VCAP_SERVICES environment variable
   * 
   * @throws IOException
   */
  private void readEnv () throws IOException
  {
    System.out.println ("In readEnv()");
    Map<String, String> env = System.getenv ();
    String vcap = env.get ("VCAP_SERVICES");

    // parse the VCAP JSON structure
    JSONObject obj = JSONObject.parse (vcap);
    for (Iterator<?> iter = obj.keySet ().iterator (); iter.hasNext ();) {
      String key = (String) iter.next ();
      if (key.startsWith (Constants.VCAP_BIGINSIGHTS_SERVICE_KEY)) {
      JSONArray val = (JSONArray) obj.get (key) != null ? (JSONArray) obj.get (key) : null;
        if (val != null) {
          JSONObject serviceAttr = val.get (0) != null ? (JSONObject) val.get (0) : null;

          // read credentials
          Object credObj = serviceAttr.get (Constants.VCAP_CREDENTIALS);
          JSONObject credentials = serviceAttr != null ? (credObj != null ? (JSONObject) credObj : null) : null;

          // read username
          Object userNameObj = credentials.get (Constants.VCAP_USER_NAME);
          userName = (userNameObj != null) ? (String) userNameObj : "";

          // read password
          Object pwdObj = credentials.get (Constants.VCAP_PASSWORD);
          password = (pwdObj != null) ? (String) pwdObj : "";

          // read BigSQL url
          Object bigSqlUrlObj = credentials.get (Constants.VCAP_BIGSQL_URL);
          bigSqlUrl = (bigSqlUrlObj != null) ? (String) bigSqlUrlObj : "";
          break;
        }
      }
    }

  }
}
