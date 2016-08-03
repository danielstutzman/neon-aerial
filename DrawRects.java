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

public class DrawRects {
  public static void main(String[] argv) throws java.io.IOException {
    new DrawRects().mainInstance(argv);
  }

  public void mainInstance(String[] argv) throws java.io.IOException {
    File[] files = new File("/Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D10/CPER/2013/CPER_L2/CPER_Spectrometer/Veg_Indices").listFiles();
    //File[] files = new File("/Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices").listFiles();
    List<Header> headers = new ArrayList<Header>();
    for (File file : files) {
      int inputWidth = 0;
      int inputHeight = 0;
      float northing = 0.0f;
      float easting = 0.0f;
      if (file.getName().endsWith("hdr")) {
        Header header = new Header();
        header.filename = file.getName();
        BufferedReader stream =
          new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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
      File pngFilename = new File(header.filename.replace(".hdr", ".dat.png"));
      System.err.println("Reading " + pngFilename);
      BufferedImage shrunkenImage = ImageIO.read(pngFilename);

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
    ImageIO.write(img, "PNG", new File("draw-rects.png"));
  }

  class Header {
    String filename;
    int inputWidth;
    int inputHeight;
    float northing;
    float easting;
    float rotation;
    public String toString() {
      return filename;
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
