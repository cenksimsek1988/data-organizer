package cenk.dataorganizer.util;

import java.sql.Date;
import java.util.Calendar;
import java.util.TreeMap;

import org.joda.time.LocalDate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cenk.dataorganizer.exception.DOException;

public class ParserUtils implements DOConstants{
	
	public static char returnIfOperator(char toCheck) throws DOException {
		for(char op:EXCEL_OPERATORS) {
			if(op==toCheck) {
				return op;
			}
		}
		throw new DOException(String.valueOf(toCheck));
	}
	
	public static int getOperatorStartIndex(String s) {
		for(int i = 0; i < s.length(); i++) {
			for(char op:EXCEL_OPERATORS) {
				if(op==s.charAt(i)) {
					return i;
				}
			}
		}
		return s.length();
	}
	
	public static String getStringBeforeOperator(String s) {
		int opIndex = getOperatorStartIndex(s);
		return s.substring(0, opIndex);
	}
	
	public static LocalDate getDateNow() {
		return LocalDate.now();
	}
	
	public static Date sqlDate(LocalDate d) {
		return	new Date(d.toDate().getTime());
	}
	
	public static LocalDate resolveValidDate(Calendar c) throws DOException {
		String dateString = c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH);
		return resolveValidDate(dateString);
	}
	
	public static LocalDate resolveValidDate(String s) throws DOException {
		try {
			return LocalDate.parse(s);
		} catch (IllegalArgumentException e) {
			throw new DOException("String '" + s + "' cannot be converted to a Local Date");
		}
	}

	public static Integer resolveValidInteger(String s) throws DOException {
		Integer answer = null;
		try {
			answer = Integer.valueOf(s);
		} catch (NumberFormatException e){
			throw new DOException("Integer value cannot be converted from string: " + s);
		}
		return answer;
	}

	public static Integer optValidIntegerId(String s) {
		try {
			return resolveValidInteger(s);
		} catch (DOException e) {
			return null;
		}
	}
	
	public static String resolveValidString (String s)throws DOException {
		return resolveValidString(s, null);
	}

	public static String resolveValidString(String s, Integer expectedLenght) throws DOException{
		if (s == null) {
			throw new DOException("string value is null: ");
		}
		if (s.length()==0) {
			throw new DOException("string value is empty: " + s);
		}
		if (s.trim().length() == 0) {
			throw new DOException("string value is white space: " + s);
		}
		if (s.equalsIgnoreCase("NA") | s.equalsIgnoreCase("null")) {
			throw new DOException("string value is hardcoded null: " + s);
		}
		if (expectedLenght != null) {
			if (s.length() != expectedLenght) {
				throw new DOException("string value lenght is not as expected. expectedlenght: " + expectedLenght + ". ");
			}
			String x = "";
			for (int i = 0; i < expectedLenght; i++) {
				x += "X";
			}
			if (s.equalsIgnoreCase(x)) {
				throw new DOException("string value seems as null hardcoded. value: " + s);
			}
		}
		return s;
	}

	public static double resolveValidDouble(String s) throws DOException{
		try {
			return Double.parseDouble(s);
		} catch (NullPointerException e1) {
			throw new DOException("Cannot resolve a double from string, because it is null");
		} catch (NumberFormatException e2) {
			throw new DOException("Cannot resolve a double from string: " + s);
		}
	}
	public static String pickTheFirstWordFromCamelCase(String s) {
		int firstCap = firstCapitalIndex(s);
		if(firstCap != -1) {
			s = s.substring(0, firstCap);
			String firstChar = String.valueOf(s.charAt(0)).toLowerCase();
			return firstChar + s.substring(1);
		}
		return null;
	}
	
	public static int firstCapitalIndex(String str) {        
		for(int i = 1; i < str.length(); i++) {
			if(Character.isUpperCase(str.charAt(i))) {
				return i;
			}
		}
		return -1;
	}
	
	public static TreeMap<String, String> getAsMap(Node e){
		TreeMap<String, String> values = new TreeMap<String, String>();
		NodeList list = e.getChildNodes();
		for(int i=0; i < list.getLength(); i++) {
			Node n = list.item(i);
			String value = null;
			try {
				String pseudoValue = n.getTextContent();
				value = resolveValidString(pseudoValue, null);
			} 
			catch (DOException e2) {
				continue;
			}
			values.put(n.getNodeName(), value);
		}
		return values;
	}
}
