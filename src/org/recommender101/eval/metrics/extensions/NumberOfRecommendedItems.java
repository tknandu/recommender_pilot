/** DJ **/
package org.recommender101.eval.metrics.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.eval.interfaces.RecommendationlistEvaluator;
import org.recommender101.tools.Utilities101;

/**
 * This is a simple demo class which calculates how many different *relevant* items
 * have been recommended to all the users in the test set
 * @author DJ
 *
 */
public class NumberOfRecommendedItems extends RecommendationlistEvaluator {

	/**
	 * Calculate the user averages
	 */
	@Override
	public void initialize() {
		userAverages = getTestDataModel().getUserAverageRatings();
		itemAverages = Utilities101.getItemAverageRatings(getTrainingDataModel().getRatings());

	}
	
	Map<Integer, Float> userAverages;
	Map<Integer, Float> itemAverages;
	
	/*
	 * Remember all recommended items in a set
	 */
	Set<Integer> recommendedItems = new HashSet<Integer>();
	
	/*
	 * What we should count: All recommendations or all relevant ones
	 */
	enum mode {all,onlyrelevant};

	// default mode: count all
	mode themode = mode.all;
	
	
	
	// We calculate the number of recommended items
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		// Simply add all elements to the set of recommended items
		if (themode == mode.all) {
			int max = Math.min(topN, list.size());
			List<Integer> topNList = list.subList(0, max);
//			System.out.println("Adding");
//			System.out.println(topNList);
			recommendedItems.addAll(topNList);
			
//			for (Integer item : recommendedItems) {
//				System.out.println("AVG: " + this.itemAverages.get(item));
//				System.out.println("Rating in test: " + getTestDataModel().getRating(user, item));
//			}
			
			
			
		}
		// Only consider the relevant ones
		else {
			int cnt = 0;
			for (Integer item : list) {
				byte r = -1;
				r = getTestDataModel().getRating(user, item);
				if (r != -1 && isItemRelevant(item, user)) {
					recommendedItems.add(item);
					cnt++;
				}
				if (cnt >= topN) {
					break;
				}
			}
		}
	}

	/**
	 * Returns the number of different items recommended by the algorithm
	 */
	@Override
	public float getEvaluationResult() {
		return recommendedItems.size();
	}

	/**
	 * Set the list length to analyze
	 */
	public void setTopN(String n) {
		this.topN = Integer.parseInt(n);
	}
	
	
	/**
	 * A setter for the auto-instantiator
	 * @param m the mode
	 */
	public void setMode(String m) {
		themode = mode.valueOf(m.toLowerCase());
	}
	
	// =====================================================================================


	
}
