/** DJ **/
package org.recommender101.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.Rating;

/**
 * Interface for data splitters
 * @author DJ
 *
 */
public abstract class DataSplitter {

	// A method that splits the dataset for cross validation into n bins
	public abstract List<Set<Rating>> splitData (DataModel dataModel) throws Exception;
	// Use a global split and not a per-user split; could be set to false in later experiments as default
	protected boolean globalRandomSplit = false;
	
	/**
	 * Remember the number of folds
	 */
	int nbFolds = 5;
	
	/**
	 * stores additional training data
	 */
	protected Set<Rating> additionalTrainingData = new HashSet<Rating>();
	
	/**
	 * Get the folds
	 * @return
	 */
	public int getNbFolds() {
		return nbFolds;
	}

	/**
	 * Alternative setter for the number of folds
	 * @param nbFolds
	 */
	public void setNbFolds(String nbFolds) {
		this.nbFolds = Integer.parseInt(nbFolds);
	}

	// Get small splits if any
	public List<Set<Rating>> getSpecialTestSplits() {
		return null;
	};

	// Split data randomly across users or not
	public void setGlobalRandomSplit(String b) {
		this.globalRandomSplit = Boolean.parseBoolean(b);
	}
	
	/**
	 * Returns additional training data
	 * @return
	 */
	public Set<Rating> getAdditionalTrainingData() { 
		return additionalTrainingData;
	}
}
