package com.thevoiceasia.security;

import java.math.BigInteger;
import java.security.SecureRandom;

public class SimplePasswordGenerator {

	/**
	 * Helper Class, produces a SecureRandom long alphanumeric string
	 */
	public static void main(String[] args){
		
		SecureRandom random = new SecureRandom();
		System.out.println(new BigInteger(130, random).toString(32));
		
	}

}
