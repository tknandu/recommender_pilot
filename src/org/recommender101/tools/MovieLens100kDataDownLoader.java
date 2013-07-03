package org.recommender101.tools;

import java.io.File;

/**
 * A class that downloads and extracts the movielens 100k dataset (if not
 * already present)
 * 
 * @author Dietmar
 * 
 */
public class MovieLens100kDataDownLoader {

	 static String DOWNLOAD_URL = "http://www.grouplens.org/system/files/ml-100k.zip";

	static String TARGET_FILE = "MovieLens100kRatings.txt";
	static String TARGET_DIRECTORY = "data/movielens/";
	static String TEMP_FILE = "ml-100k.zip";

	/**
	 * The main downloader method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Testing ML 100k download");
		boolean result = false;
		try {
			result = MovieLens100kDataDownLoader.downloadML100k();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Tests successful: " + result);

	}

	/**
	 * The main working method. Downloads extracts and transforms the data file
	 * 
	 * @return
	 */
	public static boolean downloadML100k() {
		boolean downLoadSuccessful = false;
		System.out.println("Checking and obtaining MovieLens 100k data");

		String filename = TARGET_DIRECTORY + TARGET_FILE;
		File f = new File(filename);
		if (f.exists()) {
			System.out.println("Target file " + filename + " already exists.");
			return true;
		} else {
			downLoadSuccessful = downloadAndProcessML100kFile(TARGET_DIRECTORY, TEMP_FILE, DOWNLOAD_URL, TARGET_FILE);
		}

		return downLoadSuccessful;
	}

	/**
	 * A method that actually downloads the zip file and stores it locally
	 */
	@SuppressWarnings({ "rawtypes", "resource" })
	static boolean downloadAndProcessML100kFile(String targetDirectory, String tempFile, String downloadURL, String targetFileName) {
		try {
			Utilities101.downloadFile(targetDirectory, tempFile, downloadURL);
			Utilities101.extractFileFromZip(targetDirectory, tempFile, "ml-100k/u.data", targetFileName);

		} catch (Exception e) {
			System.out.println("Cound not download or process Movielens file: "
					+ e.getMessage());
			return false;
		}
		return true;
	}
	

	

	


}
