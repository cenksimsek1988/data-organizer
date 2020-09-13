package cenk.dataorganizer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import okhttp3.Response;

public class DOFileUtil {
	
	public static Set<File> fileList(String dir) throws IOException {
	    Set<File> fileList = new HashSet<>();
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
	        for (Path path : stream) {
	            if (!Files.isDirectory(path)) {
	                fileList.add(path.toFile());
	            }
	        }
	    }
	    return fileList;
	}
	
	public static String getCoreName(File f) {
		return FilenameUtils.getBaseName(f.getName());
	}
	
	public static File prepareDownloadFile(Response response, String folderPath) throws IOException {
		String filename = null;
		String contentDisposition = response.header("Content-Disposition");
		String[] names = contentDisposition.split("\"");
		if(names.length>1) {
			filename = names[1];
		}
//		if (contentDisposition != null && !"".equals(contentDisposition)) {
//			// Get filename from the Content-Disposition header.
//			Pattern pattern = Pattern.compile("filename=['\"]?([^'\"\\s]+)['\"]?");
//			Matcher matcher = pattern.matcher(contentDisposition);
//			if (matcher.find()) {
//				filename = sanitizeFilename(matcher.group(1));
//			}
//		}

//		String prefix = null;
//		String suffix = null;
//		if (filename == null) {
//			prefix = "download-";
//			suffix = "";
//		} else {
//			int pos = filename.lastIndexOf(".");
//			if (pos == -1) {
//				prefix = filename + "-";
//			} else {
//				prefix = filename.substring(0, pos);
//				suffix = filename.substring(pos);
//			}
//			// File.createTempFile requires the prefix to be at least three characters long
//			if (prefix.length() < 3)
//				prefix = "download-";
//		}
		return new File(folderPath, filename);
	}

//	public static String sanitizeFilename(String filename) {
//		return filename.replaceAll(".*[/\\\\]", "");
//	}
}
