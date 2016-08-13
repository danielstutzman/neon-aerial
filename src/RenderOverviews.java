import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

public class RenderOverviews {
  private static Pattern L3_CAMERA_PATTERN = Pattern.compile(
    "(201[3-5]_[A-Z]{4}_(v0)?[0-9])_([0-9]{6})_([0-9]{7})_(.*).tif");

  private static short ModelTiepointTag   = (short)33922;
  private static short ModelPixelScaleTag = (short)33550;
  private static short GeoKeyDirectoryTag = (short)34735;
  private static short ProjectedCSTypeGeoKey = 3072;

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
        if (!siteDir.getName().equals("Field_Data")) {
          System.err.println(siteDir);
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
    if (l3CameraDir.exists() && !siteName.equals("SJER")) {
      return renderL3CameraDir(l3CameraDir, siteName);
    } else {
      return new ArrayList<Layer>();
    }
  }

  private static List<Layer> renderL3CameraDir(File dir, String siteName) {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new RuntimeException("Got null from listFiles of " + dir);
    }

    int allMinNorthing = Integer.MAX_VALUE;
    int allMaxNorthing = Integer.MIN_VALUE;
    int allMinEasting = Integer.MAX_VALUE;
    int allMaxEasting = Integer.MIN_VALUE;
    Short allEpsgProjection = null;
    File allEpsgProjectionFromFile = null;
    for (File tiffFile : files) {
      if (tiffFile.getName().endsWith(".tif") &&
         !tiffFile.getName().endsWith("verview.tif")) {
        Matcher matcher = L3_CAMERA_PATTERN.matcher(tiffFile.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + tiffFile.getName() +
            " doesn't match pattern " + L3_CAMERA_PATTERN);
        }
        String firstPart = matcher.group(1);
        int fileMinEasting = Integer.parseInt(matcher.group(3));
        int fileMinNorthing = Integer.parseInt(matcher.group(4));
        int fileMaxEasting = fileMinEasting + 1000;
        int fileMaxNorthing = fileMinNorthing + 1000;
        String lastPart = matcher.group(5);

        TIFFTagger.TIFFTag[] ttags;
        try {
          ttags = new TIFFTagger(tiffFile.getAbsolutePath()).getTIFFInfo();
        } catch (java.io.IOException e) {
          throw new RuntimeException("Error from getTIFFInfo on " + tiffFile, e);
        }

        if (ttags == null) {
          System.err.println("Got null TIFF tags from " + tiffFile);
          continue;
        }

        Double fileMinEastingFromTag = null;
        Double fileMaxNorthingFromTag = null;
        Short fileEpsgProjection = null;
        for (TIFFTagger.TIFFTag tag : ttags) {
          if (tag.tag == ModelTiepointTag) {
            if (tag.count != 6) {
              throw new RuntimeException("Expected tag.count of 6 in " + tiffFile +
                "for ModelTiepointTag but got " + tag.count);
            }
            double[] doubles = (double[])tag.data;
            if (doubles[0] != 0.0 ||
                doubles[1] != 0.0 ||
                doubles[2] != 0.0 ||
                doubles[5] != 0.0) {
              throw new RuntimeException("Expected zeros in tag.data [0, 1, 2, 5] " +
                " in " + tiffFile + "for ModelTiepointTag");
            }
            fileMinEastingFromTag = doubles[3];
            fileMaxNorthingFromTag = doubles[4];
          } else if (tag.tag == ModelPixelScaleTag) {
            if (tag.count != 3) {
              throw new RuntimeException("Expected tag.count of 3 in " + tiffFile +
                "for ModelPixelScaleTag but got " + tag.count);
            }
            double[] doubles = (double[])tag.data;
            if (doubles[0] != 0.25 || doubles[1] != 0.25 || doubles[2] != 0.0) {
              throw new RuntimeException("Unexpected ModelPixelScaleTag values in " +
                tiffFile);
            }
          } else if (tag.tag == GeoKeyDirectoryTag) {
            short[] shorts = (short[])tag.data;
            for (int i = 4; i < shorts.length; i += 4) {
              short keyID = shorts[i];
              short tiffTagLocation = shorts[i + 1];
              short count = shorts[i + 2];
              short valueOffset = shorts[i + 3];
              if (keyID == ProjectedCSTypeGeoKey) {
                if (tiffTagLocation == 0) {
                  short epsgProjection = valueOffset;
                  if (allEpsgProjection == null ||
                      allEpsgProjection == epsgProjection) {
                    allEpsgProjection = epsgProjection;
                    allEpsgProjectionFromFile = tiffFile;
                  } else {
                    throw new RuntimeException("Differing projections: " +
                      allEpsgProjectionFromFile + " had " + allEpsgProjection +
                      " but " + tiffFile + " has " + epsgProjection);
                  }
                } else {
                  throw new RuntimeException("Expected immediate value for " +
                    "ProjectedCSTypeGeoKey in GeoKeyDirectoryTag in " + tiffFile);
                }
              }
            }
          }
        }

        if (fileMinEastingFromTag == null) {
          throw new RuntimeException("Couldn't find fileMinEastingFromTag");
        }
        if (fileMaxNorthingFromTag == null) {
          throw new RuntimeException("Couldn't find fileMaxNorthingFromTag");
        }
        if (allEpsgProjection == null) {
          throw new RuntimeException("Couldn't find allEpsgProjection");
        }

        if (fileMinEasting != fileMinEastingFromTag ||
            fileMaxNorthing != fileMaxNorthingFromTag) {
          throw new RuntimeException(
            "Filename and TIFF tags disagree about position: " +
            "filename is " + tiffFile + " but TIFF tags say easting=" +
            fileMinEastingFromTag + ",northing=" + fileMaxNorthingFromTag);
        }

        if (fileMinEasting < allMinEasting) {
          allMinEasting = fileMinEasting;
        }
        if (fileMaxEasting > allMaxEasting) {
          allMaxEasting = fileMaxEasting;
        }
        if (fileMinNorthing < allMinNorthing) {
          allMinNorthing = fileMinNorthing;
        }
        if (fileMaxNorthing > allMaxNorthing) {
          allMaxNorthing = fileMaxNorthing;
        }
      }
    }

    Layer newLayer = new Layer();
    newLayer.layerType = "combine_l3_camera";
    newLayer.siteName = siteName;
    newLayer.projection = "EPSG:" + allEpsgProjection;
    if (newLayer.projection == null) {
      throw new RuntimeException("Don't know projection for site name " + siteName);
    }
    newLayer.minEasting = allMinEasting;
    newLayer.maxEasting = allMaxEasting;
    newLayer.minNorthing = allMinNorthing;
    newLayer.maxNorthing = allMaxNorthing;
    return Collections.singletonList(newLayer);
  }
}
