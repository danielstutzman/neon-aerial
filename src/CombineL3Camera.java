import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class CombineL3Camera {
  private static int FROM_WIDTH  = 4000;
  private static int FROM_HEIGHT = 4000;
  private static int TO_WIDTH    =  100;
  private static int TO_HEIGHT   =  100;

  public static void main(String[] argv) {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length < 2) {
      System.err.println("1st arg: directory to read .tif files from");
      System.err.println("2nd arg: output .png");
      System.exit(1);
    }
    File inputDirectory = new File(argv[0]);
    File outputPng = new File(argv[1]);

    Pattern pattern = Pattern.compile(
      "(201[3-5]_[A-Z]{4}_(v0)?[0-9])_([0-9]{3})000_([0-9]{4})000_(.*).tif");
    // "Nort" means UTM Zone 18N northing (divided by 1000 to change meters to km)
    // "East" means UTM Zone 18N easting  (divided by 1000 to change meters to km)
    int minNort = Integer.MAX_VALUE;
    int maxNort = Integer.MIN_VALUE;
    int minEast = Integer.MAX_VALUE;
    int maxEast = Integer.MIN_VALUE;
    Set<String> tiffExists = new HashSet<String>();

    System.err.println("Listing " + inputDirectory + "...");
    File[] files = inputDirectory.listFiles();
    if (files == null) {
      throw new RuntimeException(
          "listFiles() for " + inputDirectory + " returned null");
    }

    for (File file : files) {
      if (file.getName().endsWith(".tif") &&
         !file.getName().endsWith("verview.tif")) {
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        String firstPart = matcher.group(1);
        int east = Integer.parseInt(matcher.group(3));
        int nort = Integer.parseInt(matcher.group(4));
        String lastPart = matcher.group(5);

        if (east < minEast) {
          minEast = east;
        }
        if (east > maxEast) {
          maxEast = east;
        }
        if (nort < minNort) {
          minNort = nort;
        }
        if (nort > maxNort) {
          maxNort = nort;
        }

        tiffExists.add(file.getName());
      }
    }
    System.out.println(minEast + " " + minNort + " " + maxEast + " " + maxNort);

    BufferedImage out = new BufferedImage(
      TO_WIDTH * (maxEast - minEast + 1),
      TO_HEIGHT * (maxNort - minNort + 1),
      BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setColor(Color.gray);
    g.fillRect(0, 0, out.getWidth(), out.getHeight());

    int i = 0;
    for (File file : inputDirectory.listFiles()) {
      if (file.getName().endsWith(".tif") &&
         !file.getName().endsWith("verview.tif")) {
        System.out.println(file.getName());
        i += 1;

        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        String firstPart = matcher.group(1);
        int east = Integer.parseInt(matcher.group(3));
        int nort = Integer.parseInt(matcher.group(4));
        String lastPart = matcher.group(5);

        BufferedImage image;
        try {
          image = ImageIO.read(file);
        } catch (java.lang.IllegalArgumentException e) {
          System.err.println(
            "Skipping " + file + " because of IllegalArgumentException");
          // java.lang.IllegalArgumentException: Empty region!
          // at javax.imageio.ImageReader.computeRegions(ImageReader.java:2702)
          // at javax.imageio.ImageReader.getDestination(ImageReader.java:2882)
          // at com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.read(TIFFImageReader.java:1154)
          // at javax.imageio.ImageIO.read(ImageIO.java:1448)
          // at javax.imageio.ImageIO.read(ImageIO.java:1308)
          e.printStackTrace();
          continue;
        } catch (java.io.IOException e) {
          throw new RuntimeException("IOException reading " + file, e);
        }

        if (file.getName().startsWith("LENO") && east > 387) {
          System.err.println("Skipping " + file + " since it's LENO east of 387000");
        }

        String tiffToTheEast =
          firstPart + "_" + (east + 1) + "000_" + nort + "000_" + lastPart + ".tif";
        float justifyX;
        if (tiffExists.contains(tiffToTheEast)) {
          justifyX = TO_WIDTH - (image.getWidth() * (float)TO_WIDTH / FROM_WIDTH);
        } else {
          justifyX = 0;
        }

        String tiffToTheSouth =
          firstPart + "_" + east + "000_" + (nort - 1) + "000_" + lastPart + ".tif";
        float justifyY;
        if (tiffExists.contains(tiffToTheSouth)) {
          justifyY = TO_HEIGHT - (image.getHeight() * (float)TO_HEIGHT / FROM_HEIGHT);
        } else {
          justifyY = 0;
        }

        AffineTransform transform = AffineTransform.getScaleInstance(
          (float)TO_WIDTH / FROM_WIDTH,
          (float)TO_HEIGHT / FROM_HEIGHT);
        int translateX = TO_WIDTH * (east - minEast);
        int translateY = TO_HEIGHT * -(nort - minNort) +
          (TO_HEIGHT * (maxNort - minNort));
        transform.translate((translateX + justifyX) * FROM_WIDTH / TO_WIDTH,
          (translateY + justifyY) * FROM_HEIGHT / TO_HEIGHT);

        g.drawRenderedImage(image, transform);

        g.setColor(Color.white);
        g.drawRect(translateX, translateY, TO_WIDTH, TO_HEIGHT);

        g.drawString("" + nort + "km N", translateX + 1, translateY + TO_HEIGHT - 11);
        g.drawString("" + east + "km E", translateX + 1, translateY + TO_HEIGHT - 1);

        File outputPngTmp = new File(outputPng.getName() + ".tmp");
        try {
          ImageIO.write(out, "PNG", outputPngTmp);
        } catch (java.io.IOException e) {
          throw new RuntimeException("IOException from ImageIO.write", e);
        }

        boolean didRenameSucceed = outputPngTmp.renameTo(outputPng);
        if (!didRenameSucceed) {
          throw new RuntimeException(
              "Couldn't rename " + outputPngTmp + " to " + outputPng);
        }
      } // end if ends with .tif
    } // loop to next file
  }
}
