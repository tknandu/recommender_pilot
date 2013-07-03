/** DJ **/
package org.recommender101.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.recommender101.tools.DataDensityTool;
import org.recommender101.tools.Debug;

/**
 * An abstract class for data splitting, e.g. for cross validation
 * @author DJ
 *
 */
public class DefaultDataSplitter extends DataSplitter {
	
	/**
	 * A min-ratings per user constraint for the test set 
	 */
	int minRatingsPerUser = -1;
	
	/**
	 * A min-ratings per item constraint for the test set
	 */
	int minRatingsPerItem = -1;
	
	
	/**
	 * A handle to the condensed splits
	 */
	public List<Set<Rating>> smallSplits = null;
	/**
	 * Accepts the min-ratings-per-user / min-ratings-per-item constraint
	 * @param s the lower bound in the form of minPerUser/minPerItem
	 */
	public void setMinRatingsConstraintForTestset(String s) {
		String[] tokens = s.split("/");
		this.minRatingsPerUser = Integer.parseInt(tokens[0]);
		this.minRatingsPerItem = Integer.parseInt(tokens[1]);
	}

	// =====================================================================================

	/**
	 * A method that splits the data randomly in n folds
	 * @param n the number of folds
	 * @param dataModel the data model containing the original data.
	 * @return the list
	 */
	public List<Set<Rating>> splitData (DataModel dataModel) throws Exception {
		
		List<Set<Rating>> result = new ArrayList<Set<Rating>> ();
		// If only a subset of users and items should be considered in the test set
		if (this.minRatingsPerUser == -1) {
			// Default behavior
			result = createNFolds(nbFolds, dataModel);
			return result;
		}
		else {
			// create a copy of the data model and remove things. Protocol is slightly different
			// test data is only taken from the condensed data set.

			// Step 1) Condense the data
			DataModel copiedDM = new DataModel(dataModel);
			System.out.println("Copied the data model..");
			DataDensityTool.applyConstraints(this.minRatingsPerUser, this.minRatingsPerItem, copiedDM);
			Debug.log("Reduced the data model to " + this.minRatingsPerUser + "/" + this.minRatingsPerItem);
			Debug.log(copiedDM.getRatings().size() + " ratings remain");
			if (copiedDM.getRatings().size() == 0) {
				System.err.println("[FATAL] No more test data - exiting");
				System.exit(1);
			}
			
			
			// Step 2) Split the condensed data into n folds and remember them for later user
			smallSplits = new ArrayList<Set<Rating>> ();
			smallSplits = createNFolds(nbFolds, copiedDM);
			
			// Step 3) Return only one large split with all the data we have
			// must be handled by cross validation runner later on
			result.add(dataModel.getRatings());
			
			// use implicit ratings in the training set
			if (copiedDM.getImplicitRatings().size()>0){
				result.get(0).addAll(copiedDM.getImplicitRatings());
			}
			
			return result;
		}
	} 
	
	/**
	 * A method that splits the data in n folds
	 * @param theratings a list of ratings
	 * @param nbFolds
	 * @return
	 */
	protected List<Set<Rating>> createNFolds(int nbFolds, DataModel dataModel) {
		List<Set<Rating>> result = new ArrayList<Set<Rating>>();
		// Create the empty lists
		for (int i=0;i<nbFolds;i++) {
			result.add(new HashSet<Rating>());
		}
		
		// Split data randomly across users
		if (this.globalRandomSplit) {
			// Use an array to shuffle things
			List<Rating> ratingsCopy = new ArrayList<Rating>(dataModel.getRatings());
			// Shuffle the ratings first
			Collections.shuffle(ratingsCopy);
			// distribute the ratings round robin to the bins
			int i = 0;
			for (Rating r : ratingsCopy)  {
				result.get(i%nbFolds).add(r);
				i++;
			}
			return result;
		}
		// Distribute things per user
		else {
			Random random = new Random();
			// Get the ratings each user
			for (Integer user : dataModel.getUsers()){
				// Get a copy to shuffle
				List<Rating> ratingsCopy = new ArrayList<Rating> (dataModel.getRatingsOfUser(user));
				Collections.shuffle(ratingsCopy);
				// distribute to the bins
				// do not start with 0 all the time as this leads to unbalanced bins
				int i = random.nextInt(nbFolds);
				for (Rating r : ratingsCopy) {
					result.get(i%nbFolds).add(r);
					i++;
				}
			}
//			// Display the size of the bins
//			for (Set<Rating> bin : result) {
//				System.out.println("Bin size: " + bin.size());
//			}
			return result;
		}

	}
	
	public List<Set<Rating>>getSpecialTestSplits() {
		return this.smallSplits;
	}

}
