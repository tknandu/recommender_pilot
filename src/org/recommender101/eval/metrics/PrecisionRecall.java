package org.recommender101.eval.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.RecommendationlistEvaluator;

/**
 * A class which implements precision and recall functions. Specific code is
 * implemented in the subclasses
 * 
 * @author dietmar
 * 
 */
public abstract class PrecisionRecall extends RecommendationlistEvaluator {

	/**
	 * remember the precision/recall so far
	 */
	double accumulatedValue = 0;

	/**
	 * Remember how often we have been called
	 */
	int counter = 0;

	/** Stores the type of the target set */
	evalTypes targetSetType = evalTypes.allrelevantintestset;

	// The execution modes
	public enum evalTypes {
		allrelevantintestset, allintestset, positioninrandomset
	};

	// To be used as a counter when measuring the random set procedure
	int additionalUsers = 0;

	// The number of additional items for the random evaluator
	int nbRandomElements = 500;

	/**
	 * What to count
	 */
	boolean considerOnlyItemsRatedByUser = false;

	@Override
	public abstract void addRecommendations(Integer user, List<Integer> list);

	@Override
	public void initialize() {
		String targetSet = getTargetSet();
		if (targetSet != null) {
			targetSetType = evalTypes.valueOf(targetSet.toLowerCase());
		}
		if (targetSetType == evalTypes.allrelevantintestset) {
			considerOnlyItemsRatedByUser = true;
		}
	}

	/**
	 * A helper function that determines the number of ratings above (or equals)
	 * a defined threshold
	 * 
	 * @param ratings
	 * @param avg
	 * @return the number of above-average rated items in the set
	 */
	int nbOftruePositives(Set<Rating> ratings) {
		int truePositives = 0;
		for (Rating r : ratings) {
			// if this is a hit, we count it
			if (isItemRelevant(r.item, r.user)) {
				truePositives++;
			}
		}
		return truePositives;

	}

	/**
	 * Return the overall precision or recall
	 */
	@Override
	public float getEvaluationResult() {
		float result = ((float) accumulatedValue / (float) counter);
		return result;
	}

	// =====================================================================================
	/**
	 * Setter for the random set evaluation procedure
	 * 
	 * @param n
	 */
	public void setNbRandomElements(String n) {
		this.nbRandomElements = Integer.parseInt(n);
	}

	/**
	 * A method to calculate recall according to the procedure by Cremonesi et
	 * al. Calculate the position of a liked item in a number of random methods
	 * 
	 * @param user
	 * @param rankedList
	 *            the ranked list of items. Will be filtered to obtain the
	 *            correct positioning
	 * @param calculatePrecision
	 *            set this to true to calculate precision (by dividing recall
	 *            through the list length
	 */
	public void addRecallForRandomSetProcedure(Integer user, boolean calculatePrecision, List<Integer> rankedList) {
		List<Integer> rankedListCopy = new ArrayList<Integer>(rankedList);
  	    System.out.println("Precision in random set to be determined");
		// Creating a list of non-rated items
		Set<Integer> nonRatedItems = new HashSet<Integer>();
		List<Integer> items = new ArrayList<Integer>(getTrainingDataModel().getItems());
		// Shuffling
		Collections.shuffle(items);
		int cnt = 0;
		float rating = -1;
		for (Integer item : items) {
			rating = getTestDataModel().getRating(user, item);
			if (rating == -1) {
				rating = getTrainingDataModel().getRating(user, item);
				// Check also for the blacklisted items
//				System.out.println("Forbidden ones: " + forbiddenRandomElements);
				if (rating == -1 && !forbiddenRandomElements.contains(item)) {
					cnt++;
					nonRatedItems.add(item);
					if (cnt >= nbRandomElements) {
						break;
					}
				}
			}
		}
		if (nonRatedItems.size() < nbRandomElements) {
			 System.err.println("Not enough unrated items for user " + user + ", skipping test");
			return;
		}
//		System.out.println("Nb of non-rated items: " + nonRatedItems.size());
//		System.out.println("\nGot a number of non rated items: " + nonRatedItems.size());
		

		// Now get the items in the test set the user considered relevant
		List<Integer> relevantItems = new ArrayList<Integer>();
		for (Rating r : getTestDataModel().getRatingsOfUser(user)) {
			if (isItemRelevant(r.item, r.user)) {
				relevantItems.add(r.item);
			}
		}
		System.out.println("Got a number of relevant items " + relevantItems);
//		System.out.println("User is " + user);
		
		// Remove all items from the ranked list which are not chosen as random
		// and are not part of the relevant ones
		List<Integer> itemsToRetain = new ArrayList<Integer>(relevantItems);
		itemsToRetain.addAll(nonRatedItems);
		
//		System.out.println("Items to retain " + itemsToRetain.size());
//		System.out.println("Number of all available items: " + rankedListCopy.size());
		rankedListCopy.retainAll(itemsToRetain);
//		System.out.println("Retained a number of items: " + rankedListCopy );

		// Now go through each relevant item and leave it as the only one in the
		// set to test
		List<Integer> relevantToRemove;
		List<Integer> finalItemList;
		for (Integer relevantOne : relevantItems) {
			
//			if (!rankedList.contains(relevantOne)) {
//				System.err.println(" ... OMG - it was never in my list of size " + rankedList.size() + " - item " + relevantOne);
//				System.out.println("Ranked list: " + rankedList);
//			}
			
			// Create a copy of the relevant and the ranked list
			relevantToRemove = new ArrayList<Integer>(relevantItems);
			
//			System.out.println("Relevant one: " + relevantOne);
//			System.out.println("To remove from the other list " + relevantToRemove);
			
			finalItemList = new ArrayList<Integer>(rankedListCopy);
//			System.out.println("Copy of rankedList (final): " + finalItemList);
			// remove the current one
			relevantToRemove.remove(relevantOne);
//			System.out.println("Removing: " + relevantToRemove);
			
			// Subtract all other relevant ones from the ranked list
			finalItemList.removeAll(relevantToRemove);
			// Now we got the ranked list which contains only one relevant item.
			// keep the top-n items
//			System.out.println("Checking item: " + relevantOne + " in long list: " + finalItemList);
//			if (!finalItemList.contains(relevantOne)) {
//				System.err.println("OMG, the item's not there!");
//			}

			finalItemList = finalItemList.subList(0, Math.min(getTopN(), finalItemList.size()));
			
//			System.out.println("top n: " + getTopN());
			// Let's see if the item is there
			boolean hit = finalItemList.contains(relevantOne);
//			System.out.println("Found a hit: " + hit );
			float recall = 0;
			float precision = 0;
			if (hit) {
				recall = 1;
				precision = recall / (getTopN());
			}
			float value = 0;
//			System.out.println("calculate precision: " + calculatePrecision + " precision = " + precision);
			if (calculatePrecision == true) {
				value = precision;
			} else {
				value = recall;
			}

//			System.out.println("Add precision/recall: " + precision);
			if (value != Float.NaN) {
				accumulatedValue += value;
				counter++;
			}
//			System.out.println("Accumulated: " + accumulatedValue);

		}
	}
	
	/**
	 * A black list when using the positioninrandomset setting.
	 * Can be needed in some situations
	 */
	public List<Integer> forbiddenRandomElements = new ArrayList<Integer>();
	
}


