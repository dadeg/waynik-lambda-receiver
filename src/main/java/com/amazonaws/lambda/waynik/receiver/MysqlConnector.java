package com.amazonaws.lambda.waynik.receiver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlConnector {
	private static final String url = "jdbc:mysql://se";
	private static final String userName = "web";
	private static final String password = "7";

	public static Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection(url, userName, password);
	}
}
