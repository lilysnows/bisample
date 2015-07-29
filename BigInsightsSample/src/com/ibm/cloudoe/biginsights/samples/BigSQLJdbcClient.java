package com.ibm.cloudoe.biginsights.samples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample client code to access BigSQL database using JDBC
 * 
 * @author IBM
 */
public class BigSQLJdbcClient
{
  private static final String BIGSQL_JDBC_DRIVER = "com.ibm.db2.jcc.DB2Driver";
  private static final String BIGSQL_V1_JDBC_DRIVER = "com.ibm.biginsights.bigsql.jdbc.BigSQLDriver";

  private String userName;
  private String password;
  private String bigSqlUrl;

  public BigSQLJdbcClient (String userName, String password, String bigSqlUrl)
  {
    super ();
    this.userName = userName;
    this.password = password;
    this.bigSqlUrl = bigSqlUrl;
  }

  public void loadData (String tableName, String sheetsOutputFile) throws Exception
  {
    System.out.println ("Enter: loadData()");

    Connection con = null;
    Statement stmt = null;
    try {
      con = connectToBigSQL ();
      System.out.println ("Obtained connection");
      stmt = con.createStatement ();

      String dbName = getDBName (bigSqlUrl);

      useDatabase (stmt, dbName);
      createTable (stmt, tableName);
      System.out.println ("Table created successfully");
      loadData (stmt, tableName, sheetsOutputFile);
      System.out.println ("Data loaded successfully");
    }

    finally {
      if (stmt != null) {
        stmt.close ();
      }

      if (con != null) {
        con.close ();
      }
    }
  }

  /**
   * Fetches all records from the table
   * 
   * @param bigSqlTable Name of the table to query
   * @return List of matches
   * @throws Exception
   */
  public Map<String, String> getData (String bigSqlTable) throws Exception
  {
    long startTime = System.currentTimeMillis ();
    Map<String, String> result = new HashMap<String, String> ();

    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      con = connectToBigSQL ();
      stmt = con.createStatement ();

      String dbName = getDBName (bigSqlUrl);
      useDatabase (stmt, dbName);

      // Execute query and prepare result
      rs = stmt.executeQuery ("select * from " + bigSqlTable);

      int rowCount = 0;
      while (rs.next () && rowCount < 10) {
        rowCount++;
        String lang = rs.getString ("language");
        String subject = rs.getString ("subjecthtml");

        // trim to fit display size
        String trimmed = (subject.length () < 50) ? subject : subject.substring (0, 50);
        result.put (lang, trimmed);
      }
    }
    finally {

      if (rs != null) {
        rs.close ();
      }

      if (stmt != null) {
        stmt.close ();
      }

      if (con != null) {
        con.close ();
      }
    }
    long endTime = System.currentTimeMillis ();
    System.out.println ("BigSQL: getData() took " + (endTime - startTime) / 1000 + " seconds");
    return result;
  }

  /**
   * Loads the specified BigSheets output file into the BigSQL database specified by the URL {@link #bigSqlUrl}
   * 
   * @param stmt Jdbc statement
   * @param sheetsOutputFile absolute path of the BigSheets output file in TSV format
   * @param tableName name of the table in to which data should be loaded
   * @throws SQLException
   */
  private void loadData (Statement stmt, String tableName, String sheetsOutputFile) throws SQLException
  {
    String sql = String.format (
      "load hadoop using file url '%s' with SOURCE PROPERTIES ('field.delimiter'='\t')  INTO TABLE %s overwrite",
      sheetsOutputFile, tableName);
    stmt.execute (sql);
  }

  /**
   * Creates a table in the database as specified by {@link #bigSqlUrl}
   * 
   * @param stmt Jdbc statement
   * @param tableName name of the table to create
   * @throws SQLException
   */
  private void createTable (Statement stmt, String tableName) throws SQLException
  {
    String sql = "create hadoop table IF NOT EXISTS " + tableName + "(Country varchar(2),FeedInfo varchar(300), "
      + "Language varchar(25),Published varchar(25), " + "SubjectHtml varchar(300),Tags varchar(100), "
      + "Type varchar(20), Url varchar(100)) " + "row format delimited fields terminated by '\t'";

    stmt.execute (sql);
  }

  /**
   * Sets the specified database as the current database, so that SQLs executed after this need not bear the schema name
   * for tables.
   * 
   * @param stmt Jdbc statement
   * @param dbName database name
   * @throws SQLException
   */
  private void useDatabase (Statement stmt, String dbName) throws SQLException
  {
    stmt.execute ("use " + dbName);
  }

  /**
   * Retrieves the dbName portion of the URL
   * 
   * @param url BigSQL url
   * @return dbName portion of the url
   */
  private String getDBName (String url)
  {
    int idx = url.lastIndexOf ('/');
    return url.substring (idx + 1);
  }

  /**
   * Obtains a connection to BigSQL and returns a Connection object
   * 
   * @return Connection object
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  private Connection connectToBigSQL () throws ClassNotFoundException, SQLException
  {
    // Register JDBC driver
    Class.forName (BIGSQL_JDBC_DRIVER);

    // Get a connection
    Connection con = DriverManager.getConnection (bigSqlUrl, userName, password);
    return con;
  }

  /**
   * Loads the specified BigSheets output file into the BigSQL database specified by the URL {@link #bigSqlUrl}. Uses
   * BigSQL v1.0.
   * 
   * @param stmt Jdbc statement
   * @param sheetsOutputFile absolute path of the BigSheets output file in TSV format
   * @param tableName name of the table in to which data should be loaded
   * @throws SQLException
   */
  private void loadData_V1 (Statement stmt, String tableName, String sheetsOutputFile) throws SQLException
  {
    String sql = String.format ("load hive data inpath '%s' overwrite into table %s", sheetsOutputFile, tableName);

    stmt.execute (sql);
  }

  /**
   * Creates a table in the database as specified by {@link #bigSqlUrl}. Uses BigSQL v1 connectivity mode.
   * 
   * @param stmt Jdbc statement
   * @param tableName name of the table to create
   * @throws SQLException
   */
  private void createTable_V1 (Statement stmt, String tableName) throws SQLException
  {
    String sql = "create table " + tableName + "(Country char(2),FeedInfo varchar(300), "
      + "Language char(25),Published char(25), " + "SubjectHtml varchar(300),Tags varchar(100), "
      + "Type char(20), Url varchar(100)) " + "row format delimited fields terminated by '\t'";

    stmt.execute (sql);
  }
}
