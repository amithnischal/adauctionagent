import java.io.IOException;

public class Runner {

	
	private Runner()
	{
	}
	
	   private static class RunnerHolder { 
		     private static final Runner INSTANCE = new Runner();
		     private static final AdAuctionNeuralNetwork NETWORK = 
		    	 new AdAuctionNeuralNetwork("Prediction.snet");
		   }
		 
		   public static Runner getInstance() {
		     return RunnerHolder.INSTANCE;
		   }	

     	public static AdAuctionNeuralNetwork getNetwork()
			{
				return RunnerHolder.NETWORK;
			}

	public static void main(String[] args) throws Exception 
	{
		Runner temp = getInstance();

	/* String host = "localhost";
	    String host = "michaelmunsey.com";
	    String autojoin = "3";
	    String login = "rac6";
	    String pass = "pa$$w0rd";
	  	    
		
        String port = "6502";

        String autojoin = "1";
        edu.umich.eecs.tac.aa.agentware.Main.main(
                new String[] 
                       {"-serverHost", host,
                        "-serverPort", port,
                        "-agentName", ,
                        "-agentPassword","password",
                        "-agentImpl","TestAgent",
                        "-log.consoleLevel","4",
                        "-log.fileLevel","6",
                        "-autojoin","" + autojoin});

	}
	*/

		/*String host = "localhost";
	    String host = "michaelmunsey.com";
	    String autojoin = "3";
	    String login = "jp";
	    String pass = "password";
	*/
	    //String host = "michaelmunsey.com";  
	    String host = "localhost";  
		String port = "6502";
	    String autojoin = "1";
	    String login = "jp";
	    String pass = "password";
	    //String login = "rac6";
	    //String pass = "pa$$w0rd";
	    
	    try {
	      edu.umich.eecs.tac.aa.agentware.Main.main(
	              new String[] 
	                     {"-serverHost", host,
	                      "-serverPort", port,
	                      "-agentName", login,
	                      "-agentPassword", pass,
	                      "-agentImpl", "RandomAgent",
	                      "-log.consoleLevel", "4",
	                      "-log.fileLevel", "6",
	                      "-autojoin", "" + autojoin});
	    }
	    catch(IOException e) {
	      
	      e.printStackTrace();
	    }	
}

}
