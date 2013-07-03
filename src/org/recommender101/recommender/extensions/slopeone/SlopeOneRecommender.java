/** DJ **/
package org.recommender101.recommender.extensions.slopeone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;

/**
 * A weighted slope one recommender. Uses the original implementation of Daniel Lemire
 * (lemire.me/fr/documents/publications/SlopeOne.java)
 * @author DJ
 *
 */
public class SlopeOneRecommender extends AbstractRecommender {

	/**
	 * Holds item to item differences
	 */
	Map<Integer,Map<Integer,Float>> mDiffMatrix;
	/**
	 * The item-item-frequency matrix
	 */
  Map<Integer,Map<Integer,Integer>> mFreqMatrix;
  

  /**
   * Based on Lemire's predict function.
   * Returns Float.NaN in case no prediction is possible
   */
	@Override
	public float predictRating(int user, int item) {
//		System.out.println("Predicting one");
		float prediction = 0;
		int frequency = 0;
  	// Go through all items the user has rated
  	for (Rating rj : dataModel.getRatingsOfUser(user)) {
  		// get the differences from the rating matrix
			Map<Integer, Float> diffsForItem = mDiffMatrix.get(item);
			if (diffsForItem != null) {
				Float avgDiff = diffsForItem.get(rj.item);
				if (avgDiff != null) {
					int frq = mFreqMatrix.get(item).get(rj.item).intValue();
				
					float newval = (avgDiff + rj.rating ) * frq;
					prediction += newval;
					frequency += mFreqMatrix.get(item).get(rj.item).intValue();
				}
			}
  	}
  	if (frequency > 0) {
  		prediction = prediction / frequency;
  	}
  	
  	// Default behavior
  	if (prediction == 0) {
  		return Float.NaN;
  	}
//  	System.out.println("Final prediction: " + prediction);
		return prediction;
	}


	/**
	 * Use the default strategy
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		return super.recommendItemsByRatingPrediction(user);
	}

	/**
	 * Pre-processes the data and calculates differences
	 */
	@Override
	public void init() throws Exception {

		buildDiffMatrix();    

	}



	/**
	 * As taken from Lemire's code: Calculates the rating differences
	 */
  public void buildDiffMatrix() {
    mDiffMatrix = new HashMap<Integer,Map<Integer,Float>>();
    mFreqMatrix = new HashMap<Integer,Map<Integer,Integer>>();
    // iterate through all ratings
    for(Integer user : dataModel.getUsers()) {
      // then iterate through user data
    	
      for(Rating rating1 : dataModel.getRatingsOfUser(user)) {
        if(!mDiffMatrix.containsKey(rating1.item)) {
          mDiffMatrix.put(rating1.item, new HashMap<Integer,Float>());
          mFreqMatrix.put(rating1.item, new HashMap<Integer,Integer>());
        }
        for(Rating rating2: dataModel.getRatingsOfUser(user)) {
          int oldcount = 0;
          if(mFreqMatrix.get(rating1.item).containsKey(rating2.item)) {
            oldcount = mFreqMatrix.get(rating1.item).get(rating2.item).intValue();
          }
          float olddiff = 0.0f;
          if(mDiffMatrix.get(rating1.item).containsKey(rating2.item)) {
            olddiff = mDiffMatrix.get(rating1.item).get(rating2.item).floatValue();
          }
          float observeddiff = rating1.rating - rating2.rating;
          mFreqMatrix.get(rating1.item).put(rating2.item,oldcount + 1);
          mDiffMatrix.get(rating1.item).put(rating2.item,olddiff+observeddiff);          
        }
      }
    }
    for (Integer j : mDiffMatrix.keySet()) {
      for (Integer i : mDiffMatrix.get(j).keySet()) {
        float oldvalue = mDiffMatrix.get(j).get(i).floatValue();
        int count = mFreqMatrix.get(j).get(i).intValue();
        mDiffMatrix.get(j).put(i,oldvalue/count);
      }
    }
  }
  
   
}



