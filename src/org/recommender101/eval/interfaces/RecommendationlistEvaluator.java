/** DJ **/
package org.recommender101.eval.interfaces;

import java.util.List;

import org.recommender101.eval.impl.Recommender101Impl;
import org.recommender101.gui.annotations.R101Class;
import org.recommender101.gui.annotations.R101Setting;
import org.recommender101.gui.annotations.R101Setting.SettingsType;
import org.recommender101.tools.Utilities101;


/**
 * An abstract class that can be used to evaluate recommendation lists
 * @author DJ
 *
 */
@R101Class
public abstract class RecommendationlistEvaluator extends Evaluator {
	
	/**
	 * Add a recommendation list for a user. The data is accumulated internally 
	 * @param user the user for whom the recommendation is made
	 * @param list the list of recommended items. Can also be null or empty
	 */
	public abstract void addRecommendations(Integer user, List<Integer> list);
	
	/**
	 * Calculates and returns the result at the end.
	 * @return
	 */
	public abstract float getEvaluationResult();
	
	/**
	 * Set the number of items to be retrieved in the evaluation
	 * @param n
	 */
	@R101Setting(displayName="Top N", name="topN", minValue=0, type=SettingsType.INTEGER, description="The top N value for this metric", defaultValue="10")
	public void setTopN(String n) {
		topN = Integer.parseInt(n);
	};
	
	/** Returns the number of items used */
	public int getTopN() {
		return topN;
	};
	

	/**
	 * How do we calculate precision, default is only relevant items in test set
	 */
	String targetSet = null;
	
	
	/**
	 * Returns the target set for the calculations
	 * @return
	 */
	public String getTargetSet() {
		return targetSet;
	}

	/**
	 * Setter for the target set used by class instantiator
	 * @param targetSet
	 */
	@R101Setting(name="targetSet", displayName="Target set", type=SettingsType.ARRAY, 
			description="The target set", values={"allrelevantintestset", "allintestset", "positioninrandomset"})
	public void setTargetSet(String targetSet) {
		this.targetSet = targetSet;
	}


	/**
	 * How long is the topN list
	 */
	public int topN = Recommender101Impl.TOP_N;
		
//=====================================================================================
	/**
	 * Determines, if an item is relevant for the user or not. An item is relevant when
	 * it is above a defined threshold, which is either the user's average or (in case
	 * the parameter is set) if it is x percent above the user's average.
	 * Alternatively, if the minRatingForRelevance parameter is set, this is used as 
	 * threshold
	 * @param item the item id
	 * @param user the user id
	 * @return true if the item is relevant
	 */
	public boolean isItemRelevant(int item, int user) {
		return Utilities101.isItemRelevant(item, user, getTestDataModel());
	}
		
//		
//		boolean result = false;
//		
//		if (Recommender101Impl.PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE > 0) {
//			// a variant
//			byte rating = getTestDataModel().getRating(user, item);
////			System.out.println("Rating in testset was: " + rating);
////			System.out.println("min relevance: " + Recommender101Impl.PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE);
//			if (rating >= Recommender101Impl.PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE) {
//				return true;
//			}
//			else {
//				return false;
//			}
//		}
//		// Otherwise -> use some percentage threshold
//		float userAvg = getTestDataModel().getUserAverageRating(user);
//		byte rating = getTestDataModel().getRating(user, item);
//		double threshold = userAvg;
//		
//		if (Recommender101Impl.PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE > 0) {
//			threshold = threshold + (threshold * (Recommender101Impl.PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE/(double) 100)); 
//		}
//		if (rating >= threshold) {
//			result = true;
//		}
//		return result;
//	}
	
}
