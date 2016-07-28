import java.io.*;
import java.util.zip.*;
import java.util.Scanner;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
 
class ReadZip {
  public static void main(String[] argv) throws java.io.IOException {
    new ReadZip().mainNonStatic(argv);
  }

  public void mainNonStatic(String[] argv) throws java.io.IOException {
    if (argv.length == 0) {
      System.err.println("Usage: provide paths to .kml files as arguments");
      System.exit(1);
    }
    for (String path : argv) {
      ZipInputStream zipinputstream = new ZipInputStream(new FileInputStream(path));
      ZipEntry zipEntry = zipinputstream.getNextEntry();
      while (zipEntry != null) { 
        String entryName = zipEntry.getName();
        System.out.println(entryName);
        File newFile = new File(entryName);
        if (!newFile.isDirectory()) {
          SAXParserFactory factory = SAXParserFactory.newInstance();

          SAXParser parser;
          try {
            parser = factory.newSAXParser();
          } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new RuntimeException(e);
          } catch (SAXException e) {
            throw new RuntimeException(e);
          }

          try {
            parser.parse(new WontCloseBufferedInputStream(zipinputstream),
              new org.xml.sax.helpers.DefaultHandler() {
                private String text;
                private float minLat = Float.POSITIVE_INFINITY;
                private float maxLat = Float.NEGATIVE_INFINITY;
                private float minLon = Float.POSITIVE_INFINITY;
                private float maxLon = Float.NEGATIVE_INFINITY;

                public void characters(char[] ac, int i, int j) throws SAXException {
                  text = new String(ac, i, j);
                }

                public void endElement(String s, String s1, String elementName)
                    throws SAXException {
                  if (elementName.equals("name")) {
                    System.out.print(text + "\t");
                  } else if (elementName.equals("coordinates")) {
                    System.out.println(text);

                    String[] parts = text.split(",");
                    float lat = Float.parseFloat(parts[0]);
                    float lon = Float.parseFloat(parts[1]);
                    if (lat < minLat) minLat = lat;
                    if (lat > maxLat) maxLat = lat;
                    if (lon < minLon) minLon = lon;
                    if (lon > maxLon) maxLon = lon;
                  } else if (elementName.equals("kml")) {
                    System.out.println(minLat);
                    System.out.println(maxLat);
                    System.out.println(minLon);
                    System.out.println(maxLon);
                  }
                }
              }
            );
          } catch (org.xml.sax.SAXException e) {
            throw new RuntimeException(e);
          }
          zipinputstream.closeEntry();
        }
        zipEntry = zipinputstream.getNextEntry();
      }
      zipinputstream.close();
    }
  }

  class WontCloseBufferedInputStream extends BufferedInputStream {
    public WontCloseBufferedInputStream(InputStream in) { super(in); }
    public void close() throws java.io.IOException {}
  }
}
