package cenk.dataorganizer.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cenk.dataorganizer.exception.DOException;
import cenk.dataorganizer.exception.DOSheetNotFoundException;
import cenk.dataorganizer.pojo.DOExcelFrame;
import cenk.dataorganizer.pojo.DOExcelFrame.DOExcelFrameBuilder;
import cenk.dataorganizer.pojo.DORow;
import cenk.dataorganizer.pojo.DORow.DORowBuilder;
import cenk.dataorganizer.util.DOFileUtil;

@Service
public class DataOrganizer {
	private static final Object LOCK = new Object();
	@Value("${dataorganizer.adjusted.data.folder.path:adjusted}")
	private String adjustedDataFolderPath;

	@Value("${dataorganizer.raw.data.folder.path:raw}")
	private String rawDataFolderPath;

	@Value("${dataorganizer.adjusting.map.folder.path:map}")
	private String adjustingMapFolderPath;

	private static final Logger logger = LoggerFactory.getLogger(DataOrganizer.class);

	private final static String ADJUSTED_FILE_NAME = "Adjusted";

	public void organize() throws Exception {
		logger.info("given path for raw data folder: {}", rawDataFolderPath);
		final Set<File> rawDataFiles;
		try {
			rawDataFiles = DOFileUtil.fileList(rawDataFolderPath);
		} catch (IOException e) {
			throw new Exception("error while listing raw data files:\n" + e.getMessage());
		}
		logger.info("raw data file count: {}", rawDataFiles.size());
		TreeMap<Integer, TreeMap<Integer, String>> adjustingMap = parseAdjustingMap(adjustingMapFolderPath);
		DOExcelFrame doExcelFrame = buildExcelFrame("Adjusted", rawDataFiles, adjustingMap);
		XSSFWorkbook adjustedWorkbook = buildWorkbook(doExcelFrame);
		String adjFileName = adjustedDataFolderPath +"/" + ADJUSTED_FILE_NAME + ".xlsx";
		write(adjFileName, adjustedWorkbook);
		logger.info("adjusted data file is generated");
	}

	private static TreeMap<Integer, TreeMap<Integer, String>> parseAdjustingMap(String adjustingMapFolderPath) throws Exception {
		String fileName = adjustingMapFolderPath + "/" + "map.xls";
		File mapFile = new File(fileName);
		if (!mapFile.exists() ) {
			throw new Exception("There is no map file at path " + mapFile);
		}
		final Workbook wb = readExcelFile(mapFile);
		final Sheet sh = wb.getSheetAt(0);
		if(sh==null) {
			throw new DOSheetNotFoundException("cannot find any sheet in workbook with name: " + mapFile.getName());
		}
		return parseAdjustingMap(sh);
	}

	private static TreeMap<Integer, TreeMap<Integer, String>> parseAdjustingMap(Sheet sh) {
		TreeMap<Integer, TreeMap<Integer, String>> answer = new TreeMap<>();
		int rowStart = sh.getFirstRowNum();
		int rowEnd = sh.getLastRowNum();
		Iterator<Row> rIterator = sh.rowIterator();
		while(rIterator.hasNext()) {
			TreeMap<Integer, String> mapOfRow = new TreeMap<>();
			Row row = rIterator.next();
			int i = row.getRowNum();
			Iterator<Cell> cIterator = row.cellIterator();
			while(cIterator.hasNext()) {
				Cell cell = cIterator.next();
				int j = cell.getColumnIndex();
				if(cell.getCellType()==CellType.STRING){
					String val = cell.getStringCellValue();
					if(!Strings.isBlank(val)) {
						mapOfRow.put(j, val.trim());
					}
				}
			}
			answer.put(i, mapOfRow);
		}
		return answer;
//		for(int i = rowStart; i <= rowEnd; i++) {
//			TreeMap<Integer, String> mapOfRow = new TreeMap<>();
//			Row row = sh.getRow(i);
//			int cellStart = row.getFirstCellNum();
//			int cellEnd = row.getLastCellNum();
//			for(int j = cellStart; j < cellEnd; j++) {
//				Cell cell = row.getCell(j);
//				if(cell!=null) {
//					if(cell.getCellType()==CellType.STRING){
//						String val = cell.getStringCellValue();
//						if(!Strings.isBlank(val)) {
//							mapOfRow.put(j, val.trim());
//						}
//					}
//
//				}
//			}
//			answer.put(i, mapOfRow);
//		}
//		return answer;
	}

	private static void write(String fileName, XSSFWorkbook workbook) throws DOException {
		synchronized (LOCK) {
			FileOutputStream fileOut = null;
			File file = new File(fileName);
			if(file.exists()) {
				logger.debug("there is an existing file with name: {}", fileName);
				logger.debug("Data Organizer will delete everyting and overwrite on it");
				file.delete();
			} else {
				logger.debug("there is not any file with name: {}", fileName);
				logger.debug("Data Organizer will generate it");
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("error while creating new file {}", fileName);
				e.printStackTrace();
				throw new DOException("Cannot create the file on path");
			}
			try {
				fileOut = new FileOutputStream(file, false);
			} catch (FileNotFoundException e) {
				logger.error("error while opening output stream on file {}", fileName);
				e.printStackTrace();
				throw new DOException("Cannot find the file on path");
			}
			try {
				workbook.write(fileOut);
			} catch (IOException e) {
				logger.error("error while writing data on excel file");
				e.printStackTrace();
				throw new DOException("Cannot write data on excel");
			}
			try {
				fileOut.close();
			} catch (IOException e) {
				logger.error("error while closing output stream connection with file {}", fileName);
				e.printStackTrace();
				throw new DOException("Cannot close output stream connection of file");
			}
		}
	}

	private static DOExcelFrame buildExcelFrame(String sheetName, Set<File> rawDataFiles, Map<Integer, TreeMap<Integer, String>> adjustingMap) {
		List<String> colNames = new ArrayList<>();
		colNames.add("period");
		for(int i:adjustingMap.keySet()) {
			final Map<Integer, String> columnMap = adjustingMap.get(i);
			for(int j:columnMap.keySet()) {
				final String columnName = columnMap.get(j);
				colNames.add(columnName);
			}
		}
		DOExcelFrameBuilder excelFrameBuilder = DOExcelFrame.builder().sheetName(sheetName).columnNames(colNames);
		for(File f: rawDataFiles) {
			try {
				final DORow doRow = organizeRawDataFile(adjustingMap, f);
				excelFrameBuilder.row(doRow.getRowName(), doRow.getValues());
			} catch (Exception e) {
				logger.error("error while organizing one raw data file with name: {}", f.getName());
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		return excelFrameBuilder.build();
	}

	private static XSSFWorkbook buildWorkbook(final DOExcelFrame frame){
		final XSSFWorkbook workbook = new XSSFWorkbook();
		final XSSFSheet sh = workbook.createSheet(frame.getSheetName());
		final XSSFRow header = sh.createRow(0);
		final List<String> colNames = frame.getColumnNames();
		for(int i = 0; i < colNames.size(); i++) {
			final XSSFCell cell = header.createCell(i);
			cell.setCellValue(colNames.get(i));
		}
		final Map<String, List<Double>> rows = frame.getRows();
		int rowCount = 1;
		for(String periodName:rows.keySet()) {
			final XSSFRow row = sh.createRow(rowCount);
			final XSSFCell periodCell = row.createCell(0);
			periodCell.setCellValue(periodName);
			final List<Double> values = rows.get(periodName);
			for(int i = 0; i < values.size(); i++) {
				final XSSFCell cell = row.createCell(i+1);
				cell.setCellValue(values.get(i));
			}
			rowCount++;
		}
		return workbook;
	}

	private static DORow organizeRawDataFile(final Map<Integer, TreeMap<Integer, String>> adjustingMap, final File rawDataFile) throws Exception {
		if (!rawDataFile.exists() ) {
			throw new Exception("There is no raw data at path " + rawDataFile);
		}
		final Workbook wb = adjustHeaderAndReadExcelFile(rawDataFile);
		final Sheet sh = wb.getSheetAt(0);
		if(sh==null) {
			throw new DOSheetNotFoundException("cannot find any sheet in workbook with name: " + rawDataFile.getName());
		}
		return mapSheet(adjustingMap, sh);
	}

	private static DORow mapSheet(final Map<Integer, TreeMap<Integer, String>> adjustingMap, final Sheet sh) throws Exception {
		String shName = sh.getSheetName();
		final String periodString = shName.replaceAll("[^\\d-]", "");
		final DORowBuilder answerBuilder = DORow.builder().rowName(periodString);
		for(int i:adjustingMap.keySet()) {
			final Row row = sh.getRow(i);
			final TreeMap<Integer, String> columnMap = adjustingMap.get(i);
			for(int j:columnMap.keySet()) {
				final Cell cell = row.getCell(j);
				Double val = null;
				final CellType type = cell.getCellType();
				if(type != CellType.NUMERIC) {
					final String valString = cell.getStringCellValue().replaceAll("[^\\d,]", "").replaceAll(",", ".");
					try {
						val = Double.valueOf(valString);
					} catch (Exception e) {
						logger.error("error while parsing numeric value from String: {}", cell.getStringCellValue());
						logger.error(e.getLocalizedMessage());
						throw e;
					}
				} else {
					val = cell.getNumericCellValue();
				}
				answerBuilder.value(val);
			}
		}
		return answerBuilder.build();
	}

	private static byte[] EXCEL_HEADER;

	static {
		try {
			EXCEL_HEADER = Hex.decodeHex("E11AB1A1E011CFD0");
		} catch (DecoderException e) {
			e.printStackTrace();
		}
	}

	public static Workbook adjustHeaderAndReadExcelFile(File rawDataFile) throws Exception {
		File tmpFile = new File(rawDataFile.getPath() + rawDataFile.getName() + UUID.randomUUID());
		FileOutputStream out = new FileOutputStream(tmpFile);
		out.write(EXCEL_HEADER);
		out.write(System.getProperty("line.separator").getBytes("utf-8")[0]);
		FileInputStream originalStream= new FileInputStream(rawDataFile);
		int b=0;
		while(b!=-1) {
			b = originalStream.read();
			out.write(b);
		}
		originalStream.close();
		out.flush();
		out.close();
		return readExcelFile(tmpFile);
//		return readExcelFile(rawDataFile);
	}

	private static Workbook readExcelFile(File file) throws Exception {
		synchronized (LOCK) {
			FileInputStream stream= new FileInputStream(file);
			try {
				return new HSSFWorkbook(stream);
			} catch (IOException e) {
				logger.error("cannot create excel workbook from input stream. bad formed file");
				e.printStackTrace();
				throw new Exception("Error while reading raw data file: " + e.getLocalizedMessage());
			}
		}
	}

}
