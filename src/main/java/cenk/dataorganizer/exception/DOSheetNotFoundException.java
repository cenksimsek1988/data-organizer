package cenk.dataorganizer.exception;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DOSheetNotFoundException extends Exception {
	private static final long serialVersionUID = 4214920247209728250L;
	private String msg;
	public DOSheetNotFoundException(String msg) {
		this.msg = msg;
	}
	@Override
	public String toString() {
		return msg;
	}
}
