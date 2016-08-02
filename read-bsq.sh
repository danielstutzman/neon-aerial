#!/bin/bash -ex
javac ReadBSQ.java
for BSQ in /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D10/CPER/2013/CPER_L2/CPER_Spectrometer/Veg_Indices/*.dat; do
  java -Djava.awt.headless=true ReadBSQ $BSQ
  echo $BSQ
done
