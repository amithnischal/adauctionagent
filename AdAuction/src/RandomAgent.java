import java.util.Random;
import java.sql.*;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;


public class RandomAgent extends AbstractAgent 
{
	public RandomAgent()
	{
		super();
		try {
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String url =  "jdbc:jtds:sqlserver://localhost:1433/adauction;loginTimeout=20";
		try {
			Connection con = DriverManager.getConnection(url, "admin", "");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	   /**
     * Sends a constructed {@link BidBundle} from any updated bids, ads, or spend limits.
     */
    protected void sendBidAndAds() {
        BidBundle bidBundle = new BidBundle();
        
        Random r = new Random();
        String publisherAddress = advertiserInfo.getPublisherId();

        
        for(Query query : querySpace) {
            // The publisher will interpret a NaN bid as
            // a request to persist the prior day's bid
            double bid = 0.55;
            // bid = [ calculated optimal bid ]


            // The publisher will interpret a null ad as
            // a request to persist the prior day's ad
            Ad ad = null;
            // ad = [ calculated optimal ad ]


            // The publisher will interpret a NaN spend limit as
            // a request to persist the prior day's spend limit
            double spendLimit = Double.NaN;
            // spendLimit = [ calculated optimal spend limit ]


            // Set the daily updates to the ad campaigns for this query class
            bidBundle.addQuery(query,  bid, ad);
            bidBundle.setDailyLimit(query, spendLimit);
        }

        // The publisher will interpret a NaN campaign spend limit as
        // a request to persist the prior day's campaign spend limit
        double campaignSpendLimit = Double.NaN;
        // campaignSpendLimit = [ calculated optimal campaign spend limit ]
        // Set the daily updates to the campaign spend limit
        bidBundle.setCampaignDailySpendLimit(campaignSpendLimit);

        // Send the bid bundle to the publisher
        if (publisherAddress != null) {
            sendMessage(publisherAddress, bidBundle);
        }
    }

}
