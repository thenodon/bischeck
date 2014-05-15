/*
#
# Copyright (C) 2010-2014 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
*/

package com.ingby.socbox.bischeck;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BischeckDecimal is a wrapper class to manage numeric data. The class support
 * a number of methods to get scale, precision, if the integer part is 0 etc. 
 * <br>
 * The class is immutable. 
 */
public class BischeckDecimal {


	private int nrOfIntegers = 0;
	private int nrOfDecimals = 0;
	private String strIntegers = null;
	private String strDecimals = null;
	private boolean integerZero = false;
	private boolean isNegative = false;   
	private String stringValue = null;
	private Integer significant = 0;
	final static BigDecimal MINUS = new BigDecimal("-1");

	/**
	 * Create BischeckDecimal from a Float
	 * @param value
	 */
	public BischeckDecimal(Float value) {
		this(value == null ? null : Float.toString(value));
	}


	/**
	 * Create BischeckDecimal from a String
	 * @param value
	 */
	public BischeckDecimal(String value) {
		
		if (value == null) {
			stringValue = null;
			//The other values according to default
		} else {
			
			String text = null;
			
			value = value.trim();
			
			if (value.charAt(0) == '-' ) {
				text = value.substring(1);
				isNegative  = true;
			} else {
				text = value;
			}

			if (text.contains("E")) {
				stringValue = (new BigDecimal(text)).toPlainString();
			} else {
				stringValue = text;
			}

			nrOfIntegers = stringValue.indexOf('.');

			// If its a decimal value or not
			if (nrOfIntegers != -1) { 
				nrOfDecimals = stringValue.length() - nrOfIntegers - 1;
			} else {
				nrOfDecimals = 0;
				nrOfIntegers = stringValue.length();
			}

			// If integer part is 0
			if (nrOfIntegers == 1 && stringValue.charAt(0) == '0')  {
				integerZero = true;
			}

			// Only one decimal and its 0 
			if (nrOfDecimals == 1 && stringValue.charAt(stringValue.length()-1) == '0')  {
				stringValue = stringValue.substring(0, stringValue.indexOf('.'));
				nrOfIntegers = stringValue.length();
				nrOfDecimals = 0;
			}

			if (integerZero) {
				strIntegers = "0";
			} else { 
				strIntegers = stringValue.substring(0,nrOfIntegers);
			}

			if (nrOfDecimals > 0) {
				strDecimals = stringValue.substring(stringValue.indexOf('.') + 1);
				for (int i = 0; i < this.nrOfDecimals; i++) {
					significant++;
					if (strDecimals.charAt(i) != '0') {
						break;
					} 

				}
			} else { 
				strDecimals = "";
				significant = 0;
			}
		}
	}


	/**
	 * Return the number of digits in the integer part of the numeric value.
	 * <p>
	 * Examples<br>
	 * <code>134.43</code> the method will return <code>3</code> 
	 * <br>
	 * <code>0.43</code> the method will return <code>0</code>
	 * @return
	 */
	public int getNrOfIntegers() {
		if (integerZero) {
			return 0;
		}
		return nrOfIntegers;
	}


	/**
	 * Return the precision of the numeric value
	 * <p>
	 * Example
	 * <br>
	 * <code>134.43</code> the method will return <code>5</code> 
	 * @return
	 */
	public int getPrecision() {
		if (integerZero) {
			return (1 + nrOfDecimals);
		}
		return (nrOfIntegers + nrOfDecimals);
	}


	/**
	 * Return the scale of the numeric value
	 * <p>
	 * Example
	 * <br>
	 * <code>134.43</code> the method will return <code>2</code> 
	 * @return
	 */
	public int getScale() {
		return nrOfDecimals;
	}

	/**
	 * Return the position of the first none 0 of decimal part of 
	 * the numeric value
	 * <p>
	 * Example
	 * <br>
	 * <code>134.0043</code> the method will return <code>3</code> 
	 * @return
	 */
	public int getDecimalSignificant() {
		return significant;
	}


	/**
	 * If the are no digits, except 0 in the integer part the method return true 
	 * <p>
	 * Example
	 * <br>
	 * <code>0.0043</code> the method will return <code>true</code> 
	 * @return
	 */
	public boolean isIntergerZero() {
		return integerZero;
	}


	/**
	 * If the numeric value is a negative number this method return true.
	 * @return
	 */
	public boolean isNegative() {
		return isNegative;
	}


	/**
	 * Return the numeric value as a {@link BigDecimal}
	 * @return
	 */
	public BigDecimal getBigDecimal() {
		if (isNegative) {
			return (new BigDecimal(stringValue)).multiply(MINUS);
		} else {
			return new BigDecimal(stringValue);
		}
	}


	/**
	 * Return the numeric value as a {@link Float}
	 * @return
	 */
	public Float getFloat() {
		if (isNegative) {
			return (new Float(stringValue) * (-1));
		} else {
			return new Float(stringValue);
		}
	}


	/**
	 * Return the numeric value as a {@link String}
	 * @return
	 */
	public String toString() {
		if (isNegative) {
			return ("-"+stringValue);
		} else {
			return stringValue;
		}
	}

	
	/**
	 * If the value is null this method return true else false
	 * @return
	 */
	public boolean isNull() {
		if (stringValue == null || stringValue.equalsIgnoreCase("null")) {
			return true;
		}
		return false;
	}
	/**
	 * Return the digits for the decimal part of the numeric value
	 * <p>
	 * Example
	 * <br>
	 * <code>0.0043</code> the method will return <code>0043</code> 
	 * <br>
	 * <code>100</code> the method will return an empty {@link String} 
	 * @return
	 */
	public String getDecimals() {
		return strDecimals;

	}

	/**
	 * Return the digits for the decimal part of the numeric value
	 * <p>
	 * Example
	 * <br>
	 * <code>0.0043</code> the method will return <code>0</code> 
	 * <br>
	 * <code>100.0043</code> the method will return <code>100</code> 
	 * @return
	 */
	public String getIntegers() {
		return strIntegers;

	}


	/**
	 * The method set the scale of the numeric value and round it using 
	 * {@link RoundingMode.HALF_DOWN}.
	 * <p>
	 * Example
	 * <br>
	 * The value <code>-13.10014</code> will scale <code>1.0001521</code> to <code>1.00015</code> 
	 * <br>
	 * The value <code>-13.1001432</code> will scale <code>1.0001521</code> to <code>1.0001521</code> 
	 * <br>
	 * The value <code>0.061</code> will scale <code>0.0001521</code> to <code>0.0002</code> 
	 * <br>
	 * The value <code>0.061</code> will scale <code>0.0001521</code> to <code>0.0002</code> 
	 * <br>
	 * The value <code>1.061</code> will scale <code>0.0001521</code> to <code>0.0002</code> 
	 * <br>
	 * The value <code>13.001</code> will scale <code>0.1521</code> to <code>0.152</code> 
	 * <br>
	 * The value <code>1.061</code> will scale <code>0.0001521</code> to <code>0.0002</code> 
	 * <br>
	 * The value <code>1.001E6</code> will scale <code>0.0001521</code> to <code>0.0002</code> 
	 * <br>
	 * The value <code>1.06</code> will scale <code>0.1581</code> to <code>0.16</code> 
	 * @param value
	 * @return
	 */
	public BischeckDecimal scaleBy(BischeckDecimal value) {        

		BischeckDecimal bisDec = null;

		if (value != null) {
			if (this.integerZero && value.getScale() <= this.getScale()) {
				int scaleIt = Math.max(value.getScale(), this.getDecimalSignificant());
				String  newValue = getBigDecimal().setScale(scaleIt, RoundingMode.HALF_DOWN).toPlainString();
				bisDec = new BischeckDecimal(newValue);                
			} else if (this.getScale() > value.getScale()) {
				String newValue = getBigDecimal().setScale(value.getScale(), RoundingMode.HALF_DOWN).toPlainString();
				bisDec = new BischeckDecimal(newValue);
			} else {
				bisDec = this;
			}

		} else {
			bisDec = this;
		}

		return bisDec;
	}
}
