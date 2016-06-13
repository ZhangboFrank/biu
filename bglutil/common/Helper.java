package bglutil.common;

public class Helper {
	
	// Enable the '-h' option for methods which call help.
	public void help(String help, String helpMessage){
		if(help.equals("-h")){
			StackTraceElement element = Thread.currentThread().getStackTrace()[2];
			System.out.println("\n b "+element.getMethodName()+" "+helpMessage+"\n");
			System.exit(0);
		}
	}
	
	public void title(String title){
		StringBuffer line = new StringBuffer("\n");
		for(int c=0;c<title.length();c++){
			line.append('_');
		}
		System.out.println(line+"\\");
		System.out.println(title+":\n");
	}
	
	
}
