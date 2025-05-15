package modelserver;

import java.util.*;

public class URIParser {
  private String uri;
  private List<String> path;

  public URIParser(String uri) {
    this.uri = uri;

    path = new ArrayList<>();

    if (uri.contains("?") && uri.indexOf("?") < uri.length() - 1) {
      String pathStr = uri.substring(1, uri.indexOf("?"));
      String paramsStr = uri.substring(uri.indexOf("?") + 1);

      path.addAll(Arrays.asList(pathStr.split("/")));

    } else if (uri.contains("?")) {
      String pathStr = uri.substring(1, uri.indexOf("?"));
      path.addAll(Arrays.asList(pathStr.split("/")));
    } else {
      String pathStr = uri.substring(1);
      path.addAll(Arrays.asList(pathStr.split("/")));
    }
  }

  public List<String> getPath() {
    return path;
  }
}