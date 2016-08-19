import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import javax.imageio.ImageIO;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;

public class ShrinkL1Spectrometer {
  private static final int WIDTH = 987;
  private static final int HEIGHT = 13303;

  public static void main(String[] argv) throws java.io.IOException {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length != 2) {
      System.err.println("1st argument is path to .h5 file to read");
      System.err.println("2nd argument is directory to put .png files to");
      System.exit(1);
    }
    File h5File = new File(argv[0]);
    File pngDir = new File(argv[1]);

    NetcdfFile ncfile = NetcdfFile.open(h5File.getAbsolutePath());

    String mapInfo = ncfile.findVariable("map_info").read().toString();
    String[] mapInfoValues = mapInfo.split(",");
    if (!mapInfoValues[0].equals("UTM") ||
        !mapInfoValues[1].equals("1") ||
        !mapInfoValues[2].equals("1")) {
      throw new RuntimeException("Expected UTM,1,1 in map_info[0-2]");
    }
    float easting = Float.parseFloat(mapInfoValues[3]);
    float northing = Float.parseFloat(mapInfoValues[4]);
    if (Float.parseFloat(mapInfoValues[5]) != 1.0f ||
        Float.parseFloat(mapInfoValues[6]) != 1.0f) {
      throw new RuntimeException("Expected 1.0,1.0 in map_info[5-6]");
    }
    String utmZoneNum = mapInfoValues[7];
    if (!mapInfoValues[8].equals("North") ||
        !mapInfoValues[9].equals("WGS-84") ||
        !mapInfoValues[10].equals("units=Meters")) {
      throw new RuntimeException(
          "Expected North,WGS-84,units=Meters in map_info[8-10]");
    }
    if (!mapInfoValues[11].startsWith("rotation=")) {
      throw new RuntimeException("Expected rotation=... in map_info[11]");
    }
    float rotation = Float.parseFloat(mapInfoValues[11].split("=")[1]);

    Variable reflectanceVar = ncfile.findVariable("Reflectance");
    for (int layerNum : new int[] { 100, 200, 300, 400 }) {
      Array data;
      try {
        data = reflectanceVar.read(
          new int[]{layerNum, 0, 0}, new int[] {1, HEIGHT, WIDTH});
      } catch (ucar.ma2.InvalidRangeException e) {
        throw new RuntimeException(e);
      }
      System.out.println(Arrays.toString(reflectanceVar.getShape()));
      int inputWidth = reflectanceVar.getShape()[2];
      int inputHeight = reflectanceVar.getShape()[1];
      short[] shorts = (short[])data.get1DJavaArray(short.class);

      short minValue = Short.MAX_VALUE;
      short maxValue = Short.MIN_VALUE;
      for (int i = 0; i < shorts.length; i++) {
        short value = shorts[i];

        if (value == 15000) {
          value = 0;
        }

        if (value < minValue) {
          minValue = value;
        }
        if (value > maxValue) {
          maxValue = value;
        }
      }

      byte[] bytes = new byte[shorts.length * 2];
      for (int i = 0; i < shorts.length; i++) {
        short value = shorts[i];
        if (value == 15000) {
          bytes[i * 2] = 0;
          bytes[i * 2 + 1] = 0;
        } else {
          bytes[i * 2] = (byte)(value * 255 / maxValue);
          bytes[i * 2 + 1] = (byte)255;
        }
      }

      ColorModel colorModel = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_GRAY),
        new int[] { 8, 8 }, true, true, Transparency.BITMASK, DataBuffer.TYPE_BYTE);
      SampleModel sm = colorModel.createCompatibleSampleModel(inputWidth, inputHeight);
      DataBuffer buffer = new DataBufferByte(bytes, bytes.length);
      WritableRaster raster = Raster.createWritableRaster(sm, buffer, null);
      BufferedImage image = new BufferedImage(colorModel, raster, false, null);

      File pngFile = new File(pngDir,
        "W" + WIDTH    + "_" +
        "H" + HEIGHT   + "_" +
        "N" + northing + "_" +
        "E" + easting  + "_" +
        "R" + rotation + "_" +
        "L" + layerNum + ".png");
      ImageIO.write(image, "png", pngFile);
      System.err.println("Wrote " + pngFile);
    } // next layerNum

    ncfile.close();
  } // end main
}
