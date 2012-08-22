/**
 * 
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * @author cfahey
 * 
 */
public class Mothership {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// OSCFunctions.examples();

		// list all links in main menu
		// OSCFunctions.listLinks("http://www.osc.edu", false);
		//OSCFunctions.listLinks("/Users/cfahey/Desktop/index.shtml", true);

		// There is no way to map Menus when using Feeds module to import CSV file.

		// set of pages that are hacked w/o id="main" container
		Set<String> noMainID = new HashSet<String>(500);
		// set of pages that are FORMs and need to be made into webforms
		Set<String> webforms = new HashSet<String>(500);
		// set of pages with no titles/h tags
		Set<String> noTitles = new HashSet<String>(500);
		// set of files that have corrupt CSV lines
		Set<String> coruptCSVLine = new HashSet<String>(500);

		// set of urls to parse
		Set<String> pages = new HashSet<String>(500);

		Set<ArrayList> csvItems = new HashSet<ArrayList>(500);

		//String directoryRoot = "www/about"; // need trailing slash
		String directoryRoot = "www"; // need trailing slash


		// find all the pages, get list of file paths
		pages.addAll(ScrapeHTML.findFileTypeCount(directoryRoot, "shtml"));
		// System.out.println(pages);
		pages.addAll(ScrapeHTML.findFileTypeCount(directoryRoot, "html"));
		pages.addAll(ScrapeHTML.findFileTypeCount(directoryRoot, "htm"));
		// System.out.println(pages);

		// iterate over all files
		for (String pageLocation : pages) {
			// System.out.println(page);

			String currentFilePath = pageLocation;
			// OSCFunctions.listLinks(currentFilePath, true);

			// does the file have an id="main"?
			// if no main ID then hacked, inspect it manually and get a listing
			Element main = ScrapeHTML.getMainConent(currentFilePath, true);
			String title = "";
			String URL = "";
			
			// see if document has id="main" container
			if (main == null) {
				// main container does not exist; add to list of manual inspections
				noMainID.add(currentFilePath);

				// go to next file
			} else 	if (ScrapeHTML.hasForms(main)) {
				//ignore forms because will be handled differently
				webforms.add(currentFilePath); // need to be made into webform

			} else {

				// find the largest <H#> tag and set that as the title
				title = ScrapeHTML.findLargestTitle(main);
				if (title.length() <= 2) {
					noTitles.add(currentFilePath);
					title = "NO TITLE FOUND";
				} else {
					//escape any titles that have "
					title = title.replaceAll("[.]*\"[.]*", "\\\"\"\"");
				}
				//title = title.replaceAll("[.]*,[.]*", "\\\",\"");

				// create correct new URL
				URL = ScrapeHTML.createURL(currentFilePath);
				
				if(URL.contains("content")){
					//System.out.println(currentFilePath);
				}

				// santize all tags by removing inline styles, &nbsp;, valign,
				// id, class, etc.
				main = ScrapeHTML.stripStuff(main);

				// update all the links to webpages
				// dir/dir/name.txt = dir/dir/name
				main = ScrapeHTML.updateAnchorPageLinks(main, currentFilePath);

				// update links to files like .pdf and jpg
				main = ScrapeHTML.updateAnchorFileLinks(main, currentFilePath);

				// update links in img tags and move the files to new drupal
				// location
				main = ScrapeHTML.updateImageSrc(main, currentFilePath);
				
				// removes all the returns/newlines
				//String HTML = main.children().toString();
				String HTML = main.children().toString().replaceAll("\\r\\n|\\r|\\n", " ");

				// find and replace all smart quotes, and dashes
				// these problems are due to pasting from Word
				HTML = HTML.replaceAll("\u2018", "&lsquo;"); // single smart quote left
				HTML = HTML.replaceAll("\u2019", "&rsquo;"); // single smart quote right
				HTML = HTML.replaceAll("\u201C", "&ldquo;"); // double left smart quote
				HTML = HTML.replaceAll("\u201D", "&rdquo;"); // double right smart quote
				HTML = HTML.replaceAll("\u2013", "&ndash;"); // en dash
				HTML = HTML.replaceAll("\u2014", "&mdash;"); // em dash
				HTML = HTML.replaceAll("\u2015", "-"); // horizontal bar
				
				//remove any common virtuals
				HTML = HTML.replaceAll("<!--#include virtual=\"/header.shtml\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/footer.html\" -->", ""); // horizontal bar
				HTML = HTML.replaceAll("<!--#include virtual=\"/about/about_nav.shtml\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/images/box_highlights.htm\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/press/press_nav.shtml\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/services/services_nav.shtml\" -->", ""); 
				HTML = HTML.replaceAll("<!--#include virtual=\"/initiatives/initiatives_nav.shtml\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/network/network_nav.shtml\" -->", "");
				HTML = HTML.replaceAll("<!--#include virtual=\"/support/support_nav.shtml\" -->", ""); 
				
				ArrayList<String> csvEntry = new ArrayList<String>();
				// convert body to ASCII
				HTML = Normalizer.normalize(HTML, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				
				//esacpe any double quotes for compatable CSV file
				HTML = HTML.replaceAll("[.]*\"[.]*", "\\\"\"");
				
				csvEntry.add(0, title); // trim title to 255
				csvEntry.add(1, HTML);
				csvEntry.add(2, URL);
				csvItems.add(csvEntry);
	
				//System.out.println("URL: " + URL);

			}
		}

		System.out.println(noMainID.size() + " files with no Main Container: " + noMainID);
		System.out.println(webforms.size() + " files with forms: " + webforms);
		System.out.println(noTitles.size() + " files with bad titles: " + noTitles);
		System.out.println(coruptCSVLine.size() + " files with bad CSV Lines/ignored: " + coruptCSVLine);
		System.out.println(pages.size() + " files processed.");

		createCSVFile(csvItems);
		
		System.out.println("Exited Gracefully");

	}

	/**
	 * Puts all of the items in the file to feed into Drupal
	 * 
	 * @param csvItems
	 */
	private static void createCSVFile(Set<ArrayList> csvItems) {

		// header for the CSV file

		String CSVHeader = "Title,Body,URL-Alias\n";
		try {
			// Create file
			FileWriter fstream = new FileWriter("about.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(CSVHeader);

			// print each CSV line item
			for (ArrayList line : csvItems) {
				out.write("\"" + line.get(0) + "\",\"" + line.get(1) + "\"," + line.get(2));
				out.write("\n");
			}
			out.close(); // close the CSV file
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

} // end Mothership class
