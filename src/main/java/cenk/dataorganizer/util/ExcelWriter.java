package cenk.dataorganizer.util;

import static cenk.dataorganizer.util.ParserUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import cenk.dataorganizer.exception.DOException;
import cenk.dataorganizer.exception.DOSheetNotFoundException;
import lombok.NonNull;

public abstract class ExcelWriter {

	// Create a Workbook
	protected XSSFWorkbook workbook;

	/* CreationHelper helps us create instances of various things like DataFormat, 
    Hyperlink, RichTextString etc, in a format (HSSF, XSSF) independent way */
	private CreationHelper createHelper;

	private DataFormat formatter;

	// Create a Font for styling header cells
	private Font headerFont;

	// Create a CellStyle with the font
	private CellStyle headerCellStyle;

	// Create a Row as Header
	private Row headerRow;

	// Create Cell Style for formatting Date
	private CellStyle styleDate, styleDouble, styleInteger, styleString;

	FormulaEvaluator evaluator;

	protected LocalDate lastDate;
	private String frq;

	public Workbook getWorkbook() {
		return workbook;
	}



	public void stampLastDate(LocalDate date) {
		if(lastDate == null) {
			lastDate = date;
		} else if(date.isAfter(lastDate)) {
			lastDate = date;
		}
	}

	protected abstract void setLastDate();

	protected abstract String fileName();

	public ExcelWriter() {
		try {
			FileInputStream stream= new FileInputStream(fileName());
			try {
				workbook = new XSSFWorkbook(stream);
				stream.close();
				setLastDate();
			} catch (IOException e) {
				System.out.println("cannot create excel woorkbook from input stream. bad formed file: " + e.getMessage());
			}
		} catch (FileNotFoundException e) {
			System.out.println("file not found with name: " + fileName() + "\nnew file will be created");
			workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
		}


		createHelper = workbook.getCreationHelper();

		headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());

		headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		formatter = createHelper.createDataFormat();

		styleDate = workbook.createCellStyle();
		styleDate.setDataFormat(formatter.getFormat(DATE_FORMAT));

		styleDouble = workbook.createCellStyle();
		styleDouble.setDataFormat(formatter.getFormat("#,##0"));

		styleInteger = workbook.createCellStyle();
		styleInteger.setDataFormat(formatter.getFormat("#,##0"));

		styleString = workbook.createCellStyle();
		styleString.setDataFormat(formatter.getFormat("@"));

		evaluator = createHelper.createFormulaEvaluator();
	}

	protected XSSFSheet getSheet(String sheetName) throws DOSheetNotFoundException {
		XSSFSheet answer = workbook.getSheet(sheetName);
		if(answer==null) {
			throw new DOSheetNotFoundException("cannot find sheet with name: " + sheetName);
		}
		return answer;
	}

	protected XSSFSheet createNewSheet(String sheetName, @NonNull String[] columns) {
		XSSFSheet answer = workbook.createSheet(sheetName);
		Row header = answer.createRow(0);
		for(int i = 0; i < columns.length; i++) {
			Cell column = header.createCell(i);
			column.setCellValue(columns[i]);
		}
		return answer;
	}

	public XSSFSheet createNewSheet(String sheetName) {
		XSSFSheet answer = workbook.createSheet(sheetName);
		answer.createRow(0);
		return answer;
	}	

	private Row getHeader(XSSFSheet sheet) {
		Row answer = sheet.getRow(0);
		if(answer == null) {
			answer = sheet.createRow(0);
		}
		return answer;
	}

	private int getCellIndex(String columnName, Row header) throws DOException {
		int answer = findColumnIndex(columnName, header);
		if(answer>-1) {
			return answer;
		}
		return createNewCell(columnName, header);
	}

	private int findColumnIndex(String columnName, Row header) {
		Iterator<Cell> list = header.cellIterator();
		while(list.hasNext()) {
			Cell c = list.next();
			String headerColumn = c.getStringCellValue();
			if(headerColumn.equals(columnName.toString())) {
				return c.getColumnIndex();
			}
		}
		return -1;
	}

	private int createNewCell(String content, Row row) throws DOException {
		int celNum = row.getLastCellNum();
		if(celNum < 0) {
			celNum = 0;
		}
		return addCell(content, row, celNum);
	}

	private String getFormulaAddress(Row r, String columnName) {
		int cIndex = findColumnIndex(columnName, getHeader((XSSFSheet)r.getSheet()));
		if (cIndex>-1) {
			return CellReference.convertNumToColString(cIndex) + (r.getRowNum() + 1);
		}
		return null;
	}

	private String adjustFormula(String rawFormula, Row r) {
		String answer = rawFormula;
		while(true) {
			String columnName = null;
			try {
				columnName = readFirstColumnName(answer);
				System.out.println("column name found: " + columnName);
			} catch (DOException e) {
				break;
			}
			String address = getFormulaAddress(r, columnName);
			System.out.println("address resolved: " + address);
			answer = answer.replace(FORMULA_DEFINER + columnName, address);
			System.out.println("new formula after current adjustment: " + answer);
		}
		return answer;
	}

	private String readFirstColumnName(String s) throws DOException {
		int defIndex = findColumnDefinerStartIndex(s);
		if(defIndex == -1) {
			throw new DOException("no column definer char found");
		}
		return getStringBeforeOperator(s.substring(defIndex+1));
	}

	private int findColumnDefinerStartIndex (String s) {
		return s.indexOf(FORMULA_DEFINER);
	}

	private int addCell(String value, Row row, int celNum) throws DOException {
		Cell newCell = row.createCell(celNum);
		if(value.startsWith("=")) {
			String rawFormula = value.substring(1);
			String formula = adjustFormula(rawFormula, row);
			System.out.println("raw formula: " + formula);
			System.out.println("adjusted formula: " + formula);
			newCell.setCellType(CellType.FORMULA);
			newCell.setCellFormula(formula);
			//			evaluator.evaluate(newCell);
		}
		try {
			int iVal = ParserUtils.resolveValidInteger(value);
			newCell.setCellStyle(styleInteger);
			newCell.setCellValue(iVal);
			return newCell.getColumnIndex();
		} catch (DOException e) {

		}
		try {
			double dVal = ParserUtils.resolveValidDouble(value);
			newCell.setCellStyle(styleDouble);
			newCell.setCellValue(dVal);
			return newCell.getColumnIndex();
		} catch (DOException e) {

		}
		try {
			DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_FORMAT);
			LocalDate ldVal = LocalDate.parse(value, formatter);
			if(ldVal == null) {
				throw new DOException("cannot resolve a Local Date from string: " + value);
			}
			newCell.setCellStyle(styleDate);
			newCell.setCellValue(value);
			return newCell.getColumnIndex();
		} catch (IllegalArgumentException e) {

		}
		newCell.setCellStyle(styleString);
		newCell.setCellValue(value);
		return newCell.getColumnIndex();
	}

	public ExcelCellPointer write(TreeMap<String, String> values, String sheetName) throws DOException {
		System.out.println("attemps to write something in workbook on the sheet named: " + sheetName);
		XSSFSheet sheet = null;
		try {
			sheet = getSheet(sheetName);
		} catch (DOSheetNotFoundException e) {
			sheet = createNewSheet(sheetName);
		}
		return addRow(values, sheet);
	}

	public ExcelCellPointer write(TreeMap<String, String> values, String sheetName, String[] columns) throws DOException{
		System.out.println("attemps to write something in workbook on the sheet named: " + sheetName);
		XSSFSheet sheet = null;
		try {
			sheet = getSheet(sheetName);
		} catch (DOSheetNotFoundException e1) {
			sheet = createNewSheet(sheetName, columns);
		}
		return addRow(values, sheet);
	}

	//	private void sortSheet(XSSFSheet sheet, int column, int rowStart) {
	//		boolean sorting = true;
	//		int lastRow = sheet.getLastRowNum();
	//		sheet.lockSort(true);
	//		while (sorting) {
	//			sorting = false;
	//			for (Row row : sheet) {
	//				if (row.getRowNum() < rowStart) continue;
	//				if (lastRow == row.getRowNum()) break;
	//				Row nextRow = sheet.getRow(row.getRowNum() + 1);
	//				if (nextRow == null) continue;
	//				String firstValue = row.getCell(column).getStringCellValue() ;
	//				String secondValue = nextRow.getCell(column).getStringCellValue() ;
	//				if (secondValue.compareTo(firstValue) < 0) {                    
	//					sheet.shiftRows(nextRow.getRowNum(), nextRow.getRowNum(), -1);
	//					sheet.shiftRows(row.getRowNum(), row.getRowNum(), 1);
	//					sorting = true;
	//				}
	//			}
	//		}
	//	}

	protected boolean hasKeyColumn() {
		return false;
	}

	protected String keyColumnName() {
		return null;
	}

	private ExcelCellPointer addRow(TreeMap<String, String> values, XSSFSheet sheet) throws DOException {
		headerRow = getHeader(sheet);
		int rowNum = sheet.getLastRowNum() + 1;
		if(rowNum > 1 && hasKeyColumn()) {
			Iterator<Row> rows = sheet.rowIterator();
			String lookUp = values.get(keyColumnName());
			if(lookUp == null) {
				String colLet = addRow(sheet, values, rowNum);
				return new ExcelCellPointer(sheet, rowNum, colLet);
			}
			int lookUpInt = 0;
			try {
				lookUpInt = Integer.valueOf(lookUp);
			} catch (NumberFormatException e) {
				if(lookUp.equals("0.0") || lookUp.equals(".")) {
					lookUpInt = 0;
				}
			}
			rows.next(); //skip the header
			while(rows.hasNext()) {
				Row r = rows.next();
				Cell c = r.getCell(0);
				if(c == null) {
					continue;
				}
				try {
					String id = c.getStringCellValue();
					if(id.equals(lookUp)) {
						String colLet = addCells(r, values);
						return new ExcelCellPointer(sheet, rowNum, colLet);
					}

				} catch (IllegalStateException e) {
					try{
						int id = (int) c.getNumericCellValue();
						if(id == lookUpInt) {
							String colLet = addCells(r, values);
							return new ExcelCellPointer(sheet, rowNum, colLet);
						}
					} catch (IllegalStateException e2) {
						throw new DOException("not even a numeric cell" + c +  "\n" + e2.getMessage());
					}
				}
			}
		}
		String colLet = addRow(sheet, values, rowNum);
		return new ExcelCellPointer(sheet, rowNum, colLet);
	}

	private String addRow(XSSFSheet sheet, TreeMap<String, String> values, int rowNum) throws DOException {
		Row newRow = sheet.createRow(rowNum);
		return addCells(newRow, values);
	}

	private String addCells(Row row, TreeMap<String, String> values) throws DOException {
		int lastEntryColIndex = 0;
		for(String key: values.descendingKeySet()) {
			int celNum = getCellIndex(key, headerRow);
			String value = null;
			try {
				value = ParserUtils.resolveValidString(values.get(key), null);
			} catch (DOException e) {
				continue;
			}
			lastEntryColIndex = addCell(value, row, celNum);
		}
		return CellReference.convertNumToColString(lastEntryColIndex);
	}

	public void flush(String fileName) throws DOException {
		writeExtras();

		// Resize all CRITERIA_COLUMNS to fit the content size
		for(int j = 0; j < workbook.getNumberOfSheets(); j++) {
			XSSFSheet sheet = workbook.getSheetAt(j);
			//			sortSheet(sheet, 0, 1);
			Row header = sheet.getRow(0);
			for(int i = 0; i < header.getLastCellNum(); i++) {
				sheet.autoSizeColumn(i);
			}
		}

		// Write the output to a file
		FileOutputStream fileOut = null;
		File file = new File(fileName);
		try {
			fileOut = new FileOutputStream(file, false);
		} catch (FileNotFoundException e) {
			System.out.println("cannot find excel file with name: " + fileName);
			try {
				file.createNewFile();
			} catch (IOException e1) {
				throw new DOException("cannot create a new file with name: " + fileName);
			}
		}
		try {
			workbook.write(fileOut);
			//		    workbook = new XSSFWorkbook(new FileInputStream(fileName()));
		} catch (IOException e) {
			throw new DOException("cannot write into excel\n" + e.getMessage());
		}
		try {
			fileOut.close();
		} catch (IOException e) {
			throw new DOException("cannot close output stream");
		}
		//		try {
		//			workbook.close();
		//		} catch (IOException e) {
		//			throw new MyException("cannot close excel file\n" + e.getMessage());
		//		}
	}

	//	private TreeMap<String, String> getPeriodInfo(){
	//		TreeMap<String, String> answer = new TreeMap<>();
	//		answer.put(lastDate.toString(), lastDate.toString());
	//		answer.put("Frekans", frq);
	//		return answer;
	//	}

	protected abstract void writeExtras();
}
