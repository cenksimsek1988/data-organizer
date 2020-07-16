package cenk.dataorganizer.exception;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DOException extends Exception {
	private static final long serialVersionUID = 839110657769157769L;
	private String msg;
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public DOException(String msg) {
		this.msg = msg;
	}
	@Override
	public String toString() {
		return msg;
	}
	public String getMsg() {
		return msg;
	}
}
