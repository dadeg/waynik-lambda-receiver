package com.amazonaws.lambda.waynik.receiver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.dbutils.DbUtils;
import org.json.simple.JSONObject;

import java.sql.PreparedStatement;

public class CheckinModel {
	
	private String table = "checkins";

    private static String ID = "id";
    private static String USER_ID = "user_id";
    private static String LATITUDE = "latitude";
    private static String LONGITUDE = "longitude";
    private static String MESSAGE = "message";
    private static String BATTERY = "battery";
    private static String SPEED = "speed";
    private static String BEARING = "bearing";
    private static String ALTITUDE = "altitude";
    private static String EMERGENCY = "emergency";
    private static String COUNTRY = "country";

    private String[] fields = {
    	ID,
        USER_ID,
        LATITUDE,
        LONGITUDE,
        MESSAGE,
        BATTERY,
        SPEED,
        BEARING,
        ALTITUDE,
        EMERGENCY,
    	COUNTRY
    };
	
	public boolean wasLastCheckinEmergencyForUser(int userId) throws Exception
    {	
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = MysqlConnector.getConnection();
			
			statement = connection.prepareStatement("select * from checkins where user_id = ? order by created_at desc limit 1;");
			statement.setInt(1, userId);
			resultSet = statement.executeQuery();
			
	        if (resultSet.next()) {
				boolean emergency = resultSet.getBoolean("emergency");
				
				if (emergency) {
					return true;
				}
	        }
	        
	        return false;
		} catch (Exception e) {
			throw e;
		} finally {
			DbUtils.closeQuietly(resultSet);
			DbUtils.closeQuietly(statement);
			DbUtils.closeQuietly(connection);
		}
    }
	
	public int create(JSONObject data) throws Exception
	{
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultingKey = null;
		try {
			validate(data);
		
	        data = filter(data);
	
	        ArrayList<String> params = new ArrayList<String>();
	        String fieldsString = "";
	        String questionMarks = "";
	        String comma = "";
	
	        for (String field : fields) {
	            if (data.containsKey(field)) {
	                params.add((String)data.get(field));
	                fieldsString += comma + "`" + field + "`";
	                questionMarks += comma + "?";
	                comma = ",";
	            }
	        }
	
	        String sql = "INSERT INTO `" + table + "` (" + fieldsString + ") VALUES (" + questionMarks + ")";
	
	        connection = MysqlConnector.getConnection();
	        statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			int i = 1;
	        for (String param : params) {
				statement.setString(i, param);
				i++;
			}
	        int rowsAffected = statement.executeUpdate();
	        
	        if (rowsAffected == 0) {
	        	throw new SQLException("Creating checkin failed");
	        }
	        
	        resultingKey = statement.getGeneratedKeys();
	        int createdId = 0;
	        if (resultingKey.next())
	        {
	            createdId = resultingKey.getInt(1);
	        }
	        return createdId;
		} catch (Exception e) {
			throw e;
		} finally {
			DbUtils.closeQuietly(resultingKey);
			DbUtils.closeQuietly(statement);
			DbUtils.closeQuietly(connection);
		}
	}
	
	private void validate(JSONObject data) throws Exception 
	{
		if (!data.containsKey(USER_ID) || data.get(USER_ID) == null) {
            throw new Exception(USER_ID + " is required.");
        }
	}
	
	private JSONObject filter(JSONObject data) throws Exception 
	{
        if (data.containsKey(EMERGENCY)) {
        	String emergency = "0";
        	if (Utils.convertToBoolean((String)data.get(EMERGENCY))) {
        		emergency = "1";
        	}
        	data.put(EMERGENCY, emergency);
        }

        return data;
	}
	
	public Checkin getMostRecentForUser(User user) throws Exception
	{
		ResultSet resultSet = null;
		PreparedStatement statement = null;
		Connection connection = null;
		
		try {
			connection = MysqlConnector.getConnection();
		
			statement = connection.prepareStatement("select * from checkins where user_id = ? order by created_at desc limit 1;");
			statement.setInt(1, user.getId());
			resultSet = statement.executeQuery();
			
	        if (!resultSet.next()) {
	        	throw new Exception("no checkins found for most recent checkin");
	        }
	        
			Checkin checkin = new Checkin(
				resultSet.getInt(ID),
				resultSet.getInt(USER_ID),
				resultSet.getDouble(LATITUDE),
				resultSet.getDouble(LONGITUDE),
				resultSet.getString(MESSAGE),
				resultSet.getBoolean(EMERGENCY),
				resultSet.getString(COUNTRY)
			);
			
			return checkin;
		} catch (Exception e) {
			throw e;
		} finally {
			DbUtils.closeQuietly(resultSet);
			DbUtils.closeQuietly(statement);
			DbUtils.closeQuietly(connection);
		}
	}
	
	public Checkin getSecondMostRecentForUser(User user) throws Exception
	{
		ResultSet resultSet = null;
		PreparedStatement statement = null;
		Connection connection = null;
		try {
			connection = MysqlConnector.getConnection();
			
			statement = connection.prepareStatement("select * from checkins where user_id = ? order by created_at desc limit 2;");
			statement.setInt(1, user.getId());
			resultSet = statement.executeQuery();
			
			if (!resultSet.next()) {
				throw new Exception("first checkin not found for second most recent checkin");
			}
			
			// 2nd most recent checkin
	        if (!resultSet.next()) {
	        	throw new Exception("second checkin not found for second most recent checkin");
	        }
	        
			Checkin checkin = new Checkin(
				resultSet.getInt(ID),
				resultSet.getInt(USER_ID),
				resultSet.getDouble(LATITUDE),
				resultSet.getDouble(LONGITUDE),
				resultSet.getString(MESSAGE),
				resultSet.getBoolean(EMERGENCY),
				resultSet.getString(COUNTRY)
			);
			
			return checkin;
		} catch (Exception e) {
			throw e;
		} finally {
			DbUtils.closeQuietly(resultSet);
			DbUtils.closeQuietly(statement);
			DbUtils.closeQuietly(connection);
		}
	}
	
}
