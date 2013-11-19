package com.thevoiceasia.misc;

import java.util.*;

public class CountryCodes {

	private HashMap<String, Country> countries = new HashMap<String, Country>();
	
	/**
	 * Returns the ISO country code from Country Name
	 * @param name - Country to search for
	 * @return - country code
	 */
	public String getCountryCodeByName(String name){
		String code = null;
		
		int start = 0;
		int end = ALL_COUNTRIES.length - 1;
		boolean found = false;
		//int rounds = 1;
		
		while(!found && start >= 0 && start <= end && end < ALL_COUNTRIES.length && end >= start){
			int index = start + ((end - start) / 2);
			int result = ALL_COUNTRIES[index].getName().compareTo(name);
			
			if(result == 0){
				code = ALL_COUNTRIES[index].getISOCode();
				found = true;
			}else{
				if(result < 0)//Match is in 2nd half
					start = index + 1;
				else//Match is in first half
					end = index - 1;
			}	
			
			//System.out.println("Loop No: " + rounds);
			//rounds++;
		}
		
		return code;
	}
	
	/**
	 * Helper method to get the country that has a given phone number
	 * Country codes are max 4 chars so it will split down to the first 4
	 * and successively check against those until its found or there are 
	 * no chars left.
	 * 
	 * @param phoneNumber number to check
	 * @return null if not found or country name if found
	 */
	public String getCountryNameByPhone(String phoneNumber){
		
		//Remove 0s
		while(phoneNumber.startsWith("0")) //$NON-NLS-1$
			phoneNumber = phoneNumber.substring(1);
		
		phoneNumber = phoneNumber.substring(0, 4);
		boolean found = false;
		String name = null;
		
		while(!found && phoneNumber.length() > 0){
			
			if(countries.get(phoneNumber) != null){
				
				found = true;
				name = countries.get(phoneNumber).getName();
				
			}else
				phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);
			
		}
		
		return name;
		
	}
	
	/**
	 * Returns the ISO country code from the phone number.
	 * @param phoneCode - code to search
	 * @return - country code
	 */
	public String getCountryCode(String phoneNumber){
		
		String code = null;
		
		Set<String> keys = countries.keySet();
		Iterator<String> it = keys.iterator();
		
		while(it.hasNext() && code == null){
			
			String key = it.next();
			
			if(!key.equals("") && phoneNumber.startsWith(key)) //$NON-NLS-1$
				code = countries.get(key).getISOCode();
			
		}
		
		return code;
		
	}
	
	public CountryCodes(){
	
		countries.put(AFGHANISTAN.getPhoneCode(), AFGHANISTAN);	
		countries.put(ALBANIA.getPhoneCode(), ALBANIA);	
		countries.put(ALGERIA.getPhoneCode(), ALGERIA);	
		countries.put(AMERICAN_SAMOA.getPhoneCode(), AMERICAN_SAMOA);	
		countries.put(ANDORRA.getPhoneCode(), ANDORRA);	
		countries.put(ANGOLA.getPhoneCode(), ANGOLA);	
		countries.put(ANGUILLA.getPhoneCode(), ANGUILLA);	
		countries.put(ANTARCTICA.getPhoneCode(), ANTARCTICA);	
		countries.put(ANTIGUA_AND_BARBUDA.getPhoneCode(), ANTIGUA_AND_BARBUDA);	
		countries.put(ARGENTINA.getPhoneCode(), ARGENTINA);	
		countries.put(ARMENIA.getPhoneCode(), ARMENIA);	
		countries.put(ARUBA.getPhoneCode(), ARUBA);	
		countries.put(AUSTRALIA.getPhoneCode(), AUSTRALIA);	
		countries.put(AUSTRIA.getPhoneCode(), AUSTRIA);	
		countries.put(AZERBAIJAN.getPhoneCode(), AZERBAIJAN);	
		countries.put(BAHAMAS.getPhoneCode(), BAHAMAS);	
		countries.put(BAHRAIN.getPhoneCode(), BAHRAIN);	
		countries.put(BANGLADESH.getPhoneCode(), BANGLADESH);	
		countries.put(BARBADOS.getPhoneCode(), BARBADOS);	
		countries.put(BELARUS.getPhoneCode(), BELARUS);	
		countries.put(BELGIUM.getPhoneCode(), BELGIUM);	
		countries.put(BELIZE.getPhoneCode(), BELIZE);	
		countries.put(BENIN.getPhoneCode(), BENIN);	
		countries.put(BERMUDA.getPhoneCode(), BERMUDA);	
		countries.put(BHUTAN.getPhoneCode(), BHUTAN);	
		countries.put(BOLIVIA.getPhoneCode(), BOLIVIA);	
		countries.put(BOSNIA_AND_HERZEGOVINA.getPhoneCode(), BOSNIA_AND_HERZEGOVINA);	
		countries.put(BOTSWANA.getPhoneCode(), BOTSWANA);	
		countries.put(BRAZIL.getPhoneCode(), BRAZIL);	
		countries.put(BRITISH_INDIAN_OCEAN_TERRITORY.getPhoneCode(), BRITISH_INDIAN_OCEAN_TERRITORY);	
		countries.put(BRITISH_VIRGIN_ISLANDS.getPhoneCode(), BRITISH_VIRGIN_ISLANDS);	
		countries.put(BRUNEI.getPhoneCode(), BRUNEI);	
		countries.put(BULGARIA.getPhoneCode(), BULGARIA);	
		countries.put(BURKINA_FASO.getPhoneCode(), BURKINA_FASO);	
		countries.put(BURMA_MYANMAR.getPhoneCode(), BURMA_MYANMAR);	
		countries.put(BURUNDI.getPhoneCode(), BURUNDI);	
		countries.put(CAMBODIA.getPhoneCode(), CAMBODIA);	
		countries.put(CAMEROON.getPhoneCode(), CAMEROON);	
		countries.put(CANADA.getPhoneCode(), CANADA);	
		countries.put(CAPE_VERDE.getPhoneCode(), CAPE_VERDE);	
		countries.put(CAYMAN_ISLANDS.getPhoneCode(), CAYMAN_ISLANDS);	
		countries.put(CENTRAL_AFRICAN_REPUBLIC.getPhoneCode(), CENTRAL_AFRICAN_REPUBLIC);	
		countries.put(CHAD.getPhoneCode(), CHAD);	
		countries.put(CHILE.getPhoneCode(), CHILE);	
		countries.put(CHINA.getPhoneCode(), CHINA);	
		countries.put(CHRISTMAS_ISLAND.getPhoneCode(), CHRISTMAS_ISLAND);	
		countries.put(COCOS_KEELING_ISLANDS.getPhoneCode(), COCOS_KEELING_ISLANDS);	
		countries.put(COLOMBIA.getPhoneCode(), COLOMBIA);	
		countries.put(COMOROS.getPhoneCode(), COMOROS);	
		countries.put(COOK_ISLANDS.getPhoneCode(), COOK_ISLANDS);	
		countries.put(COSTA_RICA.getPhoneCode(), COSTA_RICA);	
		countries.put(CROATIA.getPhoneCode(), CROATIA);	
		countries.put(CUBA.getPhoneCode(), CUBA);	
		countries.put(CYPRUS.getPhoneCode(), CYPRUS);	
		countries.put(CZECH_REPUBLIC.getPhoneCode(), CZECH_REPUBLIC);	
		countries.put(DEMOCRATIC_REPUBLIC_OF_THE_CONGO.getPhoneCode(), DEMOCRATIC_REPUBLIC_OF_THE_CONGO);	
		countries.put(DENMARK.getPhoneCode(), DENMARK);	
		countries.put(DJIBOUTI.getPhoneCode(), DJIBOUTI);	
		countries.put(DOMINICA.getPhoneCode(), DOMINICA);	
		countries.put(DOMINICAN_REPUBLIC.getPhoneCode(), DOMINICAN_REPUBLIC);	
		countries.put(ECUADOR.getPhoneCode(), ECUADOR);	
		countries.put(EGYPT.getPhoneCode(), EGYPT);	
		countries.put(EL_SALVADOR.getPhoneCode(), EL_SALVADOR);	
		countries.put(EQUATORIAL_GUINEA.getPhoneCode(), EQUATORIAL_GUINEA);	
		countries.put(ERITREA.getPhoneCode(), ERITREA);	
		countries.put(ESTONIA.getPhoneCode(), ESTONIA);	
		countries.put(ETHIOPIA.getPhoneCode(), ETHIOPIA);	
		countries.put(FALKLAND_ISLANDS.getPhoneCode(), FALKLAND_ISLANDS);	
		countries.put(FAROE_ISLANDS.getPhoneCode(), FAROE_ISLANDS);	
		countries.put(FIJI.getPhoneCode(), FIJI);	
		countries.put(FINLAND.getPhoneCode(), FINLAND);	
		countries.put(FRANCE.getPhoneCode(), FRANCE);	
		countries.put(FRENCH_POLYNESIA.getPhoneCode(), FRENCH_POLYNESIA);	
		countries.put(GABON.getPhoneCode(), GABON);	
		countries.put(GAMBIA.getPhoneCode(), GAMBIA);	
		countries.put(GAZA_STRIP.getPhoneCode(), GAZA_STRIP);	
		countries.put(GEORGIA.getPhoneCode(), GEORGIA);	
		countries.put(GERMANY.getPhoneCode(), GERMANY);	
		countries.put(GHANA.getPhoneCode(), GHANA);	
		countries.put(GIBRALTAR.getPhoneCode(), GIBRALTAR);	
		countries.put(GREECE.getPhoneCode(), GREECE);	
		countries.put(GREENLAND.getPhoneCode(), GREENLAND);	
		countries.put(GRENADA.getPhoneCode(), GRENADA);	
		countries.put(GUAM.getPhoneCode(), GUAM);	
		countries.put(GUATEMALA.getPhoneCode(), GUATEMALA);	
		countries.put(GUINEA.getPhoneCode(), GUINEA);	
		countries.put(GUINEA_BISSAU.getPhoneCode(), GUINEA_BISSAU);	
		countries.put(GUYANA.getPhoneCode(), GUYANA);	
		countries.put(HAITI.getPhoneCode(), HAITI);	
		countries.put(HOLY_SEE_VATICAN_CITY.getPhoneCode(), HOLY_SEE_VATICAN_CITY);	
		countries.put(HONDURAS.getPhoneCode(), HONDURAS);	
		countries.put(HONG_KONG.getPhoneCode(), HONG_KONG);	
		countries.put(HUNGARY.getPhoneCode(), HUNGARY);	
		countries.put(ICELAND.getPhoneCode(), ICELAND);	
		countries.put(INDIA.getPhoneCode(), INDIA);	
		countries.put(INDONESIA.getPhoneCode(), INDONESIA);	
		countries.put(IRAN.getPhoneCode(), IRAN);	
		countries.put(IRAQ.getPhoneCode(), IRAQ);	
		countries.put(IRELAND.getPhoneCode(), IRELAND);	
		countries.put(ISLE_OF_MAN.getPhoneCode(), ISLE_OF_MAN);	
		countries.put(ISRAEL.getPhoneCode(), ISRAEL);	
		countries.put(ITALY.getPhoneCode(), ITALY);	
		countries.put(IVORY_COAST.getPhoneCode(), IVORY_COAST);	
		countries.put(JAMAICA.getPhoneCode(), JAMAICA);	
		countries.put(JAPAN.getPhoneCode(), JAPAN);	
		countries.put(JERSEY.getPhoneCode(), JERSEY);	
		countries.put(JORDAN.getPhoneCode(), JORDAN);	
		countries.put(KAZAKHSTAN.getPhoneCode(), KAZAKHSTAN);	
		countries.put(KENYA.getPhoneCode(), KENYA);	
		countries.put(KIRIBATI.getPhoneCode(), KIRIBATI);	
		countries.put(KOSOVO.getPhoneCode(), KOSOVO);	
		countries.put(KUWAIT.getPhoneCode(), KUWAIT);	
		countries.put(KYRGYZSTAN.getPhoneCode(), KYRGYZSTAN);	
		countries.put(LAOS.getPhoneCode(), LAOS);	
		countries.put(LATVIA.getPhoneCode(), LATVIA);	
		countries.put(LEBANON.getPhoneCode(), LEBANON);	
		countries.put(LESOTHO.getPhoneCode(), LESOTHO);	
		countries.put(LIBERIA.getPhoneCode(), LIBERIA);	
		countries.put(LIBYA.getPhoneCode(), LIBYA);	
		countries.put(LIECHTENSTEIN.getPhoneCode(), LIECHTENSTEIN);	
		countries.put(LITHUANIA.getPhoneCode(), LITHUANIA);	
		countries.put(LUXEMBOURG.getPhoneCode(), LUXEMBOURG);	
		countries.put(MACAU.getPhoneCode(), MACAU);	
		countries.put(MACEDONIA.getPhoneCode(), MACEDONIA);	
		countries.put(MADAGASCAR.getPhoneCode(), MADAGASCAR);	
		countries.put(MALAWI.getPhoneCode(), MALAWI);	
		countries.put(MALAYSIA.getPhoneCode(), MALAYSIA);	
		countries.put(MALDIVES.getPhoneCode(), MALDIVES);	
		countries.put(MALI.getPhoneCode(), MALI);	
		countries.put(MALTA.getPhoneCode(), MALTA);	
		countries.put(MARSHALL_ISLANDS.getPhoneCode(), MARSHALL_ISLANDS);	
		countries.put(MAURITANIA.getPhoneCode(), MAURITANIA);	
		countries.put(MAURITIUS.getPhoneCode(), MAURITIUS);	
		countries.put(MAYOTTE.getPhoneCode(), MAYOTTE);	
		countries.put(MEXICO.getPhoneCode(), MEXICO);	
		countries.put(MICRONESIA.getPhoneCode(), MICRONESIA);	
		countries.put(MOLDOVA.getPhoneCode(), MOLDOVA);	
		countries.put(MONACO.getPhoneCode(), MONACO);	
		countries.put(MONGOLIA.getPhoneCode(), MONGOLIA);	
		countries.put(MONTENEGRO.getPhoneCode(), MONTENEGRO);	
		countries.put(MONTSERRAT.getPhoneCode(), MONTSERRAT);	
		countries.put(MOROCCO.getPhoneCode(), MOROCCO);	
		countries.put(MOZAMBIQUE.getPhoneCode(), MOZAMBIQUE);	
		countries.put(NAMIBIA.getPhoneCode(), NAMIBIA);	
		countries.put(NAURU.getPhoneCode(), NAURU);	
		countries.put(NEPAL.getPhoneCode(), NEPAL);	
		countries.put(NETHERLANDS.getPhoneCode(), NETHERLANDS);	
		countries.put(NETHERLANDS_ANTILLES.getPhoneCode(), NETHERLANDS_ANTILLES);	
		countries.put(NEW_CALEDONIA.getPhoneCode(), NEW_CALEDONIA);	
		countries.put(NEW_ZEALAND.getPhoneCode(), NEW_ZEALAND);	
		countries.put(NICARAGUA.getPhoneCode(), NICARAGUA);	
		countries.put(NIGER.getPhoneCode(), NIGER);	
		countries.put(NIGERIA.getPhoneCode(), NIGERIA);	
		countries.put(NIUE.getPhoneCode(), NIUE);	
		countries.put(NORFOLK_ISLAND.getPhoneCode(), NORFOLK_ISLAND);	
		countries.put(NORTH_KOREA.getPhoneCode(), NORTH_KOREA);	
		countries.put(NORTHERN_MARIANA_ISLANDS.getPhoneCode(), NORTHERN_MARIANA_ISLANDS);	
		countries.put(NORWAY.getPhoneCode(), NORWAY);	
		countries.put(OMAN.getPhoneCode(), OMAN);	
		countries.put(PAKISTAN.getPhoneCode(), PAKISTAN);	
		countries.put(PALAU.getPhoneCode(), PALAU);	
		countries.put(PANAMA.getPhoneCode(), PANAMA);	
		countries.put(PAPUA_NEW_GUINEA.getPhoneCode(), PAPUA_NEW_GUINEA);	
		countries.put(PARAGUAY.getPhoneCode(), PARAGUAY);	
		countries.put(PERU.getPhoneCode(), PERU);	
		countries.put(PHILLIPINES.getPhoneCode(), PHILLIPINES);	
		countries.put(PITCAIRN_ISLANDS.getPhoneCode(), PITCAIRN_ISLANDS);	
		countries.put(POLAND.getPhoneCode(), POLAND);	
		countries.put(PORTUGAL.getPhoneCode(), PORTUGAL);	
		countries.put(PUERTO_RICO.getPhoneCode(), PUERTO_RICO);	
		countries.put(QATAR.getPhoneCode(), QATAR);	
		countries.put(REPUBLIC_OF_THE_CONGO.getPhoneCode(), REPUBLIC_OF_THE_CONGO);	
		countries.put(ROMANIA.getPhoneCode(), ROMANIA);	
		countries.put(RUSSIA.getPhoneCode(), RUSSIA);	
		countries.put(RWANDA.getPhoneCode(), RWANDA);	
		countries.put(SAINT_BARTHELEMY.getPhoneCode(), SAINT_BARTHELEMY);	
		countries.put(SAINT_HELENA.getPhoneCode(), SAINT_HELENA);	
		countries.put(SAINT_KITTS_AND_NEVIS.getPhoneCode(), SAINT_KITTS_AND_NEVIS);	
		countries.put(SAINT_LUCIA.getPhoneCode(), SAINT_LUCIA);	
		countries.put(SAINT_MARTIN.getPhoneCode(), SAINT_MARTIN);	
		countries.put(SAINT_PIERRE_AND_MIQUELON.getPhoneCode(), SAINT_PIERRE_AND_MIQUELON);	
		countries.put(SAINT_VINCENT_AND_THE_GRENADINES.getPhoneCode(), SAINT_VINCENT_AND_THE_GRENADINES);	
		countries.put(SAMOA.getPhoneCode(), SAMOA);	
		countries.put(SAN_MARINO.getPhoneCode(), SAN_MARINO);	
		countries.put(SAO_TOME_AND_PRINCIPE.getPhoneCode(), SAO_TOME_AND_PRINCIPE);	
		countries.put(SAUDI_ARABIA.getPhoneCode(), SAUDI_ARABIA);	
		countries.put(SENEGAL.getPhoneCode(), SENEGAL);	
		countries.put(SERBIA.getPhoneCode(), SERBIA);	
		countries.put(SEYCHELLES.getPhoneCode(), SEYCHELLES);	
		countries.put(SIERRA_LEONE.getPhoneCode(), SIERRA_LEONE);	
		countries.put(SINGAPORE.getPhoneCode(), SINGAPORE);	
		countries.put(SLOVAKIA.getPhoneCode(), SLOVAKIA);	
		countries.put(SLOVENIA.getPhoneCode(), SLOVENIA);	
		countries.put(SOLOMON_ISLANDS.getPhoneCode(), SOLOMON_ISLANDS);	
		countries.put(SOMALIA.getPhoneCode(), SOMALIA);	
		countries.put(SOUTH_AFRICA.getPhoneCode(), SOUTH_AFRICA);	
		countries.put(SOUTH_KOREA.getPhoneCode(), SOUTH_KOREA);	
		countries.put(SPAIN.getPhoneCode(), SPAIN);	
		countries.put(SRI_LANKA.getPhoneCode(), SRI_LANKA);	
		countries.put(SUDAN.getPhoneCode(), SUDAN);	
		countries.put(SURINAME.getPhoneCode(), SURINAME);	
		countries.put(SVALBARD.getPhoneCode(), SVALBARD);	
		countries.put(SWAZILAND.getPhoneCode(), SWAZILAND);	
		countries.put(SWEDEN.getPhoneCode(), SWEDEN);	
		countries.put(SWITZERLAND.getPhoneCode(), SWITZERLAND);	
		countries.put(SYRIA.getPhoneCode(), SYRIA);	
		countries.put(TAIWAN.getPhoneCode(), TAIWAN);	
		countries.put(TAJIKISTAN.getPhoneCode(), TAJIKISTAN);	
		countries.put(TANZANIA.getPhoneCode(), TANZANIA);	
		countries.put(THAILAND.getPhoneCode(), THAILAND);	
		countries.put(TIMOR_LESTE.getPhoneCode(), TIMOR_LESTE);	
		countries.put(TOGO.getPhoneCode(), TOGO);	
		countries.put(TOKELAU.getPhoneCode(), TOKELAU);	
		countries.put(TONGA.getPhoneCode(), TONGA);	
		countries.put(TRINIDAD_AND_TOBAGO.getPhoneCode(), TRINIDAD_AND_TOBAGO);	
		countries.put(TUNISIA.getPhoneCode(), TUNISIA);	
		countries.put(TURKEY.getPhoneCode(), TURKEY);	
		countries.put(TURKMENISTAN.getPhoneCode(), TURKMENISTAN);	
		countries.put(TURKS_AND_CAICOS_ISLANDS.getPhoneCode(), TURKS_AND_CAICOS_ISLANDS);	
		countries.put(TUVALU.getPhoneCode(), TUVALU);	
		countries.put(UGANDA.getPhoneCode(), UGANDA);	
		countries.put(UKRAINE.getPhoneCode(), UKRAINE);	
		countries.put(UNITED_ARAB_EMIRATES.getPhoneCode(), UNITED_ARAB_EMIRATES);	
		countries.put(UNITED_KINGDOM.getPhoneCode(), UNITED_KINGDOM);	
		countries.put(UNITED_STATES.getPhoneCode(), UNITED_STATES);	
		countries.put(URUGUAY.getPhoneCode(), URUGUAY);	
		countries.put(US_VIRGIN_ISLANDS.getPhoneCode(), US_VIRGIN_ISLANDS);	
		countries.put(UZBEKISTAN.getPhoneCode(), UZBEKISTAN);	
		countries.put(VANUATU.getPhoneCode(), VANUATU);	
		countries.put(VENEZUELA.getPhoneCode(), VENEZUELA);	
		countries.put(VIETNAM.getPhoneCode(), VIETNAM);	
		countries.put(WALLIS_AND_FUTUNA.getPhoneCode(), WALLIS_AND_FUTUNA);	
		countries.put(WEST_BANK.getPhoneCode(), WEST_BANK);	
		countries.put(WESTERN_SAHARA.getPhoneCode(), WESTERN_SAHARA);	
		countries.put(YEMEN.getPhoneCode(), YEMEN);	
		countries.put(ZAMBIA.getPhoneCode(), ZAMBIA);	
		countries.put(ZIMBABWE.getPhoneCode(), ZIMBABWE);
		
	}
	
	/* STATICS */
	
	@SuppressWarnings("nls")
	private static final Country AFGHANISTAN = new Country("Afghanistan", "AF", "93");
	@SuppressWarnings("nls")
	private static final Country ALBANIA = new Country("Albania", "AL", "355");
	@SuppressWarnings("nls")
	private static final Country ALGERIA = new Country("Algeria", "DZ", "213");
	@SuppressWarnings("nls")
	private static final Country AMERICAN_SAMOA = new Country("American Samoa", "AS", "1684");
	@SuppressWarnings("nls")
	private static final Country ANDORRA = new Country("Andorra", "AD", "376");
	@SuppressWarnings("nls")
	private static final Country ANGOLA = new Country("Angola", "AO", "244");
	@SuppressWarnings("nls")
	private static final Country ANGUILLA = new Country("Anguilla", "AI", "1264");
	@SuppressWarnings("nls")
	private static final Country ANTARCTICA = new Country("Antarctica", "AQ", "672");
	@SuppressWarnings("nls")
	private static final Country ANTIGUA_AND_BARBUDA = new Country("Antigua and Barbuda", "AG", "1268");
	@SuppressWarnings("nls")
	private static final Country ARGENTINA = new Country("Argentina", "AR", "54");
	@SuppressWarnings("nls")
	private static final Country ARMENIA = new Country("Armenia", "AM", "374");
	@SuppressWarnings("nls")
	private static final Country ARUBA = new Country("Aruba", "AW", "297");
	@SuppressWarnings("nls")
	private static final Country AUSTRALIA = new Country("Australia", "AU", "61");
	@SuppressWarnings("nls")
	private static final Country AUSTRIA = new Country("Austria", "AT", "43");
	@SuppressWarnings("nls")
	private static final Country AZERBAIJAN = new Country("Azerbaijan", "AZ", "994");
	@SuppressWarnings("nls")
	private static final Country BAHAMAS = new Country("Bahamas", "BS", "1242");
	@SuppressWarnings("nls")
	private static final Country BAHRAIN = new Country("Bahrain", "BH", "973");
	@SuppressWarnings("nls")
	private static final Country BANGLADESH = new Country("Bangladesh", "BD", "880");
	@SuppressWarnings("nls")
	private static final Country BARBADOS = new Country("Barbados", "BB", "1246");
	@SuppressWarnings("nls")
	private static final Country BELARUS = new Country("Belarus", "BY", "375");
	@SuppressWarnings("nls")
	private static final Country BELGIUM = new Country("Belgium", "BE", "32");
	@SuppressWarnings("nls")
	private static final Country BELIZE = new Country("Belize", "BZ", "501");
	@SuppressWarnings("nls")
	private static final Country BENIN = new Country("Benin", "BJ", "229");
	@SuppressWarnings("nls")
	private static final Country BERMUDA = new Country("Bermuda", "BM", "1441");
	@SuppressWarnings("nls")
	private static final Country BHUTAN = new Country("Bhutan", "BT", "975");
	@SuppressWarnings("nls")
	private static final Country BOLIVIA = new Country("Bolivia", "BO", "591");
	@SuppressWarnings("nls")
	private static final Country BOSNIA_AND_HERZEGOVINA = new Country("Bosnia and Herzegovina", "BA", "387");
	@SuppressWarnings("nls")
	private static final Country BOTSWANA = new Country("Botswana", "BW", "267");
	@SuppressWarnings("nls")
	private static final Country BRAZIL = new Country("Brazil", "BR", "55");
	@SuppressWarnings("nls")
	private static final Country BRITISH_INDIAN_OCEAN_TERRITORY = new Country("British Indian Ocean Territory", "IO", "");
	@SuppressWarnings("nls")
	private static final Country BRITISH_VIRGIN_ISLANDS = new Country("British Virgin Islands", "VG", "1284");
	@SuppressWarnings("nls")
	private static final Country BRUNEI = new Country("Brunei", "BN", "673");
	@SuppressWarnings("nls")
	private static final Country BULGARIA = new Country("Bulgaria", "BG", "359");
	@SuppressWarnings("nls")
	private static final Country BURKINA_FASO = new Country("Burkina Faso", "BF", "226");
	@SuppressWarnings("nls")
	private static final Country BURMA_MYANMAR = new Country("Burma (Myanmar)", "MM", "95");
	@SuppressWarnings("nls")
	private static final Country BURUNDI = new Country("Burundi", "BI", "257");
	@SuppressWarnings("nls")
	private static final Country CAMBODIA = new Country("Cambodia", "KH", "855");
	@SuppressWarnings("nls")
	private static final Country CAMEROON = new Country("Cameroon", "CM", "237");
	@SuppressWarnings("nls")
	private static final Country CANADA = new Country("Canada", "CA", "1");
	@SuppressWarnings("nls")
	private static final Country CAPE_VERDE = new Country("Cape Verde", "CV", "238");
	@SuppressWarnings("nls")
	private static final Country CAYMAN_ISLANDS = new Country("Cayman Islands", "KY", "1345");
	@SuppressWarnings("nls")
	private static final Country CENTRAL_AFRICAN_REPUBLIC = new Country("Central African Republic", "CF", "236");
	@SuppressWarnings("nls")
	private static final Country CHAD = new Country("Chad", "TD", "235");
	@SuppressWarnings("nls")
	private static final Country CHILE = new Country("Chile", "CL", "56");
	@SuppressWarnings("nls")
	private static final Country CHINA = new Country("China", "CN", "86");
	@SuppressWarnings("nls")
	private static final Country CHRISTMAS_ISLAND = new Country("Christmas Island", "CX", "61");
	@SuppressWarnings("nls")
	private static final Country COCOS_KEELING_ISLANDS = new Country("Cocos (Keeling) Islands", "CC", "61");
	@SuppressWarnings("nls")
	private static final Country COLOMBIA = new Country("Colombia", "CO", "57");
	@SuppressWarnings("nls")
	private static final Country COMOROS = new Country("Comoros", "KM", "269");
	@SuppressWarnings("nls")
	private static final Country COOK_ISLANDS = new Country("Cook Islands", "CK", "682");
	@SuppressWarnings("nls")
	private static final Country COSTA_RICA = new Country("Costa Rica", "CR", "506");
	@SuppressWarnings("nls")
	private static final Country CROATIA = new Country("Croatia", "HR", "385");
	@SuppressWarnings("nls")
	private static final Country CUBA = new Country("Cuba", "CU", "53");
	@SuppressWarnings("nls")
	private static final Country CYPRUS = new Country("Cyprus", "CY", "CY");
	@SuppressWarnings("nls")
	private static final Country CZECH_REPUBLIC = new Country("Czech Republic", "CZ", "420");
	@SuppressWarnings("nls")
	private static final Country DEMOCRATIC_REPUBLIC_OF_THE_CONGO = new Country("Democratic Republic of The Congo", "CD", "243");
	@SuppressWarnings("nls")
	private static final Country DENMARK = new Country("Denmark", "DK", "45");
	@SuppressWarnings("nls")
	private static final Country DJIBOUTI = new Country("Djibouti", "DJ", "253");
	@SuppressWarnings("nls")
	private static final Country DOMINICA = new Country("Dominica", "DM", "1767");
	@SuppressWarnings("nls")
	private static final Country DOMINICAN_REPUBLIC = new Country("Dominican Republic", "DO", "1809");
	@SuppressWarnings("nls")
	private static final Country ECUADOR = new Country("Ecuador", "EC", "593");
	@SuppressWarnings("nls")
	private static final Country EGYPT = new Country("Egypt", "EG", "20");
	@SuppressWarnings("nls")
	private static final Country EL_SALVADOR = new Country("El Salvador", "SV", "503");
	@SuppressWarnings("nls")
	private static final Country EQUATORIAL_GUINEA = new Country("Equatorial Guinea", "GQ", "240");
	@SuppressWarnings("nls")
	private static final Country ERITREA = new Country("Eritrea", "ER", "291");
	@SuppressWarnings("nls")
	private static final Country ESTONIA = new Country("Estonia", "EE", "372");
	@SuppressWarnings("nls")
	private static final Country ETHIOPIA = new Country("Ethiopia", "ET", "251");
	@SuppressWarnings("nls")
	private static final Country FALKLAND_ISLANDS = new Country("Falkland Islands", "FK", "500");
	@SuppressWarnings("nls")
	private static final Country FAROE_ISLANDS = new Country("Faroe Islands", "FO", "298");
	@SuppressWarnings("nls")
	private static final Country FIJI = new Country("Fiji", "FJ", "679");
	@SuppressWarnings("nls")
	private static final Country FINLAND = new Country("Finland", "FI", "358");
	@SuppressWarnings("nls")
	private static final Country FRANCE = new Country("France", "FR", "33");
	@SuppressWarnings("nls")
	private static final Country FRENCH_POLYNESIA = new Country("French Polynesia", "PF", "689");
	@SuppressWarnings("nls")
	private static final Country GABON = new Country("Gabon", "GA", "241");
	@SuppressWarnings("nls")
	private static final Country GAMBIA = new Country("Gambia", "GM", "220");
	@SuppressWarnings("nls")
	private static final Country GAZA_STRIP = new Country("Gaza Strip", "", "970");
	@SuppressWarnings("nls")
	private static final Country GEORGIA = new Country("Georgia", "GE", "995");
	@SuppressWarnings("nls")
	private static final Country GERMANY = new Country("Germany", "DE", "49");
	@SuppressWarnings("nls")
	private static final Country GHANA = new Country("Ghana", "GH", "233");
	@SuppressWarnings("nls")
	private static final Country GIBRALTAR = new Country("Gibraltar", "GI", "350");
	@SuppressWarnings("nls")
	private static final Country GREECE = new Country("Greece", "GR", "30");
	@SuppressWarnings("nls")
	private static final Country GREENLAND = new Country("Greenland", "GL", "299");
	@SuppressWarnings("nls")
	private static final Country GRENADA = new Country("Grenada", "GD", "1473");
	@SuppressWarnings("nls")
	private static final Country GUAM = new Country("Guam", "GU", "1671");
	@SuppressWarnings("nls")
	private static final Country GUATEMALA = new Country("Guatemala", "GT", "502");
	@SuppressWarnings("nls")
	private static final Country GUINEA = new Country("Guinea", "GN", "224");
	@SuppressWarnings("nls")
	private static final Country GUINEA_BISSAU = new Country("Guinea-Bissau", "GW", "245");
	@SuppressWarnings("nls")
	private static final Country GUYANA = new Country("Guyana", "GY", "592");
	@SuppressWarnings("nls")
	private static final Country HAITI = new Country("Haiti", "HT", "509");
	@SuppressWarnings("nls")
	private static final Country HOLY_SEE_VATICAN_CITY = new Country("Holy See (Vatican City)", "VA", "39");
	@SuppressWarnings("nls")
	private static final Country HONDURAS = new Country("Honduras", "HN", "504");
	@SuppressWarnings("nls")
	private static final Country HONG_KONG = new Country("Hong Kong", "HK", "852");
	@SuppressWarnings("nls")
	private static final Country HUNGARY = new Country("Hungary", "HU", "36");
	@SuppressWarnings("nls")
	private static final Country ICELAND = new Country("Iceland", "IS", "354");
	@SuppressWarnings("nls")
	private static final Country INDIA = new Country("India", "IN", "91");
	@SuppressWarnings("nls")
	private static final Country INDONESIA = new Country("Indonesia", "ID", "62");
	@SuppressWarnings("nls")
	private static final Country IRAN = new Country("Iran", "IR", "98");
	@SuppressWarnings("nls")
	private static final Country IRAQ = new Country("Iraq", "IQ", "964");
	@SuppressWarnings("nls")
	private static final Country IRELAND = new Country("Ireland", "IE", "353");
	@SuppressWarnings("nls")
	private static final Country ISLE_OF_MAN = new Country("Isle of Man", "IM", "44");
	@SuppressWarnings("nls")
	private static final Country ISRAEL = new Country("Israel", "IL", "972");
	@SuppressWarnings("nls")
	private static final Country ITALY = new Country("Italy", "IT", "39");
	@SuppressWarnings("nls")
	private static final Country IVORY_COAST = new Country("Ivory Coast", "CI", "225");
	@SuppressWarnings("nls")
	private static final Country JAMAICA = new Country("Jamaica", "JM", "1876");
	@SuppressWarnings("nls")
	private static final Country JAPAN = new Country("Japan", "JP", "81");
	@SuppressWarnings("nls")
	private static final Country JERSEY = new Country("Jersey", "JE", "44");
	@SuppressWarnings("nls")
	private static final Country JORDAN = new Country("Jordan", "JO", "962");
	@SuppressWarnings("nls")
	private static final Country KAZAKHSTAN = new Country("Kazakhstan", "KZ", "7");
	@SuppressWarnings("nls")
	private static final Country KENYA = new Country("Kenya", "KE", "254");
	@SuppressWarnings("nls")
	private static final Country KIRIBATI = new Country("Kiribati", "KI", "686");
	@SuppressWarnings("nls")
	private static final Country KOSOVO = new Country("Kosovo", "", "381");
	@SuppressWarnings("nls")
	private static final Country KUWAIT = new Country("Kuwait", "KW", "965");
	@SuppressWarnings("nls")
	private static final Country KYRGYZSTAN = new Country("Kyrgyzstan", "KG", "996");
	@SuppressWarnings("nls")
	private static final Country LAOS = new Country("Laos", "LA", "856");
	@SuppressWarnings("nls")
	private static final Country LATVIA = new Country("Latvia", "LV", "371");
	@SuppressWarnings("nls")
	private static final Country LEBANON = new Country("Lebanon", "LB", "961");
	@SuppressWarnings("nls")
	private static final Country LESOTHO = new Country("Lesotho", "LS", "266");
	@SuppressWarnings("nls")
	private static final Country LIBERIA = new Country("Liberia", "LR", "231");
	@SuppressWarnings("nls")
	private static final Country LIBYA = new Country("Libya", "LY", "218");
	@SuppressWarnings("nls")
	private static final Country LIECHTENSTEIN = new Country("Liechtenstein", "LI", "423");
	@SuppressWarnings("nls")
	private static final Country LITHUANIA = new Country("Lithuania", "LT", "370");
	@SuppressWarnings("nls")
	private static final Country LUXEMBOURG = new Country("Luxembourg", "LU", "352");
	@SuppressWarnings("nls")
	private static final Country MACAU = new Country("Macau", "MO", "853");
	@SuppressWarnings("nls")
	private static final Country MACEDONIA = new Country("Macedonia", "MK", "389");
	@SuppressWarnings("nls")
	private static final Country MADAGASCAR = new Country("Madagascar", "MG", "261");
	@SuppressWarnings("nls")
	private static final Country MALAWI = new Country("Malawi", "MW", "265");
	@SuppressWarnings("nls")
	private static final Country MALAYSIA = new Country("Malaysia", "MY", "60");
	@SuppressWarnings("nls")
	private static final Country MALDIVES = new Country("Maldives", "MV", "960");
	@SuppressWarnings("nls")
	private static final Country MALI = new Country("Mali", "ML", "223");
	@SuppressWarnings("nls")
	private static final Country MALTA = new Country("Malta", "MT", "356");
	@SuppressWarnings("nls")
	private static final Country MARSHALL_ISLANDS = new Country("Marshall Islands", "MH", "692");
	@SuppressWarnings("nls")
	private static final Country MAURITANIA = new Country("Mauritania", "MR", "222");
	@SuppressWarnings("nls")
	private static final Country MAURITIUS = new Country("Mauritius", "MU", "230");
	@SuppressWarnings("nls")
	private static final Country MAYOTTE = new Country("Mayotte", "YT", "262");
	@SuppressWarnings("nls")
	private static final Country MEXICO = new Country("Mexico", "MX", "52");
	@SuppressWarnings("nls")
	private static final Country MICRONESIA = new Country("Micronesia", "FM", "691");
	@SuppressWarnings("nls")
	private static final Country MOLDOVA = new Country("Moldova", "MD", "373");
	@SuppressWarnings("nls")
	private static final Country MONACO = new Country("Monaco", "MC", "377");
	@SuppressWarnings("nls")
	private static final Country MONGOLIA = new Country("Mongolia", "MN", "976");
	@SuppressWarnings("nls")
	private static final Country MONTENEGRO = new Country("Montenegro", "ME", "382");
	@SuppressWarnings("nls")
	private static final Country MONTSERRAT = new Country("Montserrat", "MS", "1664");
	@SuppressWarnings("nls")
	private static final Country MOROCCO = new Country("Morocco", "MA", "212");
	@SuppressWarnings("nls")
	private static final Country MOZAMBIQUE = new Country("Mozambique", "MZ", "258");
	@SuppressWarnings("nls")
	private static final Country NAMIBIA = new Country("Namibia", "NA", "264");
	@SuppressWarnings("nls")
	private static final Country NAURU = new Country("Nauru", "NR", "674");
	@SuppressWarnings("nls")
	private static final Country NEPAL = new Country("Nepal", "NP", "977");
	@SuppressWarnings("nls")
	private static final Country NETHERLANDS = new Country("Netherlands", "NL", "31");
	@SuppressWarnings("nls")
	private static final Country NETHERLANDS_ANTILLES = new Country("Netherlands Antilles", "AN", "599");
	@SuppressWarnings("nls")
	private static final Country NEW_CALEDONIA = new Country("New Caledonia", "NC", "687");
	@SuppressWarnings("nls")
	private static final Country NEW_ZEALAND = new Country("New Zealand", "NZ", "64");
	@SuppressWarnings("nls")
	private static final Country NICARAGUA = new Country("Nicaragua", "NI", "505");
	@SuppressWarnings("nls")
	private static final Country NIGER = new Country("Niger", "NE", "227");
	@SuppressWarnings("nls")
	private static final Country NIGERIA = new Country("Nigeria", "NG", "234");
	@SuppressWarnings("nls")
	private static final Country NIUE = new Country("Niue", "NU", "683");
	@SuppressWarnings("nls")
	private static final Country NORFOLK_ISLAND = new Country("Norfolk Island", "NF", "672");
	@SuppressWarnings("nls")
	private static final Country NORTH_KOREA = new Country("North Korea", "KP", "850");
	@SuppressWarnings("nls")
	private static final Country NORTHERN_MARIANA_ISLANDS = new Country("Northern Mariana Islands", "MP", "1670");
	@SuppressWarnings("nls")
	private static final Country NORWAY = new Country("Norway", "NO", "47");
	@SuppressWarnings("nls")
	private static final Country OMAN = new Country("Oman", "OM", "968");
	@SuppressWarnings("nls")
	private static final Country PAKISTAN = new Country("Pakistan", "PK", "92");
	@SuppressWarnings("nls")
	private static final Country PALAU = new Country("Palau", "PW", "680");
	@SuppressWarnings("nls")
	private static final Country PANAMA = new Country("Panama", "PA", "507");
	@SuppressWarnings("nls")
	private static final Country PAPUA_NEW_GUINEA = new Country("Papua New Guinea", "PG", "675");
	@SuppressWarnings("nls")
	private static final Country PARAGUAY = new Country("Paraguay", "PY", "595");
	@SuppressWarnings("nls")
	private static final Country PERU = new Country("Peru", "PE", "51");
	@SuppressWarnings("nls")
	private static final Country PHILLIPINES = new Country("Phillipines", "PH", "63");
	@SuppressWarnings("nls")
	private static final Country PITCAIRN_ISLANDS = new Country("Pitcairn Islands", "PN", "870");
	@SuppressWarnings("nls")
	private static final Country POLAND = new Country("Poland", "PL", "48");
	@SuppressWarnings("nls")
	private static final Country PORTUGAL = new Country("Portugal", "PT", "PT");
	@SuppressWarnings("nls")
	private static final Country PUERTO_RICO = new Country("Puerto Rico", "PR", "1");
	@SuppressWarnings("nls")
	private static final Country QATAR = new Country("Qatar", "QA", "974");
	@SuppressWarnings("nls")
	private static final Country REPUBLIC_OF_THE_CONGO = new Country("Republic of the Congo", "CG", "242");
	@SuppressWarnings("nls")
	private static final Country ROMANIA = new Country("Romania", "RO", "40");
	@SuppressWarnings("nls")
	private static final Country RUSSIA = new Country("Russia", "RU", "7");
	@SuppressWarnings("nls")
	private static final Country RWANDA = new Country("Rwanda", "RW", "250");
	@SuppressWarnings("nls")
	private static final Country SAINT_BARTHELEMY = new Country("Saint Barthelemy", "BL", "590");
	@SuppressWarnings("nls")
	private static final Country SAINT_HELENA = new Country("Saint Helena", "SH", "290");
	@SuppressWarnings("nls")
	private static final Country SAINT_KITTS_AND_NEVIS = new Country("Saint Kitts and Nevis", "KN", "1869");
	@SuppressWarnings("nls")
	private static final Country SAINT_LUCIA = new Country("Saint Lucia", "LC", "1758");
	@SuppressWarnings("nls")
	private static final Country SAINT_MARTIN = new Country("Saint Martin", "MF", "1599");
	@SuppressWarnings("nls")
	private static final Country SAINT_PIERRE_AND_MIQUELON = new Country("Saint Pierre and Miquelon", "PM", "508");
	@SuppressWarnings("nls")
	private static final Country SAINT_VINCENT_AND_THE_GRENADINES = new Country("Saint Vincent and the Grenadines", "VC", "1784");
	@SuppressWarnings("nls")
	private static final Country SAMOA = new Country("SAMOA", "WS", "685");
	@SuppressWarnings("nls")
	private static final Country SAN_MARINO = new Country("San Marino", "SM", "378");
	@SuppressWarnings("nls")
	private static final Country SAO_TOME_AND_PRINCIPE = new Country("Sao Tome and Principe", "VC", "1784");
	@SuppressWarnings("nls")
	private static final Country SAUDI_ARABIA = new Country("Saudi Arabia", "SA", "966");
	@SuppressWarnings("nls")
	private static final Country SENEGAL = new Country("Senegal", "SN", "221");
	@SuppressWarnings("nls")
	private static final Country SERBIA = new Country("Serbia", "RS", "381");
	@SuppressWarnings("nls")
	private static final Country SEYCHELLES = new Country("Seychelles", "SC", "248");
	@SuppressWarnings("nls")
	private static final Country SIERRA_LEONE = new Country("Sierra Leone", "SL", "232");
	@SuppressWarnings("nls")
	private static final Country SINGAPORE = new Country("Singapore", "SG", "65");
	@SuppressWarnings("nls")
	private static final Country SLOVAKIA = new Country("Slovakia", "SK", "421");
	@SuppressWarnings("nls")
	private static final Country SLOVENIA = new Country("Slovenia", "SI", "386");
	@SuppressWarnings("nls")
	private static final Country SOLOMON_ISLANDS = new Country("Solomon Islands", "SB", "677");
	@SuppressWarnings("nls")
	private static final Country SOMALIA = new Country("Somalia", "SO", "252");
	@SuppressWarnings("nls")
	private static final Country SOUTH_AFRICA = new Country("South Africa", "ZA", "27");
	@SuppressWarnings("nls")
	private static final Country SOUTH_KOREA = new Country("South Korea", "KR", "82");
	@SuppressWarnings("nls")
	private static final Country SPAIN = new Country("Spain", "ES", "34");
	@SuppressWarnings("nls")
	private static final Country SRI_LANKA = new Country("Sri Lanka", "LK", "94");
	@SuppressWarnings("nls")
	private static final Country SUDAN = new Country("Sudan", "SD", "249");
	@SuppressWarnings("nls")
	private static final Country SURINAME = new Country("Suriname", "SR", "597");
	@SuppressWarnings("nls")
	private static final Country SVALBARD = new Country("Svalbard", "SJ", "");
	@SuppressWarnings("nls")
	private static final Country SWAZILAND = new Country("Swaziland", "SZ", "268");
	@SuppressWarnings("nls")
	private static final Country SWEDEN = new Country("Sweden", "SE", "46");
	@SuppressWarnings("nls")
	private static final Country SWITZERLAND = new Country("Switzerland", "CH", "41");
	@SuppressWarnings("nls")
	private static final Country SYRIA = new Country("Syria", "SY", "963");
	@SuppressWarnings("nls")
	private static final Country TAIWAN = new Country("Taiwan", "TW", "886");
	@SuppressWarnings("nls")
	private static final Country TAJIKISTAN = new Country("Tajikistan", "TJ", "992");
	@SuppressWarnings("nls")
	private static final Country TANZANIA = new Country("Tanzania", "TZ", "255");
	@SuppressWarnings("nls")
	private static final Country THAILAND = new Country("Thailand", "TH", "66");
	@SuppressWarnings("nls")
	private static final Country TIMOR_LESTE = new Country("Timor-Leste", "TL", "670");
	@SuppressWarnings("nls")
	private static final Country TOGO = new Country("Togo", "TG", "228");
	@SuppressWarnings("nls")
	private static final Country TOKELAU = new Country("Tokelau", "TK", "690");
	@SuppressWarnings("nls")
	private static final Country TONGA = new Country("Tonga", "TO", "676");
	@SuppressWarnings("nls")
	private static final Country TRINIDAD_AND_TOBAGO = new Country("Trinidad and Tobago", "TT", "1868");
	@SuppressWarnings("nls")
	private static final Country TUNISIA = new Country("Tunisia", "TN", "216");
	@SuppressWarnings("nls")
	private static final Country TURKEY = new Country("Turkey", "TR", "90");
	@SuppressWarnings("nls")
	private static final Country TURKMENISTAN = new Country("Turkmenistan", "TM", "993");
	@SuppressWarnings("nls")
	private static final Country TURKS_AND_CAICOS_ISLANDS = new Country("Turks and Caicos Islands", "TC", "1649");
	@SuppressWarnings("nls")
	private static final Country TUVALU = new Country("Tuvalu", "TV", "688");
	@SuppressWarnings("nls")
	private static final Country UGANDA = new Country("Uganda", "UG", "256");
	@SuppressWarnings("nls")
	private static final Country UKRAINE = new Country("Ukraine", "UA", "380");
	@SuppressWarnings("nls")
	private static final Country UNITED_ARAB_EMIRATES = new Country("United Arab Emirates", "AE", "971");
	@SuppressWarnings("nls")
	private static final Country UNITED_KINGDOM = new Country("United Kingdom", "GB", "44");
	@SuppressWarnings("nls")
	private static final Country UNITED_STATES = new Country("United States", "US", "1");
	@SuppressWarnings("nls")
	private static final Country URUGUAY = new Country("Uruguay", "UY", "598");
	@SuppressWarnings("nls")
	private static final Country US_VIRGIN_ISLANDS = new Country("US Virgin Islands", "VI", "1340");
	@SuppressWarnings("nls")
	private static final Country UZBEKISTAN = new Country("Uzbekistan", "UZ", "998");
	@SuppressWarnings("nls")
	private static final Country VANUATU = new Country("Vanuatu", "VU", "678");
	@SuppressWarnings("nls")
	private static final Country VENEZUELA = new Country("Venezuela", "VE", "58");
	@SuppressWarnings("nls")
	private static final Country VIETNAM = new Country("Vietnam", "VN", "84");
	@SuppressWarnings("nls")
	private static final Country WALLIS_AND_FUTUNA = new Country("Wallis and Futuna", "WF", "681");
	@SuppressWarnings("nls")
	private static final Country WEST_BANK = new Country("West Bank", "", "970");
	@SuppressWarnings("nls")
	private static final Country WESTERN_SAHARA = new Country("Western Sahara", "EH", "");
	@SuppressWarnings("nls")
	private static final Country YEMEN = new Country("Yemen", "YE", "967");
	@SuppressWarnings("nls")
	private static final Country ZAMBIA = new Country("Zambia", "ZM", "260");
	@SuppressWarnings("nls")
	private static final Country ZIMBABWE = new Country("Zimbabwe", "ZW", "263");
	private static final Country[] ALL_COUNTRIES = {AFGHANISTAN, ALBANIA, ALGERIA, AMERICAN_SAMOA, ANDORRA, 
		ANGOLA, ANGUILLA, ANTARCTICA, ANTIGUA_AND_BARBUDA, ARGENTINA, ARMENIA, ARUBA, AUSTRALIA, AUSTRIA, 
		AZERBAIJAN, BAHAMAS, BAHRAIN, BANGLADESH, BARBADOS, BELARUS, BELGIUM, BELIZE, BENIN, BERMUDA, BHUTAN, 
		BOLIVIA, BOSNIA_AND_HERZEGOVINA, BOTSWANA, BRAZIL, BRITISH_INDIAN_OCEAN_TERRITORY, 
		BRITISH_VIRGIN_ISLANDS,  BRUNEI, BULGARIA, BURKINA_FASO, BURMA_MYANMAR, BURUNDI, CAMBODIA, CAMEROON, 
		CANADA, CAPE_VERDE, CAYMAN_ISLANDS, CENTRAL_AFRICAN_REPUBLIC, CHAD, CHILE, CHINA, CHRISTMAS_ISLAND, 
		COCOS_KEELING_ISLANDS, COLOMBIA, COMOROS, COOK_ISLANDS, COSTA_RICA, CROATIA, CUBA, CYPRUS, 
		CZECH_REPUBLIC, DEMOCRATIC_REPUBLIC_OF_THE_CONGO, DENMARK, DJIBOUTI, DOMINICA, DOMINICAN_REPUBLIC, 
		ECUADOR, EGYPT, EL_SALVADOR, EQUATORIAL_GUINEA, ERITREA, ESTONIA, ETHIOPIA, FALKLAND_ISLANDS, 
		FAROE_ISLANDS, FIJI, FINLAND, FRANCE, FRENCH_POLYNESIA, GABON, GAMBIA, GAZA_STRIP, GEORGIA, GERMANY, 
		GHANA, GIBRALTAR, GREECE, GREENLAND, GRENADA, GUAM, GUATEMALA, GUINEA, GUINEA_BISSAU, GUYANA, HAITI, 
		HOLY_SEE_VATICAN_CITY, HONDURAS, HONG_KONG, HUNGARY, ICELAND, INDIA, INDONESIA, IRAN, IRAQ, IRELAND, 
		ISLE_OF_MAN, ISRAEL, ITALY, IVORY_COAST, JAMAICA, JAPAN, JERSEY, JORDAN, KAZAKHSTAN, KENYA, KIRIBATI, 
		KOSOVO, KUWAIT, KYRGYZSTAN, LAOS, LATVIA, LEBANON, LESOTHO, LIBERIA, LIBYA, LIECHTENSTEIN, LITHUANIA, 
		LUXEMBOURG, MACAU, MACEDONIA, MADAGASCAR, MALAWI, MALAYSIA, MALDIVES, MALI, MALTA, MARSHALL_ISLANDS, 
		MAURITANIA, MAURITIUS, MAYOTTE, MEXICO, MICRONESIA, MOLDOVA, MONACO, MONGOLIA, MONTENEGRO, MONTSERRAT, 
		MOROCCO, MOZAMBIQUE, NAMIBIA, NAURU, NEPAL, NETHERLANDS, NETHERLANDS_ANTILLES, NEW_CALEDONIA, 
		NEW_ZEALAND, NICARAGUA, NIGER, NIGERIA, NIUE, NORFOLK_ISLAND, NORTH_KOREA, NORTHERN_MARIANA_ISLANDS, 
		NORWAY, OMAN, PAKISTAN, PALAU, PANAMA, PAPUA_NEW_GUINEA, PARAGUAY, PERU, PHILLIPINES, PITCAIRN_ISLANDS, 
		POLAND, PORTUGAL, PUERTO_RICO, QATAR, REPUBLIC_OF_THE_CONGO, ROMANIA, RUSSIA, RWANDA, SAINT_BARTHELEMY, 
		SAINT_HELENA, SAINT_KITTS_AND_NEVIS, SAINT_LUCIA, SAINT_MARTIN, SAINT_PIERRE_AND_MIQUELON, 
		SAINT_VINCENT_AND_THE_GRENADINES, SAMOA, SAN_MARINO, SAO_TOME_AND_PRINCIPE, SAUDI_ARABIA, SENEGAL, 
		SERBIA, SEYCHELLES, SIERRA_LEONE, SINGAPORE, SLOVAKIA, SLOVENIA, SOLOMON_ISLANDS, SOMALIA, 
		SOUTH_AFRICA, SOUTH_KOREA, SPAIN, SRI_LANKA, SUDAN, SURINAME, SVALBARD, SWAZILAND, SWEDEN, SWITZERLAND, 
		SYRIA, TAIWAN, TAJIKISTAN, TANZANIA, THAILAND, TIMOR_LESTE, TOGO, TOKELAU, TONGA, TRINIDAD_AND_TOBAGO, 
		TUNISIA, TURKEY, TURKMENISTAN, TURKS_AND_CAICOS_ISLANDS, TUVALU, UGANDA, UKRAINE, UNITED_ARAB_EMIRATES, 
		UNITED_KINGDOM, UNITED_STATES, URUGUAY, US_VIRGIN_ISLANDS, UZBEKISTAN, VANUATU, VENEZUELA, VIETNAM, 
		WALLIS_AND_FUTUNA, WEST_BANK, WESTERN_SAHARA, YEMEN, ZAMBIA, ZIMBABWE};

}
