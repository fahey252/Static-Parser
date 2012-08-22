import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
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
 * 
 */

/**
 * @author cfahey
 * 
 */
/**
 * @author cfahey
 * 
 */
public class ScrapeHTML {
	public static void examples() {

		String html = "<html><head><title>First parse</title></head>"
				+ "<body><p>Parsed HTML into a doc.</p></body></html>";

		Document doc = Jsoup.parse(html); // ensures valid docuemnt

		/*
		 * String html = "<div><p>Lorem ipsum.</p>"; Document doc =
		 * Jsoup.parseBodyFragment(html); Element body = doc.body(); //gets the
		 * element children in body element doc.getElementsByTag("body"); //does
		 * same thing clean(String bodyHtml, Whitelist whitelist); //clean up
		 * fro XSS
		 */

		// screenscraping
		Document osc = null;
		try {
			osc = Jsoup.connect("http://www.osc.edu/").get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String title = osc.title();
		System.out.println(title);

		// localfile sysetm
		File input = new File("/Users/cfahey/Desktop/index.shtml");
		Document localDoc = null;
		try {
			localDoc = Jsoup.parse(input, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(localDoc.title());

		// Get all the links on the pages
		Element content = osc.getElementById("storywrap");
		Elements links = content.getElementsByTag("a");
		Elements links2 = doc.select("a[href]"); // a with href
		Elements pngs = doc.select("img[src$=.png]"); // img with src ending
														// .png
		Elements resultLinks = doc.select("h3.r > a"); // direct a after h3
		// Element link3 = doc.select("a").first();
		// String relHref = link3.attr("href"); // == "/"
		// String absHref = link3.attr("abs:href"); // "http://jsoup.org/"
		for (Element link : links) {
			String linkHref = link.attr("href");
			String linkText = link.text();
			System.out.println(linkText);
		}
	}

	/**
	 * Lists links in a file whether img, import, input or anchor
	 * 
	 * @param location
	 *            URi Of the file to search
	 * @param local
	 *            States if local or public file
	 */
	public static void listLinks(String location, Boolean local) {
		Document doc = getFile(location, local);

		Elements links = doc.select("a[href]");
		Elements media = doc.select("[src]");
		Elements imports = doc.select("link[href]");

		print("\nMedia: (%d)", media.size());
		for (Element src : media) {
			if (src.tagName().equals("img")) {

				// ignore spacers cause we have alot
				if (!(src.attr("src").contains("spacer.gif"))) {
					print(" * %s: <%s> %sx%s (%s)", src.tagName(),
							src.attr("src"), src.attr("width"),
							src.attr("height"), trim(src.attr("alt"), 20));
				}
			} else {
				print(" * %s: <%s>", src.tagName(), src.attr("src"));
			}
		}

		print("\nImports: (%d)", imports.size());
		for (Element link : imports) {
			print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"),
					link.attr("rel"));
		}

		print("\nLinks: (%d)", links.size());
		for (Element link : links) {
			print(" * a: <%s>  (%s)", link.attr("href"), // or abs:href
					trim(link.text(), 35));
		}
	}

	/**
	 * Get the main body content
	 * 
	 * @param location
	 *            URi Of the file to search
	 * @param local
	 *            States if local or public file
	 */
	public static Element getMainConent(String location, Boolean local) {

		Document doc = getFile(location, local);
		if (doc == null) {
			return null;
		}
		Element content = doc.getElementById("main");
		
		if(content == null) {
			//had no main container
			content = doc.body();
		}
		
		/* need to see if file includes any virtual content and if so, get it.
		 *  <td id="main" valign="top">
            	<!--#include virtual="content.html"-->
			</td>
		 */

		return content;

	}

	/**
	 * Local folder want to recurse.
	 * 
	 * @param folder
	 *            local folder want to recurse down
	 * @param type
	 *            file extention including .
	 */
	public static Set<String> findFileTypeCount(String folder, String type) {
		// iterate over all files

		Set<String> allFiles = new HashSet<String>(500);

		File dir = new File(folder);
		// File dir = new File("/Volumes/web-content/www");

		try {
			System.out.println("Getting ." + type + " files in "
					+ dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<File> files = (List<File>) FileUtils.listFiles(dir,
				TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		int count = 0;
		String pattern = ".*[.]{1}" + type;
		for (File file : files) {
			// find all files of a certain ending i.e. .shtml
			if (file.getName().matches(pattern)) {
				// System.out.println(file.getName()); //file name only
				// System.out.println("file: " + file);

				// add the file name to the set
				allFiles.add(file.toString());
				// OSCFunctions.listLinks(file.getCanonicalPath(), true);
				count++;
			}

		}

		System.out.println("Number of " + type + " files " + count + "\n");

		return allFiles;
	}

	/**
	 * Get a set of links in the OSC Main menu
	 * 
	 * OBSOLETE - Can't add menu info with feeds
	 * 
	 * @return
	 */
	public static Set<String> getMainMenuLinks(String file) {
		Set<String> urls = new HashSet<String>(500);

		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Add URLs to set
				urls.add(strLine);
			}
			// Close the input stream
			in.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		return urls;
	}

	/**
	 * Gets the largest heading/title in the document
	 * 
	 * @param html
	 *            - Main container
	 * @return String - title to use in Drupal
	 */
	public static String findLargestTitle(Element html) {
		// return first h tag that is not null

		// remove the title from body

		Element h1 = html.getElementsByTag("h1").first();
		if (h1 != null) {
			h1.remove();
			return h1.text();
		}
		Element h2 = html.getElementsByTag("h2").first();
		if (h2 != null) {
			h2.remove();
			return h2.text();
		}
		Element h3 = html.getElementsByTag("h3").first();
		if (h3 != null) {
			h3.remove();
			return h3.text();
		}
		Element h4 = html.getElementsByTag("h4").first();
		if (h4 != null) {
			h4.remove();
			return h4.text();
		}
		Element h5 = html.getElementsByTag("h5").first();
		if (h5 != null) {
			h5.remove();
			return h5.text();
		}
		Element h6 = html.getElementsByTag("h6").first();
		if (h6 != null) {
			h6.remove();
			return h6.text();
		}

		return ""; // no title

	}

	public static Boolean hasForms(Element html) {
		boolean hasForm = false; // assume page does not have a form

		if (html.select("form").first() != null) {
			hasForm = true;
		}

		return hasForm;
	}

	/*
	 * Turns the filename with the HTML into a URL-alias for drupal
	 * 
	 * about/directory.shtml becomes about/directory
	 * about/conferencerooms/index.shtml becomes about/conference rooms
	 * 
	 */
	public static String createURL(String filepath) {
		String newFilepath = filepath;

		// remove the www
		newFilepath = newFilepath.replaceFirst("[a-z-]*[^/]/", ""); // remove begin www
		
		// remove the file extention for Drupal
		if (newFilepath.contains(".")) {
			newFilepath = newFilepath.replaceFirst(".[a-zA-Z0-9]*$", "");
		}

		if (filepath.contains("/index")) {
			// strip index
			newFilepath = newFilepath.replaceFirst("/index$", "");
		}
		
		//System.out.println("New URL: " + filepath + " -> " + newFilepath);

		return newFilepath;

	}

	public static Element stripStuff(Element main) {
		Element sanitized = main;

		// remove classes
		Elements els = sanitized.select("[class]");
		for (Element e : els) {
			e.removeAttr("class");
		}

		// remove id's
		els = sanitized.select("[id]");
		for (Element e : els) {
			e.removeAttr("id");
		}

		// remove inline styles
		els = sanitized.select("[style]");
		for (Element e : els) {
			e.removeAttr("style");
		}

		// remove forms
		els = sanitized.select("form");
		for (Element e : els) {
			e.remove();
		}
		// remove code
		els = sanitized.select("object");
		for (Element e : els) {
			e.remove();
		}
		// remove scripts
		els = sanitized.select("script");
		for (Element e : els) {
			e.remove();
		}
		// remove scripts
		els = sanitized.select("noscript");
		for (Element e : els) {
			e.remove();
		}
		// remove widths
		els = sanitized.select("td[width]");
		for (Element e : els) {
			e.removeAttr("width");
		}
		els = sanitized.select("table[width]");
		for (Element e : els) {
			e.removeAttr("width");
		}
		// remove heights
		els = sanitized.select("td[height]");
		for (Element e : els) {
			e.removeAttr("height");
		}
		els = sanitized.select("table[height]");
		for (Element e : els) {
			e.removeAttr("height");
		}
		// remove backgrounds
		els = sanitized.select("[background]");
		for (Element e : els) {
			e.removeAttr("background");
		}
		
		// remove font face's
		els = sanitized.select("[face]");
		for (Element e : els) {
			e.removeAttr("face");
		}
		
		// remove alt's that are empty
		els = sanitized.select("[alt]");
		for (Element e : els) {
			if (e.attr("alt").toString().isEmpty()) {
				e.removeAttr("alt");
			}
			
			//remove commans from alts because breaks CSV
			if (e.attr("alt").toString().contains(",")) {
				e.attr("alt", e.attr("alt").toString().replace(',', ' '));
			}
			
		}
		// remove javascript
		els = sanitized.select("[onMouseOver]");
		for (Element e : els) {
			e.removeAttr("onMouseOver");
		}
		els = sanitized.select("[onMouseOut]");
		for (Element e : els) {
			e.removeAttr("onMouseOut");
		}
		els = sanitized.select("[onclick]");
		for (Element e : els) {
			e.removeAttr("onclick");
		}

		return sanitized;
	}

	/*
	 * Fix the src of <a> tags. Drupal cannot have relative links. Need to
	 * rename links to where they will be in Drupal
	 * 
	 * This normalizes hyperlink refences to point to a complete location.
	 * It up dates reference to files like PDF's, docs too but the function
	 * updateAnchorFileLinks corrects it to the correct location
	 * 
	 * i.e. <a href="directory.shtml"> is relative.
	 */
	public static Element updateAnchorPageLinks(Element main, String filepath) {
		// filepath String is need for relative urls
		// get the relative file path
		// www/about/contact.shtml becomes about/contact.shtml

		// System.out.println("File path: " + filepath); //debug
		// System.out.println("File path: " + filepath); //debug
		filepath = filepath.replaceFirst("[a-z-]*[^/]", ""); // remove begin www
		// about/contact.shtml becomes about/
		// remove everything (filename/extention) until first / encountered
		filepath = filepath.replaceFirst("[^/]*$", "");
		String originalFilePath = filepath.toString(); // may chance with ../../

		// get all <a> on the page
		Elements anchors = main.select("a");		

		for (Element anchor : anchors) {	

			String hyperlinkReference = anchor.attr("href");
			String originalReference = anchor.attr("href");
			
			if(hyperlinkReference.contains("/archive")) {
				//System.out.println("archive: " + hyperlinkReference);
			}
			
			// for ESRI pages, Basically, wherever you see 
			//http://landsat.ohiolink.edu/esri/ you can convert it to http://esri.osc.edu/.
			if(hyperlinkReference.startsWith("http://landsat.ohiolink.edu/esri/"))  {
				hyperlinkReference = hyperlinkReference.replace("http://landsat.ohiolink.edu/esri/", "http://esri.osc.edu/");
				//System.out.println(hyperlinkReference);
			}
			if(hyperlinkReference.startsWith("ftp://landsat.ohiolink.edu/esri/"))  {
				hyperlinkReference = hyperlinkReference.replace("ftp://landsat.ohiolink.edu/esri/", "http://esri.osc.edu/");
				//System.out.println(hyperlinkReference);
			}

			//if URL contains the complete URL to oar.net in first 20 characters like
			//http://www.oar.net/emailmigration/questions/index.shtml
			//everything previouly to oar.net needs to be removed so doesn't think its an external site
			if(hyperlinkReference.contains("www.oar.net")) {
				int index = hyperlinkReference.indexOf("oar.net");
				hyperlinkReference = hyperlinkReference.substring(index+7);
			}
			
			//remove commas from URL's because breaks links
			hyperlinkReference = hyperlinkReference.replace(',', ' ');
			
			// do not modify any mailto or if href is empty i.e. <a name="enter"
			// id="enter">
			if (!hyperlinkReference.startsWith("mailto:")
					&& !hyperlinkReference.isEmpty() && !hyperlinkReference.startsWith("ftp://")) {

				// some links contain .. (i.e. going up a directory) due to
				// improper coding
				if (hyperlinkReference.startsWith("..")) {
					// /research/csd/ and ../report/index.shtml -->
					// reportindex.shtm
					// should become /research/report
					while (hyperlinkReference.startsWith("..")) {
						//replace first ../
						hyperlinkReference = hyperlinkReference.replaceFirst("[.][.][/]", "");
						//now go up a directory
						filepath = filepath.replaceFirst("[^/]*/$", "");
					}

				}

				// handle links that have anchors
				int hashIndex = hyperlinkReference.indexOf('#');
				String hashAnchor = ""; // assume we have no anchors
				if (hashIndex != -1) {
					// link has an achor
					hashAnchor = hyperlinkReference.substring(hashIndex);
				}

				if (hyperlinkReference.startsWith("/")) {
					hyperlinkReference = removeExtension(hyperlinkReference);
				} else if (hyperlinkReference.startsWith("#")) {
					hyperlinkReference = filepath.toString(); // only and anchor
																// so remove
																// because
																// adding back
																// later

				} else if (!hyperlinkReference
						.matches(".*[.](com|org|net|mil|edu|info|gov|edu|us|biz|kr|de)[/]*.*")) {
					// not an external URL
					hyperlinkReference = filepath
							+ removeExtension(hyperlinkReference);

				}

				// append any anchors
				if (hashIndex != -1) {
					String noSlash = hyperlinkReference.replaceAll("[/]$", "");
					hyperlinkReference = noSlash + hashAnchor;
				}

			} else if (hyperlinkReference.isEmpty()) {
				// handle anchors
				// drupal requires id and name attributes to be set and match.
				// Most of our anchors just have the name attr set
				String id = anchor.attr("id");
				String name = anchor.attr("name");
				if (id.isEmpty() && !name.isEmpty()) {
					anchor.attr("id", name);
				}
				// System.out.println("Anchor Tag: " + anchor);

			}

			// System.out.println("Relative File Path: " + filepath); //debug
			//System.out.println(anchor.attr("href") + " --> " + hyperlinkReference); //debug
						
			// don't set the href if it is empty; likely an anchor
			if (!anchor.attr("href").isEmpty()) {
				anchor.attr("href", hyperlinkReference);
			}
			
			//if new URL alias ends with a /, get rid of it
			if(anchor.attr("href").endsWith("/")) {
				String minusSlash = anchor.attr("href");
				minusSlash = minusSlash.substring(0, minusSlash.length()-1);
				anchor.attr("href", minusSlash);
			}

			filepath = originalFilePath.toString(); // may change with ../..
			

			// see if anchor is relative bath
			// see if anchor has a anchor #
			// see if abosulute
			// see if external site
			// see if link to file or a webpage
			
			//System.out.println("LINK: " + originalReference + " -> " + anchor.attr("href") + " in " + filepath);
			//System.out.println(anchor.attr("href")  + " in " + filepath);
		}

		return main;
	}

	/*
	 * Update a links to files and move to their new Drupal location Need to rename
	 * links to where they will be in Drupal
	 * 
	 * i.e.<a href="/about/images/Timeline_large.jpg">
	 * <img src="/sites/default/files/about/images/Timeline.jpg"></a>
	 * 
	 * 
	 * Cannot have relative URLs
	 * 
	 * href look like: /images/highlights_bottom.gif (absolute) imgs/blue.jpg
	 * (relative) ../images/energyAndEnvironment.jpg (annoying)
	 * ../../../images/highlights/alice.jpg (even more annoying)
	 * http://www.oar.net/services/conferencerooms/images/baleconference_1.jpg
	 * (external)
	 */
	public static Element updateAnchorFileLinks(Element main, String filepath) {
		
		//System.out.println("File: " + filepath);
		String orig = filepath;		//file we are processing
		
		// filepath = filepath.replaceFirst("[^/]*$", ""); //remove everything
		// until first / encountered
		String originalFilePath = filepath.toString(); // may chance with ../../
		//System.out.println(filepath);

		// get all <a> on the page
		Elements anchors = main.select("a");

		for (Element anchor : anchors) {

			String href = anchor.attr("href");
			String originalAnchor = anchor.attr("href");
			
			// make sure actually pointing somewhere and not external and not linking to webpage
			if (!(href.isEmpty() || isExternalSite(href) || href.matches(".*[.](shtml|html|htm)$"))) {
				 
				// TODO: Need to updated links like http://osc.edu/about/conferencerooms/index.shtml

				
				String filename = href.substring(href.lastIndexOf('/')+1, href.length());
				
				//if the file location has one of these extentions
				String pattern = ".*[.](pdf|jpg|JPG|mov|MOV|pptx|png|rel|ppt|mp3|txt|mpg|doc|zip|c|gz|swf|" +
						"tiff|wav|flv|rel|avi|xls|wmv|PDF|pptm|gif|XML|xml|tgz|asp|docx|exe|batch|php|ps|pbs" +
						"tif|eps)$";
				if(filename.matches(pattern) && filename.length() > 0) {
					//some src contain .. (i.e. going up a directory)
					//System.out.println(href + ": " + orig);
					//we now have correct full absolute file path.	
					
					String tempFullPath = "/files";	//begin w/ slash and none at ending
					
					//System.out.println("img src: " + tempFullPath + imgSrc); // debug
					//System.out.println(img.attr("src") + " --> " + imgSrc); //debug

					anchor.attr("href", tempFullPath + href);					//update the new path
					filepath = originalFilePath.toString(); 	//may change with ../..
					
					//if path has %20, replace with space
					href = href.replace("%20", " ");
					
					File source = new File("www" + href);
					File desc = new File("drupalFiles" + href);
					try {
					    FileUtils.copyFile(source, desc);
					} catch (IOException e) {
					    //e.printStackTrace();
						
						if(href.contains("archive")) {
							//System.out.println("archive: " + source.getPath());
						}
						
					    System.out.println("ERROR: Source file does not exist: " + source.getPath() + " in " + orig);
					}
					
					//debug for where new files are pointed at
					//System.out.println(originalAnchor + " -> " + anchor.attr("href"));

				}
			}			
		}

		return main;

	}
	
	/*
	 * Update img links and move to their new Drupal location Need to rename
	 * links to where they will be in Drupal
	 * 
	 * i.e.<img src="images/Timeline.jpg"/></a> Becomes: <img
	 * src="/about/images/Timeline.jpg"/></a> And put that file there
	 * 
	 * 
	 * Cannot have relative URLs, create directory if not exists.
	 * 
	 * SRC look like: /images/highlights_bottom.gif (absolute) imgs/blue.jpg
	 * (relative) ../images/energyAndEnvironment.jpg (annoying)
	 * ../../../images/highlights/alice.jpg (even more annoying)
	 * http://www.oar.net/services/conferencerooms/images/baleconference_1.jpg
	 * (external)
	 */
	public static Element updateImageSrc(Element main, String filepath) {
		
		//System.out.println("File with Images: " + filepath);
		
		String trueFilePath = filepath;
		
		filepath = filepath.replaceFirst("[a-z-]*[^/]", ""); 	//remove begin www
		filepath = filepath.replaceFirst("[^/]*$", "");			//remove the file extention
		
		// filepath = filepath.replaceFirst("[^/]*$", ""); //remove everything
		// until first / encountered
		String originalFilePath = filepath.toString(); // may chance with ../../
		// System.out.println(filepath);

		// get all <img> on the page
		Elements imgs = main.select("img");

		for (Element img : imgs) {

			String imgSrc = img.attr("src");

			// make sure actually pointing somewhere and not external
			if (!(imgSrc.isEmpty() || imgSrc.startsWith("http://"))) {
				//some src contain .. (i.e. going up a directory) 
				if(imgSrc.startsWith("..")) {
					//find absolute path for files like ../../../images/highlights/alice.jpg
					while(imgSrc.startsWith("..")) {
						imgSrc = imgSrc.replaceFirst("[.][.][/]", ""); //for begining of link
						filepath = filepath.replaceFirst("[^/]*/$", "");
					}
					filepath = filepath + imgSrc;	//new complete path to img
					
				} else if(!imgSrc.startsWith("/")) {
					//relative links imgs/blue.jpg
					filepath = filepath + imgSrc;	//new complete path to img
					
				} if (imgSrc.startsWith("/")) {
					//already absolute path
					filepath = imgSrc;
				}
				
				//we now have correct full absolute file path.
				imgSrc = filepath;
				
				//move file and get its new location
				imgSrc = moveFileToNewLocation(imgSrc, filepath, trueFilePath);				
			}
			
			//System.out.println("img src: " + tempFullPath + imgSrc);	// debug
			//System.out.println(img.attr("src") + " --> " + imgSrc);	//debug

			img.attr("src", imgSrc);					//update the new path
			filepath = originalFilePath.toString(); 	//may change with ../..

		}

		return main;

	}

	private static Document getFile(String location, Boolean local) {
		// Validate.isTrue(args.length == 1, "usage: supply url to fetch");
		// System.out.println("Getting links for: " + location);

		// check if we are a local or public site file
		String url = location;
		Document doc = null;
		if (local) {
			// local filesystem
			File input = new File(url);
			try {
				if (input.length() > 0) {
					// file is not empty
					doc = Jsoup.parse(input, "UTF-8");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			print("Fetching %s...", url);

			try {
				doc = Jsoup.connect(url).get();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Document title: " + doc.title());
		}

		Cleaner cleaner;
		Whitelist clean = Whitelist.relaxed();
		cleaner = new Cleaner(clean);

		// System.out.println(cleaner.clean(doc));

		return doc;

	}
	
	/*
	 * Tell is the site is internal or external
	 */
	
	private static boolean isExternalSite(String site) {
		
		// TODO: Need to updated links like http://osc.edu/about/conferencerooms/index.shtml
		
		boolean externalURL = site.matches(".*[.](com|org|net|mil|edu|info|gov|edu|us|biz|kr|de)[/]*.*");
		boolean ftpSite = site.startsWith("ftp://");
		
		return externalURL || ftpSite;
	}

	private static void print(String msg, Object... args) {
		//System.out.println(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}

	private static String removeExtension(String path) {

		// need to preserve the #

		// we only want to remove extensions to web pages, not links to .pdf,
		// .jpg, .etc

		String newPath = path;

		// we only want to remove extensions to web pages, not links to .pdf,
		// .jpg, .etc
		if (newPath.matches(".*(.shtm|.html|.htm).*")) {
			// remove any file extensions or if its an index
			if (newPath.contains(".")) {
				newPath = newPath.replaceFirst(".[a-zA-Z0-9#]*$", "");
			}

			if (newPath.endsWith("index")) {
				// strip index
				newPath = newPath.replaceFirst("index$", "");
				newPath = newPath.replaceFirst("[/]$", "");

			}
		}
		return newPath; // dont want starting /
	}
	
	
	/*
	 * Move the file and return its new location.
	 */
	private static String moveFileToNewLocation(String imgSrc, String filepath, String filename) {
		
		File source = new File("www" + imgSrc);
		File desc = new File("drupalFiles" + imgSrc);
		try {
		    FileUtils.copyFile(source, desc);
		} catch (IOException e) {
		    //e.printStackTrace();
			
			if(imgSrc.contains("archive")) {
				//System.out.println("archive: " + imgSrc);
			}
									
		    System.out.println("ERROR: Source file does not exist: " + source.getPath() + " in " + filename);
		}	
		
		//now give it drupal's file path
		String tempFullPath = "/files";
		imgSrc = tempFullPath + filepath;
		
		return imgSrc;	//new location
		
	}
	
	/*
	 * Update attribute values for CSV File
	 * 
	 * tile="example" needs to become 
	 */
	

}
