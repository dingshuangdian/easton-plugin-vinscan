package vinscan;

import java.util.Map;

public class SaveDevcode {
  private Map<String,String> data;

  public Map<String,String> getData() {
    return data;
  }

  public void setData(Map<String,String> data) {
    this.data = data;
  }
  private SaveDevcode() {
  }
  private static SaveDevcode holder = null;
  public static SaveDevcode getInstance() {
    if (holder == null) {
      synchronized (SaveDevcode.class) {
        if (holder == null) {
          holder = new SaveDevcode();
        }
      }
    }
    return holder;
  }
}
