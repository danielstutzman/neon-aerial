#!/bin/bash -ex

CP=.
CP=$CP:jai_imageio-1_1/lib/jai_imageio.jar
CP=$CP:~/dev/imagej/source/ij.jar

javac -cp $CP WritePNG.java

java -Djava.awt.headless=true -cp $CP WritePNG \
  /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D1/HARV/2014/HARV_L3/HARV_Camera/ \
  4000 4000 HARV_Camera2.png jai
#  /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D17/TEAK/2013/TEAK_L3/TEAK_Lidar/DSM \
#  1000 1000 TEAK_DSM2.png imagej

