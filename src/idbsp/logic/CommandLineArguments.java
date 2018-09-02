package idbsp.logic;

/**
 * CommandLineArguments
 * 
 * @author Agnes
 *
 */
public class CommandLineArguments {

	private String inmapname;
	private String outmapname;
	private boolean draw;
	
	
	public String getInmapname() {
		return inmapname;
	}

	public String getOutmapname() {
		return outmapname;
	}

	public boolean isDraw() {
		return draw;
	}

	public void parse(String[] args) {		
		if (args.length == 3) {
			if (!"-draw".equals(args[0])) {
				printUsage();
				System.exit(1);
			}
			inmapname = args[1];
			outmapname = args[2];
			draw = true;
		} else if (args.length == 2) {
			inmapname = args[0];
			outmapname = args[1];
			draw = false;
		} else {
			printUsage();
			System.exit(1);
		}

	}
	
	private void printUsage() {
		System.err.println("idbsp [-draw] inmap outwadpath");
	}
}
