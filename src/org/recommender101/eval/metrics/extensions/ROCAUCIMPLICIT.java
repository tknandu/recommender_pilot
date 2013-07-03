package org.recommender101.eval.metrics.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.RecommendationlistEvaluator;

/**
 * AUC under the ROC-Curve for implicit Feedback 
 * Found in http://www.ismll.uni-hildesheim.de/pub/pdfs/Rendle_et_al2009-
 * Bayesian_Personalized_Ranking.pdf
 * 
 * Code based on MyMediaLite http://www.ismll.uni-hildesheim.de/mymedialite/
 * 
 * 
 * @author MW
 * 
 */
public class ROCAUCIMPLICIT extends RecommendationlistEvaluator {

	/**
	 * A private attribute which holds the number of users for which the AUC has
	 * been calculated
	 */
	private int count;

	/**
	 * The sum all previously calculated AUC values.
	 */
	private double aucSum;

	/**
	 * Initialize the metric
	 */
	@Override
	public void initialize() {
		this.count = 0;
		this.aucSum = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> rankedItems) {
		// Check if list is null or empty
		if (rankedItems == null || rankedItems.size() == 0)
			return;		
	
		List<Integer> testItems = new ArrayList<Integer>();
		
		Set<Rating> testRatings = getTestDataModel().getRatingsOfUser(user);		

		for(Rating r : testRatings)
		{
			if (isItemRelevant(r.item, r.user)) 
				testItems.add(r.item);
		}

		//Number of all evaluation-pairs	
		int numberEvalpairs = (rankedItems.size() - testRatings.size()) * testRatings.size();
		
		
		if(testItems.size() == 0)
		{
			this.aucSum += 0;
		}
		
		else
		{	   
			count++;
		    if (numberEvalpairs == 0)
		    {
		    	this.aucSum += 0.5;
		    	return;
		    }
	
		    int numberCorrectpairs = 0;
		    int numberCorretcomparisons = 0;
		    
		    //Counting all the correct comparisons between items of the testdata and ones that neither in the testdata nor trainigsdata
		    for (int item_id : rankedItems)
		    {
		      if (!testItems.contains(item_id))
		        numberCorrectpairs += numberCorretcomparisons;
		      else
		    	  numberCorretcomparisons++;
		    }
		    this.aucSum += (double) numberCorrectpairs / numberEvalpairs;
		    
		}
	}

	/**
	 * Returns the arithmetic mean of all calculated AUC values.
	 */
	@Override
	public float getEvaluationResult() {

		return (float) this.aucSum/count;
	}

	

}