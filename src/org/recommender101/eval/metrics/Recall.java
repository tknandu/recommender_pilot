package org.recommender101.eval.metrics;

import java.util.List;
import java.util.Set;

import org.recommender101.data.Rating;

/**
 * Implements variants of the recall method
 * @author timkraemer, dietmar
 *
 */
public class Recall extends PrecisionRecall {


	/**
	 * Determines the recall value
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		// Special treatment required for position in random set
		// Standard behaviour first.
		if (targetSetType != PrecisionRecall.evalTypes.positioninrandomset) {
			// If there are no such items for the user, return
			Set<Rating> ratingsOfUser = getTestDataModel().getRatingsPerUser().get(user);
			int truePositives = nbOftruePositives(ratingsOfUser);
			// Cannot measure recall
			if (truePositives == 0) {
				return;
			}
			int hitCounter = 0;
			int recCounter = 0;
			
			// Go through the list of recommendations while filtering out irrelevant items
			for (Integer item: list) {
				// If we have a rating for the item in the test set, add it to the true recommendations

				if (considerOnlyItemsRatedByUser) {
					if (getTestDataModel().getRating(user, item) != -1) {
						// OK, we recommend it
						recCounter++;
						
						// if this is a hit, we count it
						if (isItemRelevant(item, user)) {
							hitCounter++;
						}
					}				
				}
				else {
					// Always add the recommendation
					recCounter++;
					
					// if this is a hit, we count it
					if (isItemRelevant(item, user)) {
						hitCounter++;
					}
				}
				
				// Exit loop if TopN has been reached
				if (recCounter >= getTopN()) {
					break;
				}
			}

			// Calculate the recall value
			double recall = ((double)hitCounter) / ((double) truePositives);
			
			if (!Double.isNaN(recall)) {
				accumulatedValue += recall;
				counter++;
			}
			
		}
		else {
			// Use the procedure from Cremonesi et al and measure the 
			// position of each relevant item in some random list
			addRecallForRandomSetProcedure(user, false, list);
		}

	}

}
