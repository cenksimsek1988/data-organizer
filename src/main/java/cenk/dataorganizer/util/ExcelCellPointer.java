package cenk.dataorganizer.util;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;


public class ExcelCellPointer {
	private int rowIndex = -1;
	private int columnIndex = -1;
	private Sheet sheet;
	private Row header;
	private String columnName;
	private String code;
	private int codeColumnIndex = -1;
	private Row row;
	private String address;
	private String columnLetter;
	private int rowNum = -1;
	private String fullAddress;
	private String sheetName;
	
	public ExcelCellPointer(Sheet sheet, int rowIndex, String columnLetter) {
		this.sheet = sheet;
		columnIndex = CellReference.convertColStringToIndex(columnLetter);
		this.rowIndex = rowIndex;
		header = sheet.getRow(0);
	}
	
	public ExcelCellPointer(Sheet sheet, String rowName, String columnName) {
		this.sheet = sheet;
		this.columnName = columnName;
		code = rowName;
		header = sheet.getRow(0);
	}
	
	public int getRowNum() {
		if(rowNum == -1) {
			rowNum = getRowIndex() + 1;
		}
		return rowNum;
	}
	
	public int getRowIndex() {
		if(rowIndex == -1) {
			Iterator<Row> rows = sheet.rowIterator();
			rows.next();
			while(rows.hasNext()) {
				Row r = rows.next();
				Cell c = r.getCell(getCodeColumnIndex());
				String id = c.getStringCellValue();
				if(id.equals(getCode())) {
					row = r;
					rowIndex = r.getRowNum();
					break;
				}
			}
		}
		return rowIndex;
	}
	
	protected String uniqueColumnName() {
		return null;
	}
	
	public String getCode() {
		if(code == null) {
			code = getRow().getCell(getCodeColumnIndex()).getStringCellValue();
		}
		return code;
	}
	
	public int getCodeColumnIndex() {
		if(codeColumnIndex == -1) {
			codeColumnIndex = getColumnIndex(uniqueColumnName());
		}
		return codeColumnIndex;
	}	
	public String getAddress() {
		if(address == null) {
			address = getColumnLetter() + getRowNum();
		}
		return address;
	}
	
	public String getColumnLetter() {
		if(columnLetter == null) {
			columnLetter = CellReference.convertNumToColString(getColumnIndex());
		}
		return columnLetter;
	}
	
	public int getColumnIndex() {
		if(columnIndex == -1) {
			columnIndex = getColumnIndex(getColumnName());
		}
		return columnIndex;
	}
	
	

	public String getColumnName() {
		if(columnName==null) {
			columnName = header.getCell(columnIndex).getStringCellValue();
		}
		return columnName;
	}
	
	public Row getRow() {
		if(row == null) {
			row = sheet.getRow(rowIndex);
		}
		return row;
	}
	
	protected int getColumnIndex(String columnName) {
		Iterator<Cell> iterator = getHeader().cellIterator();
		while(iterator.hasNext()) {
			Cell c = iterator.next();
			if(c.getStringCellValue().equals(columnName)) {
				return c.getColumnIndex();
			}
		}
		return -1;
	}
	
	public Row getHeader() {
		if(header == null) {
			header = sheet.getRow(0);
		}
		return header;
	}
	
	public String getSheetName() {
		if(sheetName == null) {
			sheetName = sheet.getSheetName();
		}
		return sheetName;
	}
	public String getFullAddress() {
		if(fullAddress == null) {
			fullAddress = getSheetName() + "!" + getAddress();
		}
		return fullAddress;
	}
}