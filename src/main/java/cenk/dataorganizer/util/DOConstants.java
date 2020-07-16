package cenk.dataorganizer.util;

import org.joda.time.LocalDate;

public interface DOConstants {
	public static final char FORMULA_DEFINER = '_';
	
	public static final char[] EXCEL_OPERATORS = new char[] {
			'*',
			'/',
			'-',
			'+',
			'(',
			')',
			',',
			';',
			'=',
			' '
	};

	
	public final static String DATE_FORMAT = "yyyy-MM-dd";
	
	public static final LocalDate DATE_DEFAULT = LocalDate.parse("1970-01-01");
	public static final LocalDate TOO_LATE = LocalDate.parse("2099-12-31");
	public static final LocalDate TOO_EARLY = LocalDate.parse("1600-01-01");
	
	
	public static final String PARAM_ERROR = "error";
	public static final String PARAM_HAS_NEXT = "has_next";
	public static final String PARAM_LIST = "list";
	public static final String PARAM_SUCCESS = "success";
	public static final String PARAM_DATE = "date";
	public static final String PARAM_FREQUENCY_ID = "frequency_id";
	
	
	public static final int ERROR_CONNECTION = 0;
	public static final int ERROR_INVALID_RESPONSE_FORMAT = 1;
	public static final int ERROR_INVALID_RESPONSE_BODY = 2;
	public static final int ERROR_INVALID_VALUE = 3;
	public static final int ERROR_EMPTY_OBJECT_LIST = 4;
	public static final int ERROR_INVALID_METADATA_FORMAT = 5;
	public static final int ERROR_PARSING_METADATA = 13;
	public static final int ERROR_PARSING_LIST = 14;
	public static final int ERROR_INVALID_DATA_ARRAY_FORMAT = 6;
	public static final int ERROR_INVALID_JSON_OBJECT_FORMAT = 7;
	public static final int ERROR_NULL_JSON_ARRAY = 8;
	public static final int ERROR_EMPTY_JSON_ARRAY = 9;
	public static final int ERROR_INVALID_XML_NODE_TAG = 10;
	public static final int ERROR_PARSING_OBJECT_FROM_XML = 11;
	public static final int ERROR_WRITING_TO_EXCEL = 12;
	
	
	public static final String REGEX_SUFFIX = "(.*)";


}
