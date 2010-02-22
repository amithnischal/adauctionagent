/** Contains methods to normalize a value */
public final class Normalizer 
{
	/**
	 * Scales a parameter by linear interpolation
	 * @param the_x_value The input raw value
	 * @param the_x_max
	 * @param the_y_min
	 * @param the_y_max
	 * @return The input linearly scaled value.
	 */
	public static double scaleLinear(final double the_x_value, final double the_x_min,
			final double the_x_max, final double the_y_min, final double the_y_max)
	{
		return the_y_min + (the_y_max - the_y_min)*
			(the_x_value - the_x_min)/(the_x_max - the_x_min);
	}
	
	/**
	 * Scales a value using z-score normalization.
	 * @param the_value The value to be scaled
	 * @param the_mean The mean of the all the values of this parameter.
	 * @param the_SD The Standard Deviation of the values of the parameter.
	 * @return The value scaled to a z-score.
	 */
	public static double scaleZScore(final double the_value, final double the_mean, final double the_SD)
	{
		return (the_value - the_mean)/the_SD; 
	}
	
}
