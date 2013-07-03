package org.recommender101.eval.metrics.extensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.recommender101.eval.interfaces.RecommendationlistEvaluator;
import org.recommender101.tools.Utilities101;

/**
 * Calculates the average popularity of the recommended items 
 * @author dietmar
 *
 */
public class AverageItemPopularityOfRecommendations extends
		RecommendationlistEvaluator {

	
	/**
	 * Remember the item popularities in the training set
	 */
	Map<Integer, Float> itemPopularities = new HashMap<Integer, Float>();
	
	
	/**
	 * Should we use the median?
	 */
	boolean useMedian = false;
	
	/**
	 * To collect all the elements during the list construction
	 */
	Map<Integer, Double> popularityPerUser = new HashMap<Integer, Double>();
	
	public AverageItemPopularityOfRecommendations() {
		super();
	}

	/**
	 * Calculate the average popularities
	 */
	@Override
	public void initialize() {
		super.initialize();
		if (this.useAverageRating) {
			this.itemPopularities = Utilities101.getItemAverageRatings(getTrainingDataModel().getRatings());
		}
		else {
			Map<Integer, Integer> ratingCounts = Utilities101.calculateRatingsPerItem(getTrainingDataModel());
			for (Integer item : ratingCounts.keySet()) {
				this.itemPopularities.put(item,(float) ratingCounts.get(item));
			}
		}
	}

	/*
	 * What we should count: All recommendations or all relevant ones
	 */
	enum mode {all,onlyrelevant};

	// default mode: count all
	mode themode = mode.all;

	// Calculage popularity based on average rating
	boolean useAverageRating = false;
	
	
	/**
	 * A setter for the auto-instantiator
	 * @param m the mode
	 */
	public void setMode(String m) {
		themode = mode.valueOf(m.toLowerCase());
	}

	/**
	 * Calculates the average popularity values up to a certain list length
	 *
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		List<Integer> sublist = new ArrayList<Integer>(); 
		if (themode == mode.all) {
			sublist = list.subList(0, Math.min(list.size(),topN));
		}
		// Get the right sublist
		else {
			int found = 0;
			for (Integer item: list) {
				byte rating = getTestDataModel().getRating(user, item);
				if (rating != -1 && isItemRelevant(item, user)) {
					sublist.add(item);
					found++;
				}
				if (found >= topN) {
					break;
				}
			}
//			System.out.println("Found relevant items: " + sublist.size());
		}
		
		if (this.useMedian == false) {
			
			// sum everything up
			int cnt = 0;
			int accum = 0;
			for (Integer item : sublist) {
				Float pop = this.itemPopularities.get(item);
				if (pop != null) {
					accum += pop;
				}
				cnt++;
			}
			
			if (cnt != 0) {
				double avgPop  = (double) accum / (double) cnt;
				this.popularityPerUser.put(user, avgPop);
			}
		}
		else {
			if (sublist != null && sublist.size() > 0) {
				
				// Create a hash map with the popularity values 
				Map<Integer, Float> popularities = new HashMap<Integer, Float>();
				Float pop = null;
				for (Integer item : sublist) {
					pop = this.itemPopularities.get(item);
					if (pop == null) {
						pop = 0.0f;
					}
					popularities.put(item,pop);
				}
				// Sort the list and get the middle element
				popularities = Utilities101.sortByValueDescending(popularities);
				int pos = sublist.size() / 2;
				List<Float> values = new ArrayList<Float>(popularities.values());
				// Sort in ascending order
				Collections.reverse(values);
				Float median = values.get(pos);
	//			System.out.println("values: " + values);
	//			System.out.println("median: " + median);
				
				if (median != null) {
					this.popularityPerUser.put(user, (double) median);
				}
			}
		}
		
	}

	/**
	 * Calculate the overall average value
	 */
	@Override
	public float getEvaluationResult() {
		double accum = 0;
		Double d = null;
		int cnt = 0;
		for (Integer user: this.popularityPerUser.keySet()) {
			d = this.popularityPerUser.get(user);
			if (d != null) {
				accum += d;
				cnt++;
			}
		}
		if (cnt == 0) {
			return Float.NaN;
		}
		return (float) (accum / (double) cnt);
	}
	
	/**
	 * Should we use the average rating as popularity indicator
	 * @param s 
	 */
	public void setUseAverageRating(String s) {
		this.useAverageRating = Boolean.parseBoolean(s);
	}
	
	
	/**
	 * Should the median value be taken instead of the mean
	 * @param b
	 */
	public void setUseMedian(String b) {
		this.useMedian = Boolean.parseBoolean(b);
	}
	

}
