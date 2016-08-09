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
import javax.imageio.ImageIO;

public class CombineL2Spectrometer {
  public static void main(String[] argv) throws java.io.IOException {
    new CombineL2Spectrometer().mainInstance(argv);
  }

  public void mainInstance(String[] argv) throws java.io.IOException {
    if (argv.length != 3) {
      System.err.println("1st arg should be directory containing *.dat files");
      System.err.println("2nd arg should be directory containing *.dat.png files");
      System.err.println("3rd arg should be .png to create");
      System.exit(1);
    }
    File datDir = new File(argv[0]);
    File datPngDir = new File(argv[1]);
    File outputFile = new File(argv[2]);

    File[] hdrFiles = datDir.listFiles();
    if (hdrFiles.length == 0) {
      throw new RuntimeException("No files in " + datDir);
    }
    List<Header> headers = new ArrayList<Header>();
    for (File hdrFile : hdrFiles) {
      int inputWidth = 0;
      int inputHeight = 0;
      float northing = 0.0f;
      float easting = 0.0f;
      if (hdrFile.getName().endsWith("hdr")) {
        Header header = new Header();
        header.hdrFile = hdrFile;
        BufferedReader stream =
          new BufferedReader(new InputStreamReader(new FileInputStream(hdrFile)));
        while (true) {
          String line = stream.readLine();
          if (line == null) {
            break;
          }

          if (line.startsWith("samples")) {
            header.inputWidth = Integer.parseInt(line.split(" = ")[1]);
          } else if (line.startsWith("lines")) {
            header.inputHeight = Integer.parseInt(line.split(" = ")[1]);
          } else if (line.startsWith("map info")) {
            String[] parts = line.split(",");
            header.easting  = Float.parseFloat(parts[3]);
            header.northing = Float.parseFloat(parts[4]);
            if (line.contains("rotation=")) {
              header.rotation =
                Float.parseFloat(line.split("rotation=")[1].split("}")[0]);
            }
          }
        } // next line
        headers.add(header);
      } // end if
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
      File datPngFile = new File(datPngDir,
        header.hdrFile.getName().replace(".hdr", ".dat.png"));
      System.err.println("Reading " + datPngFile);
      BufferedImage shrunkenImage = ImageIO.read(datPngFile);
      if (shrunkenImage == null) {
        throw new RuntimeException("Got null from ImageIO.read of " + datPngFile);
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
  }

  class Header {
    File hdrFile;
    int inputWidth;
    int inputHeight;
    float northing;
    float easting;
    float rotation;
    public String toString() {
      return hdrFile.getName();
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
