package com.amazonaws.lambda.waynik.receiver;

public class Checkin {
	private int id;
	private int userId;
	private double latitude;
	private double longitude;
	private String message;
	private boolean emergency;
	private String country;
	
	public Checkin(int id, int userId, double latitude, double longitude, String message, boolean emergency, String country)
	{
		this.id = id;
		this.userId = userId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.message = message;
		this.emergency = emergency;
		this.country = country;
	}
	
	public int getId()
	{
		return id;
	}
	
	public int getUserId()
	{
		return userId;
	}
	
	public double getLatitude()
	{
		return latitude;
	}
	
	public double getLongitude()
	{
		return longitude;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public boolean getEmergency()
	{
		return emergency;
	}
	
	public String getCountry()
	{
		return country;
	}
}
