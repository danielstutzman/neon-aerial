#!/bin/bash -ex
javac WritePNG.java
java -cp .:jai_imageio-1_1/lib/jai_imageio.jar -Djava.awt.headless=true WritePNG
