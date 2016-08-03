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
    //File[] files = new File("/Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer").listFiles();
    File[] files = new File("/Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices").listFiles();
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
      int maxHeightOrWidth = (header.inputHeight > header.inputWidth) ?
        header.inputHeight : header.inputWidth;
      float y0 = header.northing - maxHeightOrWidth;
      float y1 = header.northing + maxHeightOrWidth;
      float x0 = header.easting - maxHeightOrWidth;
      float x1 = header.easting + maxHeightOrWidth;
      if (x0 < minEasting) { minEasting = x0; }
      if (x1 > maxEasting) { maxEasting = x1; }
      if (y0 < minNorthing) { minNorthing = y0; }
      if (y1 > maxNorthing) { maxNorthing = y1; }
    }

    int outWidth = 4000;
    int outHeight = 4000;
    BufferedImage img =
      new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    for (Header header : headers) {
      File pngFilename = new File(header.filename.replace(".hdr", ".dat.png"));
      System.err.println("Reading " + pngFilename);
      BufferedImage shrunkenImage = ImageIO.read(pngFilename);

      System.out.println(header.easting + " " + header.northing + " " +
        header.inputWidth + " " + header.inputHeight);
      g2d.setColor(Color.RED);
      float northwestX =
        (header.easting - minEasting) / (maxEasting - minEasting) * outWidth;
      float northwestY =
        (maxNorthing - header.northing) / (maxNorthing - minNorthing) * outHeight;
      float width = header.inputWidth / (maxEasting - minEasting) * outWidth;
      float height = header.inputHeight / (maxNorthing - minNorthing) * outHeight;
      g2d.translate(northwestX, northwestY);
      g2d.rotate(Math.toRadians(header.rotation));
      AffineTransform transform = AffineTransform.getScaleInstance(
        width / shrunkenImage.getWidth(), height / shrunkenImage.getHeight());
      //transform.translate(-width/2 / transform.getScaleX(),
      //  -height/2 / transform.getScaleY());
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      g2d.drawRenderedImage(shrunkenImage, transform);
      //g2d.drawRect((int)(- width/2), (int)(- height/2), (int)width, (int)height);
      g2d.drawRect(0, 0, (int)width, (int)height);
      g2d.rotate(-Math.toRadians(header.rotation));
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
  }
}





