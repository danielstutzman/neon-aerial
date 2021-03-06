import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class CombineL1OrL2Spectrometer {
  public static void main(String[] argv) throws java.io.IOException {
    new CombineL1OrL2Spectrometer().mainInstance(argv);
  }

  public void mainInstance(String[] argv) throws java.io.IOException {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length != 2) {
      System.err.println("1st arg should be directory containing *.png files");
      System.err.println("2nd arg should be .png to create");
      System.exit(1);
    }
    File pngDir = new File(argv[0]);
    File outputFile = new File(argv[1]);

    File[] allFiles = pngDir.listFiles();
    if (allFiles == null) {
      throw new RuntimeException("listFiles returned null for " + pngDir);
    }

    List<File> pngFiles = new ArrayList<File>();
    for (File file : allFiles) {
      if (file.getName().endsWith(".png")) {
        pngFiles.add(file);
      }
    }
    if (pngFiles.size() == 0) {
      throw new RuntimeException("No .png files in " + pngDir);
    }

    List<Header> headers = new ArrayList<Header>();
    for (File pngFile : pngFiles) {
      Pattern pattern = Pattern.compile(
        "W([0-9]+)_H([0-9]+)_N([0-9.]+)_E([0-9.]+)_R(-?[0-9.]+).png");
      Matcher matcher = pattern.matcher(pngFile.getName());
      if (!matcher.matches()) {
        throw new RuntimeException(
          "Filename " + pngFile + " doesn't match regex " + pattern);
      }

      Header header = new Header();
      header.pngFile = pngFile;
      header.inputWidth = Integer.parseInt(matcher.group(1));
      header.inputHeight = Integer.parseInt(matcher.group(2));
      header.northing = Float.parseFloat(matcher.group(3));
      header.easting = Float.parseFloat(matcher.group(4));
      header.rotation = Float.parseFloat(matcher.group(5));
      headers.add(header);
    } // next file

    float minNorthing = Float.POSITIVE_INFINITY;
    float maxNorthing = Float.NEGATIVE_INFINITY;
    float minEasting  = Float.POSITIVE_INFINITY;
    float maxEasting  = Float.NEGATIVE_INFINITY;
    for (Header header : headers) {
      float angle = (float)(header.rotation * Math.PI / 180);
      float x0 = header.easting;
      float y0 = header.northing;
      XY corner1 = rotatePoint(header.easting + header.inputWidth,
        header.northing,
        x0, y0, angle);
      XY corner2 = rotatePoint(header.easting + header.inputWidth,
        header.northing - header.inputHeight,
        x0, y0, angle);
      XY corner3 = rotatePoint(header.easting,
        header.northing - header.inputHeight,
        x0, y0, angle);

      if (x0 < minEasting)        { minEasting = x0; }
      if (corner1.x < minEasting) { minEasting = corner1.x; }
      if (corner2.x < minEasting) { minEasting = corner2.x; }
      if (corner3.x < minEasting) { minEasting = corner3.x; }

      if (x0 > maxEasting)        { maxEasting = x0; }
      if (corner1.x > maxEasting) { maxEasting = corner1.x; }
      if (corner2.x > maxEasting) { maxEasting = corner2.x; }
      if (corner3.x > maxEasting) { maxEasting = corner3.x; }

      if (y0 < minNorthing)        { minNorthing = y0; }
      if (corner1.y < minNorthing) { minNorthing = corner1.y; }
      if (corner2.y < minNorthing) { minNorthing = corner2.y; }
      if (corner3.y < minNorthing) { minNorthing = corner3.y; }

      if (y0 > maxNorthing)        { maxNorthing = y0; }
      if (corner1.y > maxNorthing) { maxNorthing = corner1.y; }
      if (corner2.y > maxNorthing) { maxNorthing = corner2.y; }
      if (corner3.y > maxNorthing) { maxNorthing = corner3.y; }
    }

    // Add a small margin
    minEasting -= 100;
    maxEasting += 100;
    minNorthing -= 100;
    maxNorthing += 100;

    System.out.println("minEasting: " + minEasting);
    System.out.println("maxEasting: " + maxEasting);
    System.out.println("minNorthing: " + minNorthing);
    System.out.println("maxNorthing: " + maxNorthing);

    int outWidth = 4000;
    int outHeight = 4000;
    BufferedImage img =
      new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    g2d.scale(outWidth / (maxEasting - minEasting),
      outHeight / (maxNorthing - minNorthing));
    for (Header header : headers) {
      System.err.println("Reading " + header.pngFile);
      BufferedImage shrunkenImage;
      try {
        shrunkenImage = ImageIO.read(header.pngFile);
      } catch (javax.imageio.IIOException e) {
        System.err.println("Doesn't exist: " + header.pngFile);
        e.printStackTrace();
        continue;
      }
      if (shrunkenImage == null) {
        throw new RuntimeException("Got null from ImageIO.read of " + header.pngFile);
      }

      System.out.println(header.easting + " " + header.northing + " " +
        header.inputWidth + " " + header.inputHeight);
      g2d.setColor(Color.RED);
      float northwestX = (header.easting - minEasting);
      float northwestY = (maxNorthing - header.northing);
      float width = header.inputWidth;
      float height = header.inputHeight;
      g2d.translate(northwestX, northwestY);
      g2d.rotate(-Math.toRadians(header.rotation));
      AffineTransform transform = AffineTransform.getScaleInstance(
        width / shrunkenImage.getWidth(), height / shrunkenImage.getHeight());
      //transform.translate(-width/2 / transform.getScaleX(),
      //  -height/2 / transform.getScaleY());
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      g2d.drawRenderedImage(shrunkenImage, transform);
      //g2d.drawRect((int)(- width/2), (int)(- height/2), (int)width, (int)height);
      g2d.drawRect(0, 0, (int)width, (int)height);
      g2d.rotate(Math.toRadians(header.rotation));
      g2d.translate(-northwestX, -northwestY);
    }
    g2d.dispose();

    ImageIO.write(img, "PNG", outputFile);
    System.err.println("Wrote " + outputFile);
  }

  class Header {
    File pngFile;
    int inputWidth;
    int inputHeight;
    float northing;
    float easting;
    float rotation;
    public String toString() {
      return pngFile.getName();
    }
  }

  class XY {
    float x;
    float y;
  }

  private XY rotatePoint(float inX, float inY, float centerX, float centerY,
      float angle) {
    XY xy = new XY();
    xy.x = (float)(centerX + (inX - centerX) * Math.cos(angle) -
      (inY - centerY) * Math.sin(angle));
    xy.y = (float)(centerY + (inX - centerX) * Math.sin(angle) +
      (inY - centerY) * Math.cos(angle));
    return xy;
  }
}
