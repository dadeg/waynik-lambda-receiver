package com.amazonaws.lambda.waynik.receiver;

public class User {
	private int id;
	private String email;
	private String name;
	private boolean hasCheckins;
	
	public User (int id, String email, String name, boolean hasCheckins) 
	{
		this.id = id;
		this.email = email;
		this.name = name;
		this.hasCheckins = hasCheckins;
	}
	
	public int getId() 
	{
		return id;
	}
	
	public String getEmail() 
	{
		return email;
	}
	
	public String getName() 
	{
		return name;
	}
	
	public boolean hasCheckins() 
	{
		return hasCheckins;
	}
}
