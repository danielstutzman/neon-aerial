#!/bin/bash -ex

echo "Building vendor/imagej/source/ij.jar"
cd vendor/imagej
unzip -n ij151d-src.zip # -n means don't overwrite
cd source
ant build
cd ../../..

RELEASE=jai_imageio-1_1-lib-linux-amd64.tar.gz
curl http://download.java.net/media/jai-imageio/builds/release/1.1/$RELEASE > $RELEASE
tar zxvf $RELEASE
rm $RELEASE
