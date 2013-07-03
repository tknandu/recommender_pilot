/** DJ **/
package org.recommender101.eval.metrics;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.PredictionEvaluator;

/**
 * A method that counts for how many ratings in the test set the algorithm could
 * make a prediction
 * @author DJ
 *
 */
public class PredictionCoverage extends PredictionEvaluator {

	/**
	 * A counter for the total number of ratings
	 */
	int totalRatings = 0;
	
	/**
	 * A counter for the predicted one
	 */
	int predictedRatings = 0;
	
	/**
	 * Collect all the predictions and check for Float.isNaN
	 */
	@Override
	public void addTestPrediction(Rating r, float prediction) {
		if (!Float.isNaN(prediction)) {
			predictedRatings++;
		}
		totalRatings++;

	}

	/**
	 * Calculate the percentage of given ratings
	 */
	@Override
	public float getPredictionAccuracy() {
		return predictedRatings/ (float) totalRatings;
	}

}
