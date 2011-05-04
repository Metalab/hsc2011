package org.metalab.ygor.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.metalab.ygor.YgorException;

public class YgorResult {
  public enum ResultType {
    RESULT_SET, UPDATE_COUNT
  };

  private ResultType type = null;
  private ResultSet rs = null;
  private int updateCount = -1;
  private boolean first = true;
  private String[] columnNames;
  private boolean open = true;

  public YgorResult(int updateCount) {
    this.updateCount = updateCount;
    this.type = ResultType.UPDATE_COUNT;
    this.columnNames = new String[] { "Updated" };
  }

  public YgorResult(ResultSet rs) throws SQLException {
    this.rs = rs;
    this.type = ResultType.RESULT_SET;
    ResultSetMetaData rsmd = rs.getMetaData();
    this.columnNames = new String[rsmd.getColumnCount()];

    for (int i = 0; i < columnNames.length; i++) {
      this.columnNames[i] = rsmd.getColumnName(i + 1);
    }
  }

  public boolean isOpen() {
    return open;
  }

  public ResultType resultType() {
    return this.type;
  }

  public int columnCount() {
    return columnNames.length;
  }

  public String[] columNames() {
    return columnNames;
  }

  public String getString(String name) {
    if (resultType() == ResultType.RESULT_SET) {
      try {
        return rs.getString(name);
      } catch (SQLException e) {
        return null;
      }
    } else {
      Integer i = getInteger(name);
      return i == null ? null : String.valueOf(i);
    }
  }

  public Integer getInteger(String name) {
    if (resultType() == ResultType.RESULT_SET)
      try {
        return rs.getInt(name);
      } catch (SQLException e) {
        return null;
      }
    else if (columnNames[0].equals(name))
      return updateCount;
    else
      throw new YgorException("Unknown column name: " + name);
  }
  
  public boolean next() {
    if (resultType() == ResultType.RESULT_SET) {
      try {
        return open = rs.next();
      } catch (SQLException e) {
        throw new YgorException("Unable to get next result", e);
      }
    }
    else if (first) {
      first = !first;
      return true;
    } else
      return open = false;
  }

  protected void close() {
    if (resultType() == ResultType.RESULT_SET) {
      try {
        rs.close();
      } catch (SQLException e) {
        throw new YgorException("Unable to get next result", e);
      }
    }
    open = false;
  }
}
