import java.util.HashMap;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;


public class NNAgent extends AbstractAgent {
	private AdAuctionNeuralNetwork my_network;
	private HashMap<Query, Double> day_zero_bids = new HashMap<Query, Double>();
	private HashMap<Query, Double> day_zero_costs = new HashMap<Query, Double>();
	private HashMap<Query, Double> day_zero_revenues =new HashMap<Query, Double>();
	
	/**
	 * Object containing information about the current status of the game
	 * {@link SimulationStatus}
	 */
	protected SimulationStatus simulationStatus;
	
	/**
	 * The basic advertiser specific information. {@link AdvertiserInfo}
	 * contains
	 * <ul>
	 * <li>the manufacturer specialty</li>
	 * <li>the component specialty</li>
	 * <li>the manufacturer bonus</li>
	 * <li>the component bonus</li>
	 * <li>the distribution capacity discounter</li>
	 * <li>the address of the publisher agent</li>
	 * <li>the distribution capacity</li>
	 * <li>the address of the advertiser agent</li>
	 * <li>the distribution window</li>
	 * <li>the target effect</li>
	 * <li>the focus effects</li>
	 * </ul>
	 * An agent should receive the {@link AdvertiserInfo} at the beginning of
	 * the game or during recovery.
	 */
	protected AdvertiserInfo advertiserInfo;	
	
	
	public NNAgent()
	{
		super();
		System.out.println("Calling Constructor for NN Agent");
		my_network = Runner.getNetwork();
		System.out.println("Network loaded from file");
	}
	
    /**
     * Sends a constructed {@link BidBundle} from any updated bids, ads, or spend limits.
     */
    protected void sendBidAndAds() {
        BidBundle bidBundle = new BidBundle();

        String publisherAddress = advertiserInfo.getPublisherId();

        int the_query_type_id = 0;
        for(Query query : querySpace) {
            // The publisher will interpret a NaN bid as
            // a request to persist the prior day's bid
            double bid = 0.55;

            the_query_type_id ++;
            bid =getOptimumBid(the_query_type_id, day_zero_bids.get(query), 
            		day_zero_revenues.get(query) -
            		               day_zero_costs.get(query));
            
            System.out.println("Bidding " + bid + " for " + query.toString());
            

            // The publisher will interpret a null ad as
            // a request to persist the prior day's ad
            Ad ad = new Ad();
            
            if (advertiserInfo.getManufacturerSpecialty().equals(query.getManufacturer()))
            {
            	
            }
            if( advertiserInfo.getComponentSpecialty().equals(query.getComponent()))
            {
            	ad = new Ad(new Product(query.getManufacturer(),
            			query.getComponent()));
            }
            
            
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
    
	protected void handleQueryReport(QueryReport queryReport) 
	{
		int day = this.simulationStatus == null ? -99 : this.simulationStatus.getCurrentDate() - 1;
		if(day < 0)
		{ // don't collect data before the 3rd day as this is the first day reports will contain any data from first day
			return;
		}
		else
		{
			for(int i = 0; i < 16; i++)
			{
				for (Query q :querySpace)
				{
					day_zero_bids.put(q, queryReport.getCPC(q));
					day_zero_costs.put(q, queryReport.getCost(q));
				}
			}
		}
		
			
	}   
	
	protected void handleSalesReport(SalesReport salesReport) 
	{
		int day = this.simulationStatus == null ? -99 : this.simulationStatus.getCurrentDate() - 1;
		if(day < 0) // don't collect data before the 3rd day as this is the first day reports will contain any data from first day
		{
			return;
		}
		else
		{
			for(int i = 0; i < 16; i++)
			{
				for (Query q :querySpace)
				{
					day_zero_revenues.put(q, salesReport.getRevenue(q));
					
				}
			}
			
		}
			
	}
	protected void handleSimulationStatus(SimulationStatus simulationStatus) 
	{
		this.simulationStatus = simulationStatus;
	}
	
	/**
	 * Processes the messages received the by agent from the server.
	 * 
	 * @param message
	 *            the message
	 */
	protected void messageReceived(Message message) {
		Transportable content = message.getContent();

		if (content instanceof QueryReport) {
			handleQueryReport((QueryReport) content); 			// 7 V
		} else if (content instanceof SalesReport) {
			handleSalesReport((SalesReport) content); 			// 6 V
		} else if (content instanceof SimulationStatus) {
			handleSimulationStatus((SimulationStatus) content); // 8 V
		} else if (content instanceof PublisherInfo) {
			handlePublisherInfo((PublisherInfo) content); 		// 4 V
		} else if (content instanceof SlotInfo) {
			handleSlotInfo((SlotInfo) content); 				// 3 V
		} else if (content instanceof RetailCatalog) {
			handleRetailCatalog((RetailCatalog) content); 		// 2 V
		} else if (content instanceof AdvertiserInfo) {
			handleAdvertiserInfo((AdvertiserInfo) content); 	// 5 V
		} else if (content instanceof StartInfo) {
			handleStartInfo((StartInfo) content); 				// 1 V
		}
	}

	

    /**
     * Searches for the optimum bid by querying the network.
     * @param the_query_type_id
     * @param the_day_zero_bid
     * @param the_day_zero_profit
     * @return
     */
    private double getOptimumBid(int the_query_type_id, double the_day_zero_bid, double the_day_zero_profit)
    {
    	System.out.println("Calling Get Optimum Bid");
    	double max_profit = Double.MIN_VALUE; 	
    	double the_best_bid = 0.0;
    	for(int i = 0; i < 100; i++ )
    	{
    		double the_bid = i/100.0;
    		the_best_bid = 0.0;
    		
    		double the_profit = my_network.predict(the_query_type_id, the_day_zero_bid, the_day_zero_profit, the_bid);
    		if(the_profit > max_profit)
    		{
    			max_profit = the_profit;
    			the_best_bid = the_bid;
    		}
    	}
    	
    	return the_best_bid;
    }


}
