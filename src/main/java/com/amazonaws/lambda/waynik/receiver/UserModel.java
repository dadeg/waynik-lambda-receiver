package com.amazonaws.lambda.waynik.receiver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;

import java.sql.PreparedStatement;

class UserModel {
	public User getByEmail(String email, String token) throws Exception {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			
			connection = MysqlConnector.getConnection();
			
			statement = connection.prepareStatement("SELECT u.id, u.email, u.name, if(count(*) > 0 and c.created_at is not NULL, 1, 0) as hasCheckins "
	        		+ "FROM users u "
	        		+ "JOIN user_custom_fields ucf ON ucf.user_id = u.id AND ucf.attribute = 'apiToken' AND ucf.value = ? "
	        		+ "LEFT JOIN checkins c ON c.user_id = u.id "
	        		+ "WHERE u.email = ? GROUP BY u.id LIMIT 1");
			statement.setString(1, token);
			statement.setString(2, email);
			resultSet = statement.executeQuery();
			
	        while (resultSet.next()) {
				int userId = resultSet.getInt("id");
				String name = resultSet.getString("name");
				boolean hasCheckins = resultSet.getBoolean("hasCheckins");
				
				User user = new User(userId, email, name, hasCheckins);
				
				return user;
	        }
	        
	        throw new Exception("Invalid user credentials");
		} catch (Exception e) {
			throw e;
		} finally {
			DbUtils.closeQuietly(resultSet);
			DbUtils.closeQuietly(statement);
			DbUtils.closeQuietly(connection);
		}
	}
}
