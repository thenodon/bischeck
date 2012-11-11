package com.ingby.socbox.bischeck.jepext;

import java.util.Iterator;
import java.util.Stack;

public class Util {
	
	public static int deleteNullFromStack(Stack<Object> stack) {
		Iterator<Object> itr = stack.iterator();
		int countNull = 0;
		while (itr.hasNext()) {
			Object obj = itr.next();
			if (obj instanceof Null) {
				countNull++;
				itr.remove();
			}
		}
		return countNull;
	}
}
