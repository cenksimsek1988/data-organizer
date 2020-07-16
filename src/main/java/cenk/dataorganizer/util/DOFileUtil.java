package cenk.dataorganizer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

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
}
