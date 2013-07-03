/** DJ **/
package org.recommender101.data.extensions.dataloader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.recommender101.data.DataModel;
import org.recommender101.data.DefaultDataLoader;
import org.recommender101.data.Rating;
import org.recommender101.tools.Debug;

/**
 * A demo for a data loader extension which also loads the time stamp from the movielens file
 * and adds this information to the default extension pointer of the default data model
 * @author DJ
 *
 */
public class DefaultDataLoaderWithTimeStamp extends DefaultDataLoader {
	
	public static final String DM_EXTRA_INFO_TIMESTAMP_KEY = "RatingTimeStamps";
	public static final String DM_EXTRA_INFO_TIMESTAMP_KEY_SEPARATOR = ":";
	
	// =====================================================================================

	/**
	 * The method loads the MovieLens data from the specified file location and also reads the time information
	 */
	@Override
	public void loadData(DataModel dm) throws Exception {
		
		// We construct a hashmap that stores a mapping of "user:item"->"timestamp". The key is the concatenated string
		// of user and item id.
		Map<Rating,Long> timestamps = new HashMap<Rating, Long>();
		dm.addExtraInformation(DM_EXTRA_INFO_TIMESTAMP_KEY, timestamps);
		
		int counter = 0;
		// Read the file line by line and add the ratings to the data model.
		BufferedReader reader = new BufferedReader(new FileReader(getFilename()));
		String line;
		line = reader.readLine();
		String[] tokens;
		Rating r; 
		
		while (line != null) {
			// Skip comment lines
			if (line.trim().startsWith("//")) {
				continue;
			}
			tokens = line.split("\t");
			r = dm.addRating(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
			// remember the timestamps
			timestamps.put(r,Long.parseLong(tokens[3]));
			line = reader.readLine();
			counter++;
		}
		Debug.log("DefaultDataLoader:loadData: Loaded " + counter + " ratings");
//		System.out.println("Timestamps: " + timestamps);
		reader.close();
	}


}
