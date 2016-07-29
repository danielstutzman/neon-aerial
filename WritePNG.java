import ij.ImagePlus;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.Color;
import java.awt.Graphics2D;
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

public class WritePNG {
  private static int TO_WIDTH    = 100;
  private static int TO_HEIGHT   = 100;

  public static void main(String[] argv) {
    if (argv.length < 2) {
      System.err.println("1st arg: directory to read .tif files from");
      System.err.println("2nd arg: FROM_WIDTH");
      System.err.println("3rd arg: FROM_HEIGHT");
      System.err.println("4th arg: output .png");
      System.err.println("5th arg: .tiff library (jai or imagej)");
      System.exit(1);
    }
    File inputDirectory = new File(argv[0]);
    int FROM_WIDTH = Integer.parseInt(argv[1]);
    int FROM_HEIGHT = Integer.parseInt(argv[2]);
    File outputPng = new File(argv[3]);
    String tiffLibrary = argv[4];

    Pattern pattern = Pattern.compile(
      "(201[34]_[A-Z]{4}_[0-9])_([0-9]{3})000_([0-9]{4})000_(.*).tif");
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
      if (file.getName().endsWith(".tif")) {
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        String firstPart = matcher.group(1);
        int east = Integer.parseInt(matcher.group(2));
        int nort = Integer.parseInt(matcher.group(3));
        String lastPart = matcher.group(4);

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
      if (file.getName().endsWith(".tif")) {
        System.out.println(file.getName());
        i += 1;

        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        String firstPart = matcher.group(1);
        int east = Integer.parseInt(matcher.group(2));
        int nort = Integer.parseInt(matcher.group(3));
        String lastPart = matcher.group(4);

        BufferedImage image;
        if (tiffLibrary.equals("jai")) {
          try {
            image = ImageIO.read(new File(file.getAbsolutePath()));
          } catch (java.io.IOException e) {
            throw new RuntimeException(e);
          }
        } else if (tiffLibrary.equals("imagej")) {
          ImagePlus imagePlus = new ImagePlus(file.getAbsolutePath());
          float[] pixels = (float[])imagePlus.getProcessor().getPixels();
          for (int j = 0; j < pixels.length; j++) {
            if (pixels[j] == -9999.0f) {
              pixels[j] = 0.0f;
            }
            //System.out.print(pixels[j] + " ");
          }
          image = imagePlus.getProcessor().getBufferedImage();
        } else {
          throw new RuntimeException("Unknown tiffLibrary " + tiffLibrary);
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
