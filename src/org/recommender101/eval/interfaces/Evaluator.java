/** DJ **/
package org.recommender101.eval.interfaces;

import org.recommender101.data.DataModel;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Instantiable;

/**
 * A generic evaluator to be called during the tests
 * An evaluator is a class that is used for collecting the partial evaluation results (for example the average error or the precision per user)
 * Subclasses implement the specific logic.
 * @author DJ
 *
 */
public abstract class Evaluator extends Instantiable {
	
	/**
	 * The local data model
	 */
	protected DataModel testDataModel = null;

	/**
	 * A handle to the recommender
	 */
	AbstractRecommender recommender;
	
	
	/**
	 * The recommender name 
	 */
	 String recommenderName = null;
	
	// =====================================================================================
	/**
	 * Get the data model
	 * @return
	 */
	public DataModel getTestDataModel() {
		return testDataModel;
	}

	
	/**
	 * Returns the recommender name
	 * @return
	 */
	public String getRecommenderName() {
		return recommenderName;
	}

	
	/**
	 * Sets the recommender name
	 * @param recommenderName
	 */
	public void setRecommenderName(String recommenderName) {
		this.recommenderName = recommenderName;
	}


	// =====================================================================================
	/**
	 * Set the data model
	 * @param testDataModel
	 */
	public void setTestDataModel(DataModel testDataModel) {
		this.testDataModel = testDataModel;
	}

	// =====================================================================================
	/**
	 * Can be overwritten in a subclass
	 */
	public void initialize() {};
	// =====================================================================================
	// In some cases, it might be usefule to know the training datamodel
	/**
	 * The training data model
	 */
	protected DataModel trainingDataModel = null;

	/**
	 * To retrieve the training data model
	 * @return
	 */
	public DataModel getTrainingDataModel() {
		return trainingDataModel;
	}

	/**
	 * Setter for the training data model
	 * @param trainingDataModel
	 */
	public void setTrainingDataModel(DataModel trainingDataModel) {
		this.trainingDataModel = trainingDataModel;
	}


	/**
	 * Get the recommender used for this evaluation
	 * @return
	 */
	public AbstractRecommender getRecommender() {
		return recommender;
	}


	/**
	 * Sets the recommender used for the evaluation
	 * @param recommender
	 */
	public void setRecommender(AbstractRecommender recommender) {
		this.recommender = recommender;
	}
	
	
	
	
	
}
