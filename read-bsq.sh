#!/bin/bash -ex
javac ReadBSQ.java
for BSQ in /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices/*.dat; do
  java -Djava.awt.headless=true ReadBSQ $BSQ
  echo $BSQ
done
