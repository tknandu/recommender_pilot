/** DJ **/
package org.recommender101.eval.interfaces;



/**
 * A generic evaluation result (to be done)
 * @author DJ
 *
 */
public class EvaluationResult {
	// The name of the metric
	String methodName;
	// The result;
	double value;
	// The algorithm on which it was applied
	String algorithm;
	// Any extra info to be made accessible
	Object extraInformation;
	// The evaluation round the experiment this result is assigned to was started in (timkraemer)
	int evaluationRound;
	
	// =====================================================================================

	public String getMethodName() {
		return methodName;
	}
	
	// =====================================================================================

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	// =====================================================================================

	public EvaluationResult(String thealgorithm, String methodName, double value) {
		super();
		this.algorithm = thealgorithm;
		this.methodName = methodName;
		this.value = value;
	}
	
	// =====================================================================================

	public double getValue() {
		return value;
	}
	
	// =====================================================================================

	public void setValue(double value) {
		this.value = value;
	}
	
	// =====================================================================================

	public String getAlgorithm() {
		return algorithm;
	}
	
	// =====================================================================================

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	// =====================================================================================
	
	public int getEvaluationRound() {
		return evaluationRound;
	}
	
	// =====================================================================================

	public void setEvaluationRound(int evaluationRound) {
		this.evaluationRound = evaluationRound;
	}

	/**
	 * Returns the extra information attached to this metric
	 * @return
	 */
	public Object getExtraInformation() {
		return extraInformation;
	}

	/**
	 * Sets an extra piece of information for this metric
	 * @param extraInformation
	 */
	public void setExtraInformation(Object extraInformation) {
		this.extraInformation = extraInformation;
	}

	/**
	 * Returns a string representation of the result
	 */
	public String toString() {
		return "\nAlgorithm: " + this.algorithm + ",\nMetric: " + this.getMethodName() + ",\nValue: "+ this.value;
	}
	
	/**
	 * Returns true if both the algorithm class name and the method name are identical
	 * @param otherResult
	 * @return
	 */
  public boolean equals(EvaluationResult otherResult) {
//  	System.out.println("------------");
//  	System.out.print("Comparing " );
//  	System.out.println("this. algorithm: " + this.algorithm);
//  	System.out.println("other algo: " + otherResult.algorithm);
//  	System.out.println("this method: " + this.methodName + " other: " + otherResult.methodName);
  	if (this.algorithm.equals(otherResult.algorithm) &&
  			this.methodName.equals(otherResult.methodName)
  			) {
  		return true;
  	}
  	else {
  		return false;
  	}
  }
	
}
