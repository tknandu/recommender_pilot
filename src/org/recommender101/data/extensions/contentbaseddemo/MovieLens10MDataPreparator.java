package org.recommender101.data.extensions.contentbaseddemo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.recommender101.tools.Utilities101;

import edu.udo.cs.wvtool.config.WVTConfiguration;
import edu.udo.cs.wvtool.config.WVTConfigurationFact;
import edu.udo.cs.wvtool.generic.output.WordVectorWriter;
import edu.udo.cs.wvtool.generic.stemmer.FastGermanStemmer;
import edu.udo.cs.wvtool.generic.stemmer.PorterStemmerWrapper;
import edu.udo.cs.wvtool.generic.vectorcreation.TFIDF;
import edu.udo.cs.wvtool.main.WVTDocumentInfo;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.main.WVTool;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

/**
 * A class that retrieves data from grouplens.org (MovieLens10M), pre-processes available content 
 * information for some of the movies and creates TF-IDF vectors using the word vector tool
 * 
 * The downloaded file is at : http://www.grouplens.org/sites/www.grouplens.org/external_files/data/ml-10m.zip
 * 
 * 
 * @author dietmar
 *
 */
public class MovieLens10MDataPreparator {
	
	public static String TARGET_DIRECTORY = "data/movielens/";
	public static String TEMP_FILE = "ml-10m.zip";
	public static String DOWNLOAD_URL = "http://www.grouplens.org/sites/www.grouplens.org/external_files/data/ml-10m.zip";
	public static String TARGET_FILE = "ratings.dat";
	public static String FINAL_FILE = "MovieLens5MRatings.txt";
	public static String FILE_TO_EXTRACT ="ml-10M100K/ratings.dat";
	public static String CONTENT_ZIP = "MovieLens10MContentDescriptions.zip";
	public static String CONTENT_FILE = "MovieLens10MContentDescriptions.txt";
	public static String TMP_DIRECTORY = TARGET_DIRECTORY + "tmp";
	public static String TF_IDF_FILENAME = "tf-idf-vectors.txt";
	public static String WORDLIST_FILENAME = "wordlist.txt";
	/**
	 * Default constructor 
	 */
	public MovieLens10MDataPreparator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Main entry point
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Starting data preparation for ML data set with content information");
			MovieLens10MDataPreparator preparator = new MovieLens10MDataPreparator();
			preparator.run();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Data preparation completed");
	}
	
	
	/**
	 * Does the actual work. 
	 * @throws Exception
	 */
	public void run() throws Exception {
		// Get the zip file
		System.out.println("Downloading data from grouplens.org (64mb). This may take some time");
		Utilities101.downloadFile(TARGET_DIRECTORY, TEMP_FILE, DOWNLOAD_URL);
		System.out.println("Data has been downloaded, extracting zip file");
		// Extract the ratings file
		Utilities101.extractFileFromZip(TARGET_DIRECTORY, TEMP_FILE, FILE_TO_EXTRACT, TARGET_FILE);
		System.out.println("Extracted rating information from zip file");		
		// Extract the contents file
		Utilities101.extractFileFromZip(TARGET_DIRECTORY, CONTENT_ZIP, CONTENT_FILE, CONTENT_FILE);
		System.out.println("Extracted content information from zip file");	
		// get the correct product ids
		Set<Integer> relevantProductIDs = getIDsOfMoviesWithContentInfo(TARGET_DIRECTORY + CONTENT_FILE);
		// create the ratings file from the given one and remove movies without content info
		removeRatingsOfMoviesWithoutContentInfo(TARGET_DIRECTORY, TARGET_FILE, relevantProductIDs, FINAL_FILE);
		System.out.println("Extracted relevant ratings from data file to " + FINAL_FILE);
		// create the tf-idf vectors
		System.out.println("Creating TF-IDF vectors from content information, creating temporary files");
		Set<String> filenames = runFileSplitter(TARGET_DIRECTORY, CONTENT_FILE, TMP_DIRECTORY);
//		System.out.println("Created " + filenames.size() + " temporary files");
		System.out.println("Creating output files containing tfidf vectors and wordlists");
		generateWordVectorsAndWordList(filenames,TMP_DIRECTORY,WORDLIST_FILENAME,TF_IDF_FILENAME, TARGET_DIRECTORY, 20,1000,true,"german");
		System.out.println("Created tf-idf vectors");
	}
	
	
	/**
	 * A method that creates a new ratings file and removes all lines that contain irrelevant product ids
	 */
	void removeRatingsOfMoviesWithoutContentInfo(String targetDirectory, String targetFile, Set<Integer> relevantProductIDs, String resultFile) 
			throws Exception {
		System.out.println("Extracting ratings with content information");
		BufferedReader reader = new BufferedReader(new FileReader(targetDirectory + targetFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDirectory + resultFile));
		String line;
		String[] tokens;
		line = reader.readLine();
		int cnt = 0;
		int movieId;
		double rating = 0;
		int newRating = 0;
		while (line != null) {
			tokens = line.split("::");
			movieId = Integer.parseInt(tokens[1]);
			if (relevantProductIDs.contains(movieId)) {
				rating = Double.parseDouble(tokens[2]);
				// rounding..
				newRating = (int) Math.ceil(rating);
				writer.write(tokens[0] + "\t" + tokens[1] + "\t" + newRating + "\t" + tokens[3]);
				writer.write("\n");
				cnt++;
			}
			line = reader.readLine();
		}
		System.out.println("Wrote " + cnt + " ratings to target file " + resultFile);
		reader.close();
		writer.close();
	}

	
	/**
	 * A method that extracts the IDs of movies for which we have content information
	 * @param filename
	 * @return
	 */
	public Set<Integer> getIDsOfMoviesWithContentInfo(String filename) throws Exception {
		Set<Integer> result = new HashSet<Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		String[] tokens;
		int id;
		while (line != null) {
			tokens = line.split("#");
			id = Integer.parseInt(tokens[0]);
			result.add(id);
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Extracted " + result.size() + " relevant items from content file");
		return result;
	}
	
	
	// Reads the file and generates one input file per entry in our directory. Use movie-IDs as file names
	/**
	 * Splits the given content file into individual files based on the movie id for a later use
	 * for the word vector tool
	 * @param contentDirectory the directory of the source file
	 * @param contentFileName the content file
	 * @param outputDirectory the output directory for the temporary files
	 * @return a set of filenames
	 * @throws Exception
	 */
	public static Set<String> runFileSplitter(String contentDirectory, String contentFileName, String outputDirectory) throws Exception{
		
		System.out.println("OUTPUTDIR = " + outputDirectory);
		// We know where the file is: 
		String inputFile = contentDirectory + contentFileName;
		System.out.println("Splitting the file: " + inputFile);

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		
		// We will return the file names at the end.
		Set<String> filenames = new HashSet<String>();
		
		// Read file line by line and create one outputfile
		String line = reader.readLine();
		String filename;
		int idx = -1;
		String itemID;
		BufferedWriter writer;
		int counter = 0;
		while (line != null && line.length() > 0) {
			idx = line.indexOf("#");
			itemID = line.substring(0,idx);
//			System.out.println("ID: " + itemID);
			// Remove stuff
			line = line.substring(idx+1);
			// Remove the actors
			idx = line.lastIndexOf("#");
			line = line.substring(0,idx);
			
			line = line.replaceAll("#", " ");
//			System.out.println("Line: " + line);
			
			filename = itemID;
			filenames.add(filename);
			writer = new BufferedWriter(new FileWriter(outputDirectory + "/" + filename));
			writer.write(line);
			writer.close();
			counter ++;
			
			line = reader.readLine();
		}
		reader.close();
		return filenames;
	}
	
	/**
	 * The method opens all the files, creates the word vectors and stores the data in the
	 * files called 
	 * @param filenames
	 */
	/**
	 * The method opens a set of files and creates the tf-idf vectors and wordlist-files
	 * @param filenames names of the files ot analyze
	 * @param contentDirectory the directory where to look at
	 * @param wordListFileName the name of the file containing the wordlists 
	 * @param tfidfFileName the name of the file containing the content inf
	 * @param outputDirectory the output directry
	 * @throws Exception
	 */
	public static void generateWordVectorsAndWordList(Set<String> filenames, 
														String contentDirectory, 
														String wordListFileName, 
														String tfidfFileName, 
														String outputDirectory,
														int minFrequency,
														int maxFrequency,
														boolean useStemming,
														String language
														) throws Exception {

			// Create the word vector tool
			WVTool wvt = new WVTool(true);
			WVTFileInputList list = new WVTFileInputList(filenames.size());
			
			// Add all the files
			int cnt = 0;
			for (String filename : filenames) {
				list.addEntry(new WVTDocumentInfo(contentDirectory + "/" + filename, "txt","","language"));
				cnt++;
			}
			
			System.out.println("Processed " + cnt + " files");
			
			
			// Stemming
			WVTConfiguration config = new WVTConfiguration();
			
			if (useStemming) {
				if ("german".equals(language)) {
					config.setConfigurationRule(WVTConfiguration.STEP_STEMMER, new WVTConfigurationFact(new FastGermanStemmer()));
				}
				else {
					config.setConfigurationRule(WVTConfiguration.STEP_STEMMER, new WVTConfigurationFact(new PorterStemmerWrapper()));
				}
			}
			
			// create the word list
			WVTWordList wordList = wvt.createWordList(list, config);

			// pruning seems to be necessary? 
			wordList.pruneByFrequency(minFrequency, maxFrequency);
			
			// Store the results somewhere
			wordList.storePlain(new FileWriter(outputDirectory + "/" + wordListFileName));
			
			// Also the outputs
			String tempFile = contentDirectory + tfidfFileName + ".temp";
			System.out.println("Trying to write to " + tempFile);
			
			FileWriter fileWriter = new FileWriter(tempFile);
			WordVectorWriter wvw = new WordVectorWriter(fileWriter, true);
			config.setConfigurationRule( WVTConfiguration.STEP_OUTPUT, new WVTConfigurationFact(wvw));
			config.setConfigurationRule(WVTConfiguration.STEP_VECTOR_CREATION, new WVTConfigurationFact(new TFIDF()));

			// Create everything and go
			wvt.createVectors(list, config, wordList);
			
			wvw.close();
			fileWriter.close();

			// Now delete all the files we have created
			File fileToDelete;
			for (String filename : filenames) {
				fileToDelete = new File(filename);
				fileToDelete.delete();
			}
			
			// transform the outputfile and only keep the id instead of the filename
			BufferedReader reader = new BufferedReader(new FileReader(tempFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputDirectory + tfidfFileName));
			String line = reader.readLine();
			int idx1 = -1;
			int idx2 = -1;
			String fname;
			String id;
			while (line != null) {
				idx1 = line.indexOf(";");
				fname = line.substring(0,idx1);
				// find the 
				idx2 = line.lastIndexOf("/");
				id = fname.substring(idx2+1);
				writer.write(id + ";" + line.substring(idx1+1) + "\n");
				line = reader.readLine();
			}
			reader.close();
			writer.close();
			fileToDelete = new File(tempFile);
			fileToDelete.delete();

	} 
	

}
