package org.recommender101.eval.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.RecommendationlistEvaluator;

/**
 * Calculates the nDCG (normalized discounted cumulative gain) value.
 * 
 * @author timkraemer
 * 
 */
public class NDCG extends RecommendationlistEvaluator {

	private double accumulatedNDCGValue = 0.0;
	private int count = 0;
	
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		int depth = Math.min(getTopN(), list.size());
		
		// Calculate the DCG value
		double dcg = 0.0;
		int loopCount = 0;
		for (int item:list)
		{
			if (getTestDataModel().getRating(user, item) != -1) {
				double a = Math.pow(2, getTestDataModel().getRating(user, item));
				// I've used "2+i" below instead of "1+i" because the index is
				// zero-based.
				double b = Math.log(2 + loopCount) / Math.log(2);
				//System.out.println("a: "+a+", b: "+b+", a/b: "+a/b);
				loopCount++;				
				dcg += a / b;
			}
			
			if (loopCount >= depth)
				break;
		}
		
		// Get the List of ratings and sort them by relevance (rating)
		ArrayList<Rating> ratings = new ArrayList<Rating>(getTestDataModel().getRatingsOfUser(user));
		Collections.sort(ratings, new Comparator<Rating>() {

			@Override
			public int compare(Rating o1, Rating o2) {
				return -(o1.rating - o2.rating);
			}

		});

		//System.out.println("First rating: " + ratings.get(0).rating
		//		+ ", Last rating: " + ratings.get(ratings.size() - 1).rating);

		// Calculate "ideal DCG"
		double ideal_dcg = 0.0;
		depth = loopCount;

		for (int i = 0; i < depth; i++) {
			double a = Math.pow(2,ratings.get(i).rating);
			// I've used "2+i" below instead of "1+i" because the index is
			// zero-based.
			double b = Math.log(2 + i) / Math.log(2);
			//System.out.println("a2: "+a+", b2: "+b+", a/b: "+a/b);
			ideal_dcg += a / b;
		}

		double nDCG = dcg/ideal_dcg;
		
		//System.out.println("DCG: " + dcg + ", ideal DCG: " + ideal_dcg+", nDCG: "+nDCG);
		
		// DJ: check for Double.NaN
		if (!Double.isNaN(nDCG)) {
			
			count++;
			accumulatedNDCGValue += nDCG;
		}
	}

	@Override
	public float getEvaluationResult() {
		return ((float)accumulatedNDCGValue)/((float)count);
	}

}
