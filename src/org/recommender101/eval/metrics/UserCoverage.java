/** DJ **/
package org.recommender101.eval.metrics;

import java.util.List;

import org.recommender101.eval.interfaces.RecommendationlistEvaluator;
/**
 * Calculates the fraction of users in a recommendation list evaluation scenario
 * for which a recommendation was possible
 * @author DJ
 *
 */
public class UserCoverage extends RecommendationlistEvaluator {

	
	/**
	 * A counter for the total number of ratings
	 */
	int totalUsers = 0;
	
	/**
	 * A counter for the predicted one
	 */
	int usersWithPredictions = 0;
	
	/**
	 * Counts the predictions and the non-empty recommendations
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		if (list != null && list.size() > 0) {
			usersWithPredictions++;
		}
		totalUsers++;

	}

	/**
	 * Returns the coverage metrics
	 */
	@Override
	public float getEvaluationResult() {
		if (usersWithPredictions > 0) {
			return usersWithPredictions / (float) totalUsers ;
		}
		else {
			return 0;
		}
	}

}
