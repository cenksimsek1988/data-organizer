package cenk.dataorganizer.pojo;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Builder @Getter
public class DORow {
	private String rowName;
	@Singular
	private List<Double> values;
}
