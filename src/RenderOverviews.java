import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

public class RenderOverviews {
  private static Pattern L3_CAMERA_PATTERN = Pattern.compile(
    "(201[3-5]_[A-Z]{4}_(v0)?[0-9])_([0-9]{6})_([0-9]{7})_(.*).tif");

  public static void main(String[] argv) {
    if (argv.length != 1) {
      System.err.println("1st arg should be path of data volume " +
        "(e.g. /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a)");
      System.exit(1);
    }
    File dataDir = new File(argv[0]);

    List<Layer> layers = new ArrayList<Layer>();
    File[] domainDirs = dataDir.listFiles();
    if (domainDirs == null) {
      throw new RuntimeException("Got null from listFiles of " + dataDir);
    }
    for (File domainDir : domainDirs) {
      if (domainDir.isDirectory()) {
        List<Layer> newLayers = renderSitesForDomain(domainDir);
        layers.addAll(newLayers);
      }
    }

    try {
      BufferedWriter out = new BufferedWriter(new FileWriter("output/layers.json"));
      JSONArray layerObjects = new JSONArray();
      for (Layer layer : layers) {
        JSONObject layerObject = new JSONObject();
        layerObject.put("layerType", layer.layerType);
        layerObject.put("siteName", layer.siteName);
        layerObject.put("projection", layer.projection);
        layerObject.put("minNorthing", layer.minNorthing);
        layerObject.put("maxNorthing", layer.maxNorthing);
        layerObject.put("minEasting", layer.minEasting);
        layerObject.put("maxEasting", layer.maxEasting);
        layerObjects.put(layerObject);
      }
      layerObjects.write(out, 2, 0); // 2 spaces for indentation
      out.close();
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Layer> renderSitesForDomain(File domainDir) {
    List<Layer> layers = new ArrayList<Layer>();
    File[] siteDirs = domainDir.listFiles();
    if (siteDirs == null) {
      throw new RuntimeException("Got null from listFiles of " + domainDir);
    }
    for (File siteDir : siteDirs) {
      if (siteDir.isDirectory()) {
if (siteDir.getName().equals("LENO") || siteDir.getName().equals("HARV")) {
        List<Layer> newLayers = renderSiteDir(siteDir);
        layers.addAll(newLayers);
}
      }
    }
    return layers;
  }

  private static List<Layer> renderSiteDir(File siteDir) {
    List<Layer> layers = new ArrayList<Layer>();
    String siteName = siteDir.getName();
    File[] yearDirs = siteDir.listFiles();
    if (yearDirs == null) {
      throw new RuntimeException("Got null from listFiles of " + siteDir);
    }
    for (File yearDir : yearDirs) {
      if (yearDir.isDirectory()) {
        List<Layer> newLayers = renderYearDir(yearDir, siteName);
        layers.addAll(newLayers);
      }
    }
    return layers;
  }

  private static List<Layer> renderYearDir(File yearDir, String siteName) {
    File l3Dir = new File(yearDir, siteName + "_L3");
    File l3CameraDir = new File(l3Dir, siteName + "_Camera");
    return renderL3CameraDir(l3CameraDir, siteName);
  }

  private static List<Layer> renderL3CameraDir(File dir, String siteName) {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new RuntimeException("Got null from listFiles of " + dir);
    }

    int minNorthing = Integer.MAX_VALUE;
    int maxNorthing = Integer.MIN_VALUE;
    int minEasting = Integer.MAX_VALUE;
    int maxEasting = Integer.MIN_VALUE;
    for (File file : files) {
      if (file.getName().endsWith(".tif") &&
         !file.getName().endsWith("verview.tif")) {
        Matcher matcher = L3_CAMERA_PATTERN.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + L3_CAMERA_PATTERN);
        }
        String firstPart = matcher.group(1);
        int easting = Integer.parseInt(matcher.group(3));
        int northing = Integer.parseInt(matcher.group(4));
        String lastPart = matcher.group(5);

        if (easting < minEasting) {
          minEasting = easting;
        }
        if (easting + 1000 > maxEasting) {
          maxEasting = easting + 1000;
        }
        if (northing < minNorthing) {
          minNorthing = northing;
        }
        if (northing + 1000 > maxNorthing) {
          maxNorthing = northing + 1000;
        }
      }
    }

    Layer newLayer = new Layer();
    newLayer.layerType = "combine_l3_camera";
    newLayer.siteName = siteName;
    if (newLayer.siteName.equals("LENO")) {
      newLayer.projection = "EPSG:32616";
    } else if (newLayer.siteName.equals("HARV")) {
      newLayer.projection = "EPSG:32618";
    } else {
      throw new RuntimeException("Don't know projection for site name " +
        newLayer.siteName);
    }
    newLayer.minEasting = minEasting;
    newLayer.maxEasting = maxEasting;
    newLayer.minNorthing = minNorthing;
    newLayer.maxNorthing = maxNorthing;
    return Collections.singletonList(newLayer);
  }
}
