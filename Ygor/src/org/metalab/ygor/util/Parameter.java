package org.metalab.ygor.util;

import java.io.File;

public class Parameter {

	public static enum TYPES {
	  BOOLEAN(Boolean.class),
	  INTEGER(Integer.class),
	  LONG(Long.class),
	  FLOAT(Float.class),
	  DOUBLE(Double.class),
	  STRING(String.class),
	  FILE(File.class);
	  
	  public Class mappedClass;
	  
	  TYPES(Class c) {
	    this.mappedClass = c;
	  }
	}

	private TYPES type;
	private String name;
	private boolean allowNull = false;

	public Parameter(String name, TYPES type, boolean allowNull) {
		this.name = name;
		this.type = type;
		this.allowNull = allowNull;
	}
	
	public Parameter(String name, TYPES type) {
	  this(name, type, false);
  }

	public TYPES type() {
		return type;
	}

	public String name() {
		return name;
	}

	public boolean allowNull() {
		return allowNull;
	}

	public Object typeIt(Object val) {
		if (val == null) {
			if (allowNull())
				return null;
			else
				throw new NullPointerException(
						"Null not allowed for parameter: " + name());
		}
		
		if (isTyped(val))
			return val;

		Exception type_ex = null;
		if (val instanceof String) {
			String strVal = val.toString();

			try {
				switch (type) {
				case BOOLEAN:
					return Boolean.parseBoolean(strVal);
				case INTEGER:
					return Integer.parseInt(strVal);
				case LONG:
					return Long.parseLong(strVal);
				case FLOAT:
					return Float.parseFloat(strVal);
				case DOUBLE:
					return Double.parseDouble(strVal);
				case FILE:
					return new File(strVal);
				case STRING:
					return strVal;

				default:
				}
			} catch (Exception e) {
				type_ex = e;
			}
		}
		throw new IllegalArgumentException("Type does not match for " + name
				+ "=" + val + ". " + type.mappedClass.getName()
				+ " expected", type_ex);
	}

	public boolean isTyped(Object val) {
		return val.getClass().isInstance(type.mappedClass);
	}

	public String toString() {
		return '(' + type.mappedClass.getName() + ')' + name();
	}
}
