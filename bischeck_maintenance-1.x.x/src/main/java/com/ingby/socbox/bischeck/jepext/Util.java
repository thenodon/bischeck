package com.ingby.socbox.bischeck.jepext;

import java.util.Iterator;
import java.util.Stack;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public class Util {

	/**
	 * Check if remove of Null objects,notFullListParse, is set by the
	 * ConfigurationManager
	 * @return true if set
	 */
	public static boolean getSupportNull() {
		try {
			if (ConfigurationManager.getInstance().getProperties().
					getProperty("notFullListParse","false").equalsIgnoreCase("true")) {
				return true;
			} else {
				return false;
			}
		} catch (NullPointerException ne) {
			return false;
		}
	}


	/**
	 * Remove every stack object that is of class Null
	 * @param stack
	 * @return the number of items removed from the stack
	 */
	@Deprecated
	public static int deleteNullFromStack(Stack<Object> stack) {
		int deletedNulls = 0;

		Iterator<Object> itr = stack.iterator();

		while (itr.hasNext()) {
			Object obj = itr.next();

			if (obj instanceof Null) {
				itr.remove();
				deletedNulls++;
			}
		}

		return deletedNulls;
	}
}
