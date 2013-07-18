/** DJ **/
package org.recommender101.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * A default data loader capable of loading movielens files
 * Format: user<tab>item<tab>rating<tab>timestamp
 * @author DJ
 *
 */
public class DefaultDataLoader  {

	// A default location
	protected String filename = "data/movielens/ratings.txt";
	protected int minNumberOfRatingsPerUser = -1;
	protected int sampleNUsers = -1;
	protected double density = 1.0;
	
	public static int maxLines = -1;

	// Should we transform the data 
	// 0 no
	// > 0: This is the threshold above which items are relevant 
	public int binarizeLevel = 5;     // changed
	
	// Should we remove 0 -valued ratings?
	public boolean useUnaryRatings = true;  // changed
	
	/**
	 * An empty constructor
	 */
	public DefaultDataLoader() {
	}
	
	
	public void applyConstraints(DataModel dm) throws Exception{
		// Apply sampling procedure
		if (this.sampleNUsers > -1) {
			dm = Utilities101.sampleNUsers(dm, sampleNUsers);
			dm.recalculateUserAverages();
		}
		
		// Apply min number of ratings constraint
		if (minNumberOfRatingsPerUser > 0) {
			Utilities101.applyMinRatingsPerUserConstraint(dm, minNumberOfRatingsPerUser);
			dm.recalculateUserAverages();
		}
		
		// Apply sampling of data to vary density
		if (this.density < 1.0) {
			dm = Utilities101.applyDensityConstraint(dm, this.density);
			dm.recalculateUserAverages();
		}
		
		if (this.binarizeLevel > 0) {
			Debug.log("Binarizing at level: " + this.binarizeLevel);
			this.binarize(dm);
		}
	}
		
	// =====================================================================================

	/**
	 * The method loads the MovieLens data from the specified file location.
	 * The method can be overwritten in a subclass
	 */
	public void loadData(DataModel dm) throws Exception {
		int counter = 0;
		// Read the file line by line and add the ratings to the data model.
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		line = reader.readLine();
		String[] tokens;
		while (line != null) {
			// Skip comment lines
			if (line.trim().startsWith("//")) {
				line = reader.readLine();
				continue;
			}
			tokens = line.split("::");  // ::
			// First, add the ratings.
			dm.addRating((int)Float.parseFloat(tokens[0]), (int)Float.parseFloat(tokens[1]), (int)Float.parseFloat(tokens[2]));
			line = reader.readLine();
//			System.out.println("line.." + line + " (" + counter + ")");
			counter++;
//			// debugging here..
			if (maxLines != -1) {
				if (counter >= maxLines) {
					System.out.println("DataLoader: Stopping after " + (counter)  + " lines for debug");
					break;
				}
			}
		}
		Debug.log("DefaultDataLoader:loadData: Loaded " + counter + " ratings");
		Debug.log("DefaultDataLoader:loadData: " + dm.getUsers().size() + " users and " + dm.getItems().size() + " items.");
		reader.close();
		
		
		applyConstraints(dm);
		
		
		
	}
	
	// =====================================================================================

	
	/**
	 * Sets the file name
	 * @param name
	 */
	public void setFilename(String name) {
		filename = name;
	}

	// =====================================================================================

	/**
	 *  Returns the filename 
	 * 
	 */
	public String getFilename() {
		return filename;
	}
	// =====================================================================================

	/**
	 * To be used by the class instantiator. Defines the minimum number of ratings a user 
	 * must have to remain in the dataset
	 * @param n the min number of ratings per user
	 */
	public void setMinNumberOfRatingsPerUser(String n) {
		minNumberOfRatingsPerUser = Integer.parseInt(n);
	}
	
	
	/**
	 * Instructs the load to sample a given number of users
	 * @param n how many users to keep 
	 */
	public void setSampleNUsers(String n) {
		this.sampleNUsers = Integer.parseInt(n);
	}
	
	/**
	 * Set the density
	 * @param d
	 */
	public void setDensity(String d) {
		this.density = Double.parseDouble(d);
	}
	
	/**
	 * Set the binarization method
	 * @param b
	 */
	public void setBinarizeLevel(String b) {
		this.binarizeLevel = Integer.parseInt(b);
	}
	
	/**
	 * Binarizes the data model after loading
	 * @throws Exception
	 */
	public void binarize(DataModel dm) throws Exception {
		
		Set<Rating> ratingsCopy = new HashSet<Rating>(dm.getRatings());
		
		// Go through the ratings
		for (Rating r : ratingsCopy) {
			// Option one - every rating is relevant
			
			if (r.rating >= this.binarizeLevel) {
				r.rating = 1;
			}
			else {
				// Remove rating in case we only have positive feedback
				if (this.useUnaryRatings) {
					dm.removeRating(r);
				}
				// Otherwise, set it to 0
				else {
					r.rating = 0;
				}
			}
		}
		// Recalculate things
		dm.recalculateUserAverages();
//		System.out.println("Binarization done (" + dm.getRatings().size() + " ratings)");
	}
	
	/**
	 * Sould we use unary ratings? (If yes, we delete all 0 ratings)
	 * @param b
	 */
	public void setUnaryRatings(String b) {
		this.useUnaryRatings = Boolean.parseBoolean(b);
	}
	
	
	
//	
//	public static void main(String[] args) {
//		try {
//			
//			Properties props = new Properties();
//			props.load(new FileReader("conf/recommender101.properties"));
//			Recommender101 r101 = new Recommender101(props);
//
//			DefaultDataLoader dl = new DefaultDataLoader();
//			dl.setFilename("/projects/recommender101-core/data/movielens/ratings.txt");
//			DataModel dm = new DataModel();
//			dl.loadData(dm);
//			dl.binarize(dm);
//			System.out.println("Binarized.." + dm.getRatings().size() + " ratings");
//			
//		}
//		catch (Exception e){
//			e.printStackTrace();
//		}
//		System.out.println("Dataloader test ended.");
//	}

}
