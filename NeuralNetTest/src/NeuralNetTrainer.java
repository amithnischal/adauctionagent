import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.joone.engine.DirectSynapse;
import org.joone.engine.Layer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralValidationEvent;
import org.joone.net.NeuralValidationListener;

/**
 * This is used to train the neural network based on input from the ad auction database.
 * @author John Patanian
 *
 */
public class NeuralNetTrainer implements NeuralNetListener, NeuralValidationListener
{
	/** The error at which to halt the training. */
	private static final double MAX_ERROR = 0.013;

	/** The filename of the saved network configuration for training. */
	private static final String TRAINING_NETWORK = "AdNetwork_TRAIN.snet";
	
	/** The filename of the saved network configuration for PREDICTION. */
	private static final String PREDICTION_NETWORK = "PREDICTION.snet";
	
	private static final int DEFAULT_TRAINING_EPOCHS = 1;
	/** The learning rate for the network. */ 
	
	private static final double DEFAULT_LEARNING_RATE = 0.8;
	/** The momentum for training the network. */
	
	private static final double DEFAULT_MOMENTUM = 0.3;	
	
	
	/** Reference to the neural network object. */
	private final NeuralNet my_network; 
	/** Reference to the monitor of the neural network. */
	private final Monitor my_monitor;
	/** The number of training rows for the database. */
	private int my_training_rows;
	
	/** Public constructor with a reference to an existing neural network. */
	public NeuralNetTrainer(final NeuralNet the_network)
	{
	  my_network = the_network;
	  my_monitor = my_network.getMonitor();
	  my_network.addNeuralNetListener(this);
	}
	
	/**
	 * @param args Ignored, not used.
	 */
	public static void main(String[] args) 
	{
		/* The following two lines are for training the network */
		final AdAuctionNeuralNetwork net = new AdAuctionNeuralNetwork();

		final NeuralNetTrainer trainer = new NeuralNetTrainer(net.getNetwork());
		trainer.my_training_rows = net.getTrainingRows();
		trainer.trainNetwork();
	}
	
	/** Trains the neural network with the input data. */
	private void trainNetwork()
	{
		my_monitor.setLearning(true);
		my_monitor.setLearningRate(DEFAULT_LEARNING_RATE);
		my_monitor.setMomentum(DEFAULT_MOMENTUM);		
		my_monitor.setTrainingPatterns(my_training_rows);
		my_monitor.setTotCicles(DEFAULT_TRAINING_EPOCHS);	
		my_network.go();
	}

	@Override
	/**
	 * Part of the NeuralNetworkListener Interface.
	 * @param the_event The NeuralNetEvent that generated the cicleTerminated Event.
	 */
	public void cicleTerminated(final NeuralNetEvent the_event) 
	{
		final Monitor mon = (Monitor) the_event.getSource();
		long cycle = mon.getCurrentCicle() + 1;
		/** We want to print the results every 200 epochs */
		if (cycle % 200 == 0)
		{
			System.out.println("Epoch #" + (mon.getTotCicles() - cycle)); 
			System.out.println("     Training Error:" + mon.getGlobalError());
			
			//Creates a copy of the neural network
//			my_network.getMonitor().setExporting(true);
//			NeuralNet copy = my_network.cloneNet();
//			my_network.getMonitor().setExporting(false);
		
//			//Cleans the old listeners
//			copy.removeAllListeners();

			//Set all the parameters for the validation
/*			NeuralNetValidator validator = new NeuralNetValidator(copy);
			validator.addValidationListener(this);
			validator.start();
*/		
		}	
	}

	@Override
	public void errorChanged(final NeuralNetEvent the_event) 
	{
      Monitor mon = (Monitor)the_event.getSource();
      if (mon.getGlobalError() <= MAX_ERROR && mon.getGlobalError() != 0.0)
      {
    	  my_network.stop();
      }		
	}

	@Override
	public void netStarted(final NeuralNetEvent the_event) 
	{
		System.out.println("Network Started");
	}

	@Override
	public void netStopped(final NeuralNetEvent the_event) 
	{
		if(my_monitor.isLearning())
		{
			System.out.println("Training finished");
			saveNetwork();		
		}
		else
		{
			System.out.println("Network Stopped");			
		}
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String arg1) 
	{
		System.out.println("Net Stopped Error " + arg1 );
	}

	
	/**
	 * Saves a serialized version of the network for later recall.
	 * This saves both the version for training and the version for prediction
	 * that has different input and output synapses.
	 */
	private void saveNetwork()
	{
		try
		{
			FileOutputStream stream = new FileOutputStream(TRAINING_NETWORK);
			ObjectOutputStream out = new ObjectOutputStream(stream);

			
			my_network.removeNeuralNetListener(this);
			
			out.writeObject(my_network);
			out.close();

			configNetworkForPrediction();

			
			stream = new FileOutputStream(PREDICTION_NETWORK);
			out = new ObjectOutputStream(stream);			

			out.writeObject(my_network);
			out.close();
		}
		catch(final Exception the_exception)
		{
			the_exception.printStackTrace();
		}	
	}
	
	
	/** Configures the Neural Network for use in prediction mode. */
	private void configNetworkForPrediction()
	{
		Layer input = my_network.getInputLayer();
		input.removeAllInputs();

		DirectSynapse memInp = new DirectSynapse();
		input.addInputSynapse(memInp);
				
		/** add the input synapse to the first layer. */
		my_network.getInputLayer().addInputSynapse(memInp);
		
		Layer output = my_network.getOutputLayer();
		output.removeAllOutputs();

		DirectSynapse memOut = new DirectSynapse();
		output.addOutputSynapse(memOut);		
		my_monitor.setTotCicles(1);
		my_monitor.setTrainingPatterns(1);
		my_monitor.setLearning(false);
	}

	@Override
	public void netValidated(final NeuralValidationEvent the_validation_event) 
	{
		
	}

	
}
