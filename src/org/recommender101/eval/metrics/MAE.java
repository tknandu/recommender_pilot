/** DJ **/
package org.recommender101.eval.metrics;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.PredictionEvaluator;

/**
 * Calculates the MAE
 * @author DJ
 *
 */
public class MAE extends PredictionEvaluator {

	/**
	 * We calculate the errors
	 */
	float errorAccumulator;
	
	/**
	 * The number of predictions
	 */
	int predictionCount;
	
	// =====================================================================================

	@Override
	/**
	 * Record a new prediction and determine the deviation from the true value
	 */
	public void addTestPrediction(Rating r, float prediction) {
		if (!Float.isNaN(prediction)) {
			Byte rating = r.rating;
			if (rating != null) {
				float error = Math.abs(rating - prediction);
				errorAccumulator += error;
				predictionCount++;
			}
		}
		
	}

	// =====================================================================================

	/**
	 * Return the mean error at the end of the process
	 */
	@Override
	public float getPredictionAccuracy() {
//		System.out.println("Returning prediction accuracy - accumulator: " + errorAccumulator );
//		System.out.println("prediction count: " +  (float) predictionCount);
		return errorAccumulator / (float) predictionCount;
	}

	/**
	 * String rep for eval.
	 */
	public String toString() {
		return "MAE";
	}

	
}
