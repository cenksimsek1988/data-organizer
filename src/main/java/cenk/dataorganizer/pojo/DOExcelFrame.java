package cenk.dataorganizer.pojo;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Builder @Getter
public class DOExcelFrame {
	private List<String> columnNames;
	private String sheetName;
	@Singular
	private Map<String, List<Double>> rows;
}
