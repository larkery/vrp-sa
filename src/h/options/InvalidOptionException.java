package h.options;


public class InvalidOptionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1326382489991283946L;
	private String option;

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return "No Such Option: " + option;
	}

	public InvalidOptionException(String option) {
		this.option = option;
	}

}
