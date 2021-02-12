package com.currency.currency_converter;

import org.hsqldb.jdbc.JDBCDataSource;

import java.sql.*;

public class JDBC {

    // memory mode
    private static String database = "jdbc:hsqldb:mem:database";

    // User that connect to the database
    private static String userDB = "currency";

    // Password to connect to the database
    private static String passwordDB = "";

    /**
     * Execute the request given
     * @param request SQL request
     * @throws SQLException SQL exception
     */
    public static void executerUpdate(Connection connection, String request) throws SQLException
    {
        Statement statement;
        statement = connection.createStatement();
        statement.executeUpdate(request);
    }

    /**
     * Execute the request given
     * @param request SQL request
     * @throws SQLException SQL exception
     */
    public static ResultSet executerRequete(Connection connection, String request) throws SQLException
    {
        Statement statement;
        statement = connection.createStatement();
        ResultSet result = statement.executeQuery(request);
        return result;
    }

    // Create database's tables
    public static void initTable(Connection connection) throws SQLException
    {
        // Create a table nammed "base" which corresponds to a reference currency
        String requestTableBase = "CREATE TABLE base (idBase INTEGER IDENTITY, name VARCHAR(256) NOT NULL, date DATE NOT NULL);";

        // Create a table nammed "rate" which corresponds to the rates of a reference currency
        String requestTableRate = "CREATE TABLE rate (" +
                "idRate INTEGER IDENTITY," +
                "name VARCHAR(256) NOT NULL," +
                "value REAL NOT NULL," +
                "idBase INTEGER NOT NULL);";

        executerUpdate(connection,requestTableBase);
        executerUpdate(connection,requestTableRate);
    }

    // Insert information to the bases table
    public static void insertBase(String name, String date, Connection connection) throws Exception {
        Date date_sql = Date.valueOf(date);
        String requestBase = "INSERT INTO base(name, date) VALUES('" + name + "', '" + date_sql +"');";
        executerUpdate(connection, requestBase);
    }

    // Insert information to the rates table
    public static void insertRate(String name, float value, String name_base, Connection connection) throws Exception {
        int idBase = getIdBaseByName(name_base, connection);

        String requestRate = "INSERT INTO rate(name, value, idBase) VALUES ('" +
                name + "', " +
                value + ", " +
                idBase + ");";

        executerUpdate(connection, requestRate);
    }

    // Get idBase of a base through the name of this base
    public static int getIdBaseByName(String name, Connection connection) throws Exception {
        String requestSelectBase = "SELECT idBase FROM base WHERE name = '" + name + "';";
        ResultSet result = executerRequete(connection, requestSelectBase);
        if (result.next()) {
            return result.getInt("idBase");
        }else throw new Exception("There is no base for this name (" + name + ").");
    }

    // Get rate's name through the id of this rate
    public static String getRateNameById(int id, Connection connection) throws Exception {
        String requeteSelectRate = "SELECT name FROM rate WHERE idRate = " + id + ";";
        ResultSet result = executerRequete(connection, requeteSelectRate);
        if (result.next()) {
            return result.getString("name");
        }else throw new Exception("There is no rate for this id (" + id + ").");
    }

    // Get rate's value through the name of this rate and the base
    public static float getRateByNameAndBase(String name, String base, Connection connection) throws Exception {
        // If the rate asked is the base, the rate value equals to 1
        if (name == base) {
            return 1;
        }

        int idBase = getIdBaseByName(base, connection);
        String requestSelectRate = "SELECT value FROM rate WHERE name = '" + name + "' and idBase = " + idBase + ";";
        ResultSet result = executerRequete(connection, requestSelectRate);

        if (result.next()) {
            return result.getFloat("value");
        }else throw new Exception("There is no rate for this name (" + name + ").");
    }

    // Get the connection of the database in memory mode
    public static Connection getConnectionFromDataSource() throws Exception
    {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL(database);
        return dataSource.getConnection(userDB,passwordDB);
    }


}
