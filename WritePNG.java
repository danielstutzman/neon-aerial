import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class WritePNG {
  private static int FROM_WIDTH  = 4000;
  private static int FROM_HEIGHT = 4000;
  private static int TO_WIDTH    = 100;
  private static int TO_HEIGHT   = 100;

  public static void main(String[] argv) {
    Pattern pattern = Pattern.compile(
      "2014_HARV_2_([0-9]{3})000_([0-9]{4})000_image.tif");
    File folder = new File("../HARV_L3_Camera");
    // "Nort" means UTM Zone 18N northing (divided by 1000 to change meters to km)
    // "East" means UTM Zone 18N easting  (divided by 1000 to change meters to km)
    int minNort = Integer.MAX_VALUE;
    int maxNort = Integer.MIN_VALUE;
    int minEast = Integer.MAX_VALUE;
    int maxEast = Integer.MIN_VALUE;
    for (File file : folder.listFiles()) {
      if (file.getName().endsWith(".tif")) {
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        int east = Integer.parseInt(matcher.group(1));
        int nort = Integer.parseInt(matcher.group(2));

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
      }
    }
    System.out.println(minEast + " " + minNort + " " + maxEast + " " + maxNort);

    BufferedImage out = new BufferedImage(
      TO_WIDTH * (maxEast - minEast),
      TO_HEIGHT * (maxNort - minNort),
      BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setColor(Color.gray);
    g.fillRect(0, 0, out.getWidth(), out.getHeight());

    int i = 0;
    for (File file : folder.listFiles()) {
      if (file.getName().endsWith(".tif")) {
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.matches()) {
          throw new RuntimeException("Filename " + file.getName() +
            " doesn't match pattern " + pattern);
        }
        int east = Integer.parseInt(matcher.group(1));
        int nort = Integer.parseInt(matcher.group(2));

        BufferedImage image;
        try {
          image = ImageIO.read(file);
        } catch (java.io.IOException e) {
          throw new RuntimeException(e);
        }

        AffineTransform transform = AffineTransform.getScaleInstance(
          (float)TO_WIDTH / FROM_WIDTH,
          (float)TO_HEIGHT / FROM_HEIGHT);
        int translateX = TO_WIDTH * (east - minEast);
        int translateY = TO_HEIGHT * -(nort - minNort) +
          (TO_HEIGHT * (maxNort - minNort));
        transform.translate(translateX * FROM_WIDTH / TO_WIDTH,
          translateY * FROM_HEIGHT / TO_HEIGHT);

        g.drawRenderedImage(image, transform);

        g.setColor(Color.white);
        g.drawRect(translateX, translateY, TO_WIDTH, TO_HEIGHT);

        g.drawString("" + nort + "km N", translateX + 1, translateY + TO_HEIGHT - 11);
        g.drawString("" + east + "km E", translateX + 1, translateY + TO_HEIGHT - 1);

        System.out.println(file.getName());
        i += 1;
        //if (i >= 3) {
        //  break;
        //}

        File f = new File("HARV_overview.png.tmp");
        try {
          ImageIO.write(out, "PNG", f);
        } catch (java.io.IOException e) {
          throw new RuntimeException("IOException from ImageIO.write", e);
        }

        File f2 = new File("HARV_overview.png");
        boolean didRenameSucceed = f.renameTo(f2);
        if (!didRenameSucceed) {
          throw new RuntimeException("Couldn't rename " + f + " to " + f2);
        }

      } // end if ends with .tif
    } // loop to next file
  }
}
