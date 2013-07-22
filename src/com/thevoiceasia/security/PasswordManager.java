package com.thevoiceasia.security;

/**
 * Simple class to 'secure' salt+hash a password using BCrypt
 * Provided by Damien Miller <djm@mindrot.org>
 * 
 * Having found this implementation it seems kind of silly to wrap it
 * in a class but it keeps me sane using my own terminologies.
 * 
 * Uses 13 Rounds of BCrypt which takes approx 1000ms to hash a password
 * Use this class from its main method and it will benchmark 10 passwords
 * 
 * According to BCrypt docs, you want as many rounds as you can afford to
 * wait for, however they also say 8ms is a bare minimum but that sounds 
 * a lot per second to me.
 * 
 * Valid range for rounds is 4 to 31 - be aware that 31 seems to complete
 * ridiculously quickly so its a probably bug that high up (30 is working
 * fine and takes a calculated 36.4 Hours to hash).
 * 
 * These numbers are all single threaded, so 1 second per hash is still
 * the same on a quad core but you can have 4 threads so 4 hashes per second
 * (not the same as 1 hash per .25 seconds).
 * 
 * @author Wayne Merricks
 *
 */
public class PasswordManager {

	private static final int BCRYPT_ROUNDS = 13; 
	
	/**
	 * Uses bcrypt to check a hash against the given password
	 * @param hashedPassword
	 * @param password
	 * @return true if matches
	 */
	public static boolean passwordMatches(String hashedPassword, String password){
		
		return BCrypt.checkpw(password, hashedPassword);
		
	}
	
	/**
	 * Uses bcrypt to hash a password with the default number of rounds
	 * from BCRYPT_ROUNDS (13)
	 * @param password
	 * @return hashed + uniquely salted password
	 */
	public static String hashPassword(String password){
		
		return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
		
	}
	
	/**
	 * Uses bcrypt to hash a password with the given number of rounds
	 * Not recommended to use less than 10 rounds.
	 * @param password
	 * @param rounds number of rounds to hash
	 * @return hashed + uniquely salted password
	 */
	public static String hashPassword(String password, int rounds){
		
		return BCrypt.hashpw(password, BCrypt.gensalt(rounds));
		
	}
	
	public static void main(String[] args){
	
		if(args.length == 2){
			
			long start = System.currentTimeMillis();
			
			for(int i = 0; i < 10; i++)
				System.out.println(hashPassword(args[0], Integer.parseInt(args[1])));
			
			System.out.println("Completed in: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if(args.length == 1){
			
			long start = System.currentTimeMillis();
			
			for(int i = 0; i < 10; i++)
				System.out.println(hashPassword(args[0]));
			
			System.out.println("Completed in: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else			
			System.out.println("USAGE: password [rounds (optional)]"); //$NON-NLS-1$
		
	}
	
}
