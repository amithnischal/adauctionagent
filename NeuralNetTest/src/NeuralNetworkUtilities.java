import org.joone.io.JDBCInputSynapse;
import org.joone.io.StreamInputSynapse;
import org.joone.util.LearningSwitch;

/** 
 * A collection of static methods that can be used by other classes in building networks.
 * @author John Patanian
 *
 */
public final class NeuralNetworkUtilities 
{
	/**
	 * 
	 * @param the_first_row The first row of data.
	 * @param the_last_row The last row of data.
	 * @param the_first_col The first column of data.
	 * @param the_last_col The last column of data.
	 * @return a JDBCInputSynapse connection to the data source.
	 */
	public static JDBCInputSynapse createInput(final int the_first_row, 
			final int the_last_row, final int the_first_col, final int the_last_col,
			final String the_db_url, final String the_db_driver, final String the_db_query)
	{
		JDBCInputSynapse input = new JDBCInputSynapse();
		input.setdbURL(the_db_url);
		input.setBuffered(true);
		input.setdriverName(the_db_driver);
		input.setSQLQuery(the_db_query);
		input.setFirstRow(the_first_row);
		input.setLastRow(the_last_row);
		
		if (the_first_col != the_last_col)
		{
			input.setAdvancedColumnSelector(the_first_col + "-" + the_last_col);
		}
		else
		{
			input.setAdvancedColumnSelector(Integer.toString(the_first_col));
		}	
		
		return input;	
	}
	
	/**
	 * Builds a learning switch with synapses attached for learning and for validation.
	 * @param the_training_synapse The Input Training Synapse.
	 * @param the_validation_synapse The Input Validation Synapse.
	 * @return  A Learning Switch with attached synapses.
	 */
	public static LearningSwitch createSwitch(StreamInputSynapse the_training_synapse, 
			StreamInputSynapse the_validation_synapse)
	{
		LearningSwitch the_switch = new LearningSwitch();
		the_switch.addTrainingSet(the_training_synapse);
		the_switch.addValidationSet(the_validation_synapse);
		
		return the_switch;
	}	

}
