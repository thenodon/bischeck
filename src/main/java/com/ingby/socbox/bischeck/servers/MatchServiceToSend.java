package com.ingby.socbox.bischeck.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to check if a string match a specific pattern. The pattern
 * used can be defined as a string or as a list of string to match against.
 * When a string is matched by the isMatch its stored in a cache so it do not 
 * need to execute the matching next time.    
 * @author andersh
 *
 */
public class MatchServiceToSend {

	private Map<String,Boolean> matchedstr = new HashMap<String, Boolean>();
	private List<Pattern> pats = new ArrayList<Pattern>();
	
	
	/**
	 * Take a string with multiple regex that is separated with a delimiter like
	 * <br>{@code pattern1%pattern2}<br>
	 * where the delimiter is % and create a {@link List}&lt;{@link String}&gt;
	 * <br>
	 * The method can be used as a convenient method in the constructor 
	 * {@link MatchServiceToSend#MatchServiceToSend(List)}    
	 * @param strlist a string with multiple 
	 * @param delim a string to seperat the patterns used to match 
	 * @return the {@link List} of patterns as {@link String}
	 */
	public static List<String> convertString2List(final String strlist, final String delim ) {
		StringTokenizer st = new StringTokenizer(strlist, delim);
		List<String> list = new ArrayList<String>();
		
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}
	
	
	/**
	 * Create MatchServiceToSend by a pattern string
	 * @param pattern the string that is a regex to match against
	 */
	public MatchServiceToSend(final String pattern) {
		pats.add(Pattern.compile(pattern));
	}
	
	
	/**
	 * Create MatchServiceToSend by a list of pattern strings
	 * @param pattern a list of strings that is a regex to match against
	 */
	public MatchServiceToSend(final List<String> pattern) {
		for (String pat: pattern) {
			pats.add(Pattern.compile(pat));
		}
	}
	
	
	/**
	 * Check if the matchit string is matched by any of the patterns defined in
	 * the constructor. It will return true when it matched by the first 
	 * pattern. 
	 * @param matchit the string to match againt patterns
	 * @return true if matched by any pattern or false if not matched
	 */
	public boolean isMatch(final String matchit) {
		Boolean matched = null;
		if (matchedstr.containsKey(matchit)) {
			// Check if the message already matched
			matched =matchedstr.get(matchit);
		} else {
			// Match message and put status in cache
			matched = false;
			for (Pattern pat: pats) {
				final Matcher mat = pat.matcher(matchit);
				if (mat.find()) {
					matched = true;
					break;
				}
			} 
			matchedstr.put(matchit, matched);
		}
		return matched;
	}
	
}
