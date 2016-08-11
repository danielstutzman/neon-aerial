import ij.ImagePlus;
import ij.process.FloatProcessor;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class CombineL3Lidar {
  private static int TO_WIDTH  = 100;
  private static int TO_HEIGHT = 100;

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
      "(201[3-5]_[A-Z]{4}_[0-9](_v01)?)_([0-9]{3})000_([0-9]{4})000_(.*).tif");
    // "Nort" means UTM Zone 18N northing (divided by 1000 to change meters to km)
    // "East" means UTM Zone 18N easting  (divided by 1000 to change meters to km)
    int minNort = Integer.MAX_VALUE;
    int maxNort = Integer.MIN_VALUE;
    int minEast = Integer.MAX_VALUE;
    int maxEast = Integer.MIN_VALUE;
    float minValue = Float.POSITIVE_INFINITY;
    float maxValue = Float.NEGATIVE_INFINITY;
    Map<String, FloatProcessor> filenameToImagePlus =
      new HashMap<String, FloatProcessor>();

    System.err.println("Listing " + inputDirectory + "...");
    File[] files = inputDirectory.listFiles();
    if (files == null) {
      throw new RuntimeException(
          "listFiles() for " + inputDirectory + " returned null");
    }

    for (File file : files) {
      if (file.getName().endsWith(".tif") &&
         !file.getName().endsWith("verview.tif") &&
         !file.getName().startsWith(".")) {
        System.out.println(file.getName());

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

        ImagePlus imagePlus = new ImagePlus(file.getAbsolutePath());
        float[] pixels = (float[])imagePlus.getProcessor().getPixels();
        for (int j = 0; j < pixels.length; j++) {
          float value = pixels[j];
          if (value != -9999.0f) {
            if (value < minValue) {
              minValue = value;
            }
            if (value > maxValue) {
              maxValue = value;
            }
          }
        }
        FloatProcessor shrunken =
          (FloatProcessor)imagePlus.getProcessor().resize(TO_WIDTH, TO_HEIGHT);
        filenameToImagePlus.put(file.getName(), shrunken);
      }
    }
    System.out.println("easting: " + minEast + " to " + maxEast);
    System.out.println("northing: " + minNort + " to " + maxNort);
    System.out.println("value: " + minValue + " " + maxValue);

    System.err.println("Drawing to image");
    BufferedImage out = new BufferedImage(
      TO_WIDTH * (maxEast - minEast + 1),
      TO_HEIGHT * (maxNort - minNort + 1),
      BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setColor(Color.gray);
    g.fillRect(0, 0, out.getWidth(), out.getHeight());

    for (File file : inputDirectory.listFiles()) {
      if (file.getName().endsWith(".tif") &&
         !file.getName().endsWith("verview.tif") &&
         !file.getName().startsWith(".")) {
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        String firstPart = matcher.group(1);
        int east = Integer.parseInt(matcher.group(3));
        int nort = Integer.parseInt(matcher.group(4));
        String lastPart = matcher.group(5);

        FloatProcessor processor = filenameToImagePlus.get(file.getName());
        processor.setMinAndMax(minValue, maxValue);
        BufferedImage image = processor.getBufferedImage();

        AffineTransform transform = new AffineTransform(); // identity transform
        int translateX = TO_WIDTH * (east - minEast);
        int translateY = TO_HEIGHT * -(nort - minNort) +
          (TO_HEIGHT * (maxNort - minNort));
        transform.translate(translateX, translateY);
        g.drawRenderedImage(image, transform);

        g.setColor(Color.white);
        g.drawRect(translateX, translateY, TO_WIDTH, TO_HEIGHT);

        g.drawString("" + nort + "km N", translateX + 1, translateY + TO_HEIGHT - 11);
        g.drawString("" + east + "km E", translateX + 1, translateY + TO_HEIGHT - 1);
      } // end if ends with .tif
    } // loop to next file

    System.err.println("Writing PNG");
    try {
      ImageIO.write(out, "PNG", outputPng);
    } catch (java.io.IOException e) {
      throw new RuntimeException("IOException from ImageIO.write", e);
    }
  }
}
