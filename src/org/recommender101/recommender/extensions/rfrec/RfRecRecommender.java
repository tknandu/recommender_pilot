/** DJ **/
package org.recommender101.recommender.extensions.rfrec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Utilities101;

/**
 * Implements the weighted Rf-Rec scheme as proposed in 
 * Gedikli, F., Bagdat, F., Ge, M., Jannach, D.: 
 * RF-Rec: Fast and Accurate Computation of Recommendations based on Rating Frequencies, 
 * IEEE (CEC) 2011, Luxembourg, 2011, pp. 50-57.  
 * @author DJ
 *
 */
public class RfRecRecommender extends AbstractRecommender {
	
	
	/**
	 * Number of iterations for the gradient solver
	 */
	int iterations = 20;
	
	/**
	 * Gradient descent step size
	 */
	double gammaStepSize = 0.001;
	
	/**
	 * Regularization factor
	 */
	double lambdaForRegularization = 0.02;
	
	
	/**
	 * The step size
	 */
	
	double ratingStepSize = 1;
	
	
	/**
	 * Remember the possible rating values. We do not support half-star ratings so far
	 */
	byte[] possibleRatings;
	
	/**
	 * The average ratings of users
	 */
	Map<Integer, Float> userAverages;
	
	/**
	 * The average ratings of items
	 */
	Map<Integer, Float> itemAverages;
	
	/**
	 * The number of ratings per rating value per user
	 */
	Map<Integer, Map<Byte, Integer>> userRatingFrequencies;
	/**
	 * The number of ratings per rating value per item
	 */
	Map<Integer, Map<Byte, Integer>> itemRatingFrequencies;
	
  /** User weights learned by the gradient solver */
  Map< Integer, Double > userWeights;

  /** Item weights learned by the gradient solver. */
  private Map< Integer, Double > itemWeights;


  // =====================================================================================

  /**
   * Returns the rating prediction for a user/item pair
   */
	@Override
	public float predictRating(int user, int item) {
		
    float estimate = 3.50F;
    float enumeratorUser = 0;
    float denominatorUser = 0;
    float enumeratorItem = 0;
    float denominatorItem = 0;

	
    
//    System.out.println("Looking for: user " + user + " item " + item);
//    System.out.println("His rating frequencies  " + userRatingFrequencies.get(user));
//    System.out.println("The item frequencies: " + itemRatingFrequencies.get(item));
//    System.out.println("uAverages: " + userAverages.get(user));
//    System.out.println("iAverages: " + itemAverages.get(item));
    
    // Check if we have all the data
    if (userRatingFrequencies.get(user) != null && itemRatingFrequencies.get(item) != null &&
    		userAverages.get(user) != null && itemAverages.get(item) != null ) {
	    
	    // Go through all the possible rating values
	    for ( int r = 0; r < possibleRatings.length; ++r ) {
	      byte ratingValue = possibleRatings[ r ];
	      // user component
	      int tmpUser = 0;
	      Integer frequencyInt = userRatingFrequencies.get(user).get(ratingValue);
	      int frequency = 0;
	      if (frequencyInt != null) {
	      	frequency = frequencyInt;
	      }
	      tmpUser = frequency + 1 + isAvgRating( userAverages.get(user), ratingValue );
	      enumeratorUser += tmpUser * ratingValue;
	      denominatorUser += tmpUser;
	
	      // item component
	      int tmpItem = 0;
	      frequency = 0;
	      frequencyInt = itemRatingFrequencies.get(item).get(ratingValue);
	      if (frequencyInt != null) {
	      	frequency = frequencyInt;
	      }
	      tmpItem = frequency + 1 + isAvgRating( itemAverages.get(item), ratingValue );
	      enumeratorItem += tmpItem * ratingValue;
	      denominatorItem += tmpItem;
	    }

	    double w_u = userWeights.get( user );
	    double w_i = itemWeights.get( item );
//	    System.out.println( "Using these weights w_u: " + w_u + ", w_i: " + w_i );
	    float pred_ui_user = enumeratorUser / denominatorUser;
	    float pred_ui_item = enumeratorItem / denominatorItem;
	    estimate = (float) w_u * pred_ui_user + (float) w_i * pred_ui_item;
	    
    }	
    
    else {
    	// if the user or item weren't known in the training phase...
    	if (userRatingFrequencies.get(user) == null || userAverages.get(user) == null) {
    		Float iavg = itemAverages.get(item);  
    		if (iavg != null) {
    			return iavg;
    		}
    		else {
    			// Some heuristic -> a bit above the average rating
    			return Math.round(dataModel.getMaxRatingValue() - dataModel.getMinRatingValue()) + 1;
    		}
    	}
    	if (itemRatingFrequencies.get(item) == null || itemAverages.get(item) == null) {
    		Float uavg = userAverages.get(item);  
    		if (uavg != null) {
    			return uavg;
    		}
    		else {
    			// Some heuristic -> a bit above the average rating
    			return Math.round(dataModel.getMaxRatingValue() - dataModel.getMinRatingValue()) + 1;
    		}
    	}
    }
    return (float) estimate;
	}

	// =====================================================================================

	/**
	 * Rank items according to their predicted rating
	 * TODO: Take popularity into account..
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		// Use the standard method based on the rating prediction
		return recommendItemsByRatingPrediction(user);
	}

	// =====================================================================================

	@Override
	public void init() throws Exception {
		
		// Prepare the rating steps
		int nbOfSteps = 1 + (dataModel.getMaxRatingValue() - dataModel.getMinRatingValue());
		possibleRatings = new byte[nbOfSteps];
		byte ratingValue = (byte)  dataModel.getMinRatingValue();
		// fill the array of possible values
		int i = 0;
		while (ratingValue <= dataModel.getMaxRatingValue()) {
			possibleRatings[i] = ratingValue;
			ratingValue++;
			i++;
		}
		// Calculate the average ratings
		userAverages = dataModel.getUserAverageRatings();
		itemAverages = Utilities101.getItemAverageRatings(dataModel.getRatings());

		// Calculate the frequencies.
		// Users..
		userRatingFrequencies = new HashMap<Integer, Map<Byte,Integer>>();
		for (Integer user : dataModel.getUsers()) {
			Set<Rating> ratings = dataModel.getRatingsPerUser().get(user);
			HashMap<Byte, Integer> newEntry = new HashMap<Byte, Integer>();
			userRatingFrequencies.put(user, newEntry);
			for (Rating r : ratings) {
				Utilities101.incrementMapValue(newEntry, r.rating);
			}
		}
		
		// Items
		itemRatingFrequencies = new HashMap<Integer, Map<Byte,Integer>>();
		// Iterate over all ratings 
		Set<Rating> ratings = dataModel.getRatings();
		for (Rating r : ratings) {
			// Check if we have an entry in the frequencies map
			Map<Byte, Integer> itemMap = itemRatingFrequencies.get(r.item);
			if (itemMap == null) {
				itemMap = new HashMap<Byte, Integer>();
				itemRatingFrequencies.put(r.item, itemMap);
			}
			// now add the corresponding frequency
			Utilities101.incrementMapValue(itemMap, r.rating);
		}
//		System.out.println("user freqs: " + userRatingFrequencies);
//		System.out.println("item freqs: " + itemRatingFrequencies);

		
		initializeUserAndItemWeights();
//		System.out.println(ratingFrequencies);
		gradientSolver(i, gammaStepSize, this.lambdaForRegularization, dataModel);
	}
	
	// =====================================================================================

	/**
	 * Sets start values for users (~0.6) and items (~0.4) 
	 */
	void initializeUserAndItemWeights() {
		userWeights = new HashMap<Integer, Double>();
		itemWeights = new HashMap<Integer, Double>();
		
		for (Integer user : dataModel.getUsers()) {
			userWeights.put(user, 0.6 + Math.random() * 0.01);
		}
		for (Integer item : dataModel.getItems()) {
			itemWeights.put(item, 0.4 + Math.random() * 0.01);
		}
	}
	
	
	/**
	 * Returns 1 if the rating is similar to the rounded average value
	 * @param avg the average
	 * @param rating the rating
	 * @return 1 when the values are equal
	 */
  private int isAvgRating( double avg, byte rating )  {
    if ( Math.round( avg ) == rating ) {
      return 1;
    }
    else {
      return 0;
    }
  }
  
  // =====================================================================================

  /**
   * Learn the parameter on the train data.
   * 
   * @param iteration Number of times the steps are repeated.
   * @param gammaStepSize Amount of step size.
   * @param lambdaForRegulation Amount of regulation to avoid overfitting.
   * @param trainData The given train data model.
   */

  public void gradientSolver( int iteration, double gammaStepSize,
                              double lambdaForRegulation, DataModel trainData ) 
  {
//    System.out.println( "Estimating parameters..." );

    /** Preference of user u for item i. */
    double mRui;
    /** Predicted preference for user u for item i. */
    double mPui;
    /** Prediction error for the Preference of user u for item i. */
    double mEui;

    for ( int i = 0; i < iteration; i++ ) {
//      System.out.println( "Iteration " + ( i + 1 ) );
      
      Set<Rating> ratings = dataModel.getRatings();
      // Iterate over the ratings
      for (Rating r : ratings) {
      	float estimate = predictRating(r.user, r.item);
        mPui = estimate;
        //True rating from user u for item i
        mRui = dataModel.getRating(r.user, r.item);
//        System.out.println("REAL RATING: " + mRui);
        //Prediction-Error for user u and item i.
        mEui = mRui - mPui;
        
//        System.out.println("Error: " + mEui);

        // Gradient-Step on user weights.
//        System.out.println("gamma: " + gammaStepSize + " lambda: " + lambdaForRegulation + " userweight: " + userWeights.get(r.user));
        
        
        double userWeight = userWeights.get(r.user) + gammaStepSize * ( mEui - lambdaForRegulation * userWeights.get(r.user) );
        userWeights.put(r.user, userWeight );
        // Gradient-Step on item weights.
        double itemWeight = itemWeights.get(r.item) + gammaStepSize * ( mEui - lambdaForRegulation * itemWeights.get(r.item));
        itemWeights.put(r.item, itemWeight );
        
      }
    }
  }

  
	// =====================================================================================

	/**
	 * A setter for the factory
	 * @param it
	 */
	public void setIterations(String it) {
		this.iterations = Integer.parseInt(it);
	}
	
	// =====================================================================================

	/**
	 * Set gamma as a string
	 * @param gamma
	 */
	public void setGamma(String gamma) {
		this.gammaStepSize = Double.parseDouble(gamma);
	}
	
	/**
	 * Set lambda from a string
	 * @param lambda
	 */
	public void setLambda(String lambda) {
		this.lambdaForRegularization = Double.parseDouble(lambda);
	}


	@Override
	public int getDurationEstimate() {
		return 3;
	}	
	
}
