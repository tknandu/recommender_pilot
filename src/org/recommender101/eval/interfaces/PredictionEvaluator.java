/** DJ **/
package org.recommender101.eval.interfaces;

import org.recommender101.data.Rating;


/**
 * An abstract for the implementation of prediction evaluation metrics
 * @author DJ
 *
 */
public abstract class PredictionEvaluator extends Evaluator {


	/**
	 * This method is called for every prediction and can be used to accumulate things
	 * @param user the user id
	 * @param item the item id
	 * @param prediction the predicted value (or Float.NaN in case the recommender could
	 * not make a prediction)
	 * 
	 */
	public abstract void addTestPrediction(Rating r, float prediction); 
	
	/**
	 * Returns the final number
	 * @return
	 */
	public abstract float getPredictionAccuracy();

}
