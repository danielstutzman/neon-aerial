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
  public static void main(String[] argv) throws java.io.IOException {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length <= 2) {
      System.err.println("1st argument is path to directory of .h5 files");
      System.err.println("2nd argument is directory to put .png files to");
      System.err.println("3rd-nth arguments are layer numbers (e.g. 50 100)");
      System.exit(1);
    }
    File h5Dir = new File(argv[0]);
    File pngDir = new File(argv[1]);
    int[] layerNums = new int[argv.length - 2];
    for (int i = 0; i < layerNums.length; i++) {
      layerNums[i] = Integer.parseInt(argv[i + 2]);
    }

    File[] h5Files = h5Dir.listFiles();
    if (h5Files == null) {
      throw new RuntimeException("listFiles for " + h5Dir + " returned null");
    }
    for (File h5File : h5Dir.listFiles()) {
      NetcdfFile ncfile = NetcdfFile.open(h5File.getAbsolutePath());
      handleH5File(ncfile, pngDir, layerNums);
    }
  }

  private static void handleH5File(NetcdfFile ncfile, File pngDir, int[] layerNums)
      throws java.io.IOException {
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
        !mapInfoValues[10].trim().equals("units=Meters")) {
      throw new RuntimeException(
        "Expected North,WGS-84,units=Meters in map_info[8-10] but got '" +
        mapInfoValues[8] + "," + mapInfoValues[9] + "," + mapInfoValues[10] + "'");
    }

    float rotation;
    if (mapInfoValues.length >= 12) {
      if (!mapInfoValues[11].startsWith("rotation=")) {
        throw new RuntimeException("Expected rotation=... in map_info[11]");
      }
      rotation = Float.parseFloat(mapInfoValues[11].split("=")[1]);
    } else {
      rotation = 0.0f;
    }

    Variable reflectanceVar = ncfile.findVariable("Reflectance");
    int inputWidth = reflectanceVar.getShape()[2];
    int inputHeight = reflectanceVar.getShape()[1];
    for (int layerNum : layerNums) {
      Array data;
      try {
        data = reflectanceVar.read(
          new int[]{layerNum, 0, 0}, new int[] {1, inputHeight, inputWidth});
      } catch (ucar.ma2.InvalidRangeException e) {
        throw new RuntimeException(e);
      }
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

      int shrunkenWidth = (inputWidth + 9) / 10;
      int shrunkenHeight = (inputHeight + 9) / 10;
      byte[] shrunkenBytes = new byte[shrunkenWidth * shrunkenHeight * 2];
      DataBuffer shrunkenBuffer =
        new DataBufferByte(shrunkenBytes, shrunkenBytes.length);
      WritableRaster shrunkenRaster = Raster.createInterleavedRaster(
        shrunkenBuffer, shrunkenWidth, shrunkenHeight,
        2 * shrunkenWidth, 2, new int[] {0, 1}, (Point)null);
      BufferedImage shrunkenImage =
        new BufferedImage(colorModel, shrunkenRaster, true, null);
      Graphics2D g = shrunkenImage.createGraphics();
      AffineTransform transform = AffineTransform.getScaleInstance(0.1, 0.1);
      g.drawRenderedImage(image, transform);

      File layerDir = new File(pngDir, Integer.toString(layerNum));
      layerDir.mkdir();

      File pngFile = new File(layerDir,
        "W" + inputWidth  + "_" +
        "H" + inputHeight + "_" +
        "N" + northing    + "_" +
        "E" + easting     + "_" +
        "R" + rotation    + ".png");
      ImageIO.write(shrunkenImage, "png", pngFile);
      System.err.println("Wrote " + pngFile);
    } // next layerNum

    ncfile.close();
  }
}
