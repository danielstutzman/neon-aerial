.PHONY: combine_l3_camera

L3_CAMERA_CLASSPATH := build
L3_CAMERA_CLASSPATH := $(L3_CAMERA_CLASSPATH):vendor/jai/jai_imageio-1_1/lib/jai_imageio.jar
L3_CAMERA_CLASSPATH := $(L3_CAMERA_CLASSPATH):vendor/imagej/source/ij.jar

default: \
  output/combine_l2_spectrometer/OSBS.png \
	output/combine_l3_camera/BART.png \
	output/combine_l3_camera/CPER.png \
	output/combine_l3_camera/DELA.png \
	output/combine_l3_camera/DSNY.png \
	output/combine_l3_camera/GRSM.png \
	output/combine_l3_camera/HARV.png \
	output/combine_l3_camera/LENO.png \
	output/combine_l3_camera/MLBS.png \
	output/combine_l3_camera/JERC.png \
	output/combine_l3_camera/OSBS.png \
	output/combine_l3_camera/SAWB.png \
	output/combine_l3_camera/STER.png \
	output/grep_kml/D17.txt

# 1st param: directory
# 2nd param: output file
define combine_l3_camera 
  mkdir -p output/combine_l3_camera
	java -Djava.awt.headless=true -cp $(L3_CAMERA_CLASSPATH) CombineL3Camera \
		/Volumes/AOP_1.3a_w_WF_v1.1a/$(1) 4000 4000 $(2) jai
endef

vendor/imagej/source/ij.jar: 
	cd vendor/imagej && unzip -n ij151d-src.zip # -n means don't overwrite
	cd vendor/imagej/source && ant build

vendor/jai/jai_imageio-1_1:
	#RELEASE=jai_imageio-1_1-lib-linux-amd64.tar.gz
	#curl http://download.java.net/media/jai-imageio/builds/release/1.1/$RELEASE > vendor/jai/$RELEASE
	cd vendor/jai && tar zxvf jai_imageio-1_1-lib-linux-amd64.tar.gz

build/CombineL2Spectrometer.class: src/CombineL2Spectrometer.java
	javac $^ -d build
build/CombineL3Camera.class: src/CombineL3Camera.java vendor/imagej/source/ij.jar vendor/jai/jai_imageio-1_1
	javac -cp $(L3_CAMERA_CLASSPATH) $< -d build
build/ShrinkL2Spectrometer.class: src/ShrinkL2Spectrometer.java
	javac $^ -d build
build/GrepKML.class: src/GrepKML.java
	javac $^ -d build

output/shrink_l2_spectrometer/OSBS/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/OSBS
	java -cp build -Djava.awt.headless=true \
		ShrinkL2Spectrometer output/shrink_l2_spectrometer/OSBS $(filter %.dat,$(wildcard /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices/*.dat))
	touch $@

output/combine_l2_spectrometer/OSBS.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/OSBS/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices output/shrink_l2_spectrometer/OSBS $@

output/combine_l3_camera/BART.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D1/BART/2014/BART_L3/BART_Camera,$@)
output/combine_l3_camera/CPER.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D10/CPER/2013/CPER_L3/CPER_Camera,$@)
output/combine_l3_camera/DELA.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D8/DELA/2015/DELA_L3/DELA_Camera,$@)
output/combine_l3_camera/DSNY.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D3/DSNY/2014/DSNY_L3/DSNY_Camera,$@)
output/combine_l3_camera/GRSM.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D7/GRSM/2015/GRSM_L3/GRSM_camera,$@)
output/combine_l3_camera/HARV.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D1/HARV/2014/HARV_L3/HARV_Camera,$@)
output/combine_l3_camera/LENO.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D8/LENO/2015/LENO_L3/LENO_Camera,$@)
output/combine_l3_camera/MLBS.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D7/MLBS/2015/MLBS_L3/MLBS_Camera,$@)
output/combine_l3_camera/JERC.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D3/JERC/2014/JERC_L3/JERC_Camera,$@)
output/combine_l3_camera/OSBS.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D3/OSBS/2014/OSBS_L3/OSBS_Camera,$@)
output/combine_l3_camera/SAWB.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D1/SAWB/2014/SAWB_L3/SAWB_Camera,$@)
output/combine_l3_camera/STER.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D10/STER/2013/STER_L3/STER_Camera,$@)

output/grep_kml/D17.txt: build/GrepKML.class
	mkdir -p output/grep_kml
	java -cp build GrepKML /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D17/NEON_2013_D17_Camera_Images.kmz > $@
