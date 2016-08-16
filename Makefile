.PHONY: copy_offline render_overviews

VOLUME := /Volumes/AOP_1.3a_w_WF_v1.1a
#VOLUME := offline

default: \
  output/combine_l2_spectrometer/BART.png \
  output/combine_l2_spectrometer/CPER.png \
  output/combine_l2_spectrometer/DELA.png \
  output/combine_l2_spectrometer/DSNY.png \
  output/combine_l2_spectrometer/GRSM.png \
  output/combine_l2_spectrometer/HARV.png \
  output/combine_l2_spectrometer/JERC.png \
  output/combine_l2_spectrometer/LENO.png \
  output/combine_l2_spectrometer/MLBS.png \
  output/combine_l2_spectrometer/OSBS.png \
  output/combine_l2_spectrometer/PROV.png \
  output/combine_l2_spectrometer/SAWB.png \
  output/combine_l2_spectrometer/SJER.png \
  output/combine_l2_spectrometer/SOAP.png \
  output/combine_l2_spectrometer/STER.png \
  output/combine_l2_spectrometer/TALL.png \
  output/combine_l2_spectrometer/TEAK.png \
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
 	output/combine_l3_lidar/BART.DSM.png \
	output/combine_l3_lidar/CPER.DSM.png \
	output/combine_l3_lidar/DELA.DSM.png \
	output/combine_l3_lidar/DSNY.DSM.png \
	output/combine_l3_lidar/GRSM.DSM.png \
	output/combine_l3_lidar/HARV.DSM.png \
	output/combine_l3_lidar/JERC.DSM.png \
	output/combine_l3_lidar/LENO.DSM.png \
	output/combine_l3_lidar/MLBS.DSM.png \
	output/combine_l3_lidar/OSBS.DSM.png \
	output/combine_l3_lidar/PROV.DSM.png \
	output/combine_l3_lidar/SAWB.DSM.png \
	output/combine_l3_lidar/SOAP.DSM.png \
	output/combine_l3_lidar/SJER.DSM.png \
	output/combine_l3_lidar/STER.DSM.png \
	output/combine_l3_lidar/TALL.DSM.png \
	output/combine_l3_lidar/TEAK.DSM.png \
	output/grep_kml/D17.txt

# 1st param: directory
# 2nd param: output file
define combine_l3_camera
  mkdir -p output/combine_l3_camera
	java -cp build:vendor/jai/jai_imageio-1_1/lib/jai_imageio.jar CombineL3Camera $(VOLUME)/$(1) $(2)
endef
define combine_l3_lidar
  mkdir -p output/combine_l3_lidar
	java -cp build:vendor/imagej/source/ij.jar CombineL3Lidar $(VOLUME)/$(1) $(2)
endef

vendor/imagej/source/ij.jar:
	cd vendor/imagej && unzip -n ij151d-src.zip # -n means don't overwrite
	cd vendor/imagej/source && ant build
vendor/jai/jai_imageio-1_1:
	#RELEASE=jai_imageio-1_1-lib-linux-amd64.tar.gz
	#curl http://download.java.net/media/jai-imageio/builds/release/1.1/$RELEASE > vendor/jai/$RELEASE
	cd vendor/jai && tar zxvf jai_imageio-1_1-lib-linux-amd64.tar.gz
vendor/openlayers/v3.17.1-dist:
	#curl https://github.com/openlayers/ol3/releases/download/v3.17.1/v3.17.1-dist.zip > vendor/openlayers/v3.17.1-dist.zip
	cd vendor/openlayers && unzip v3.17.1-dist.zip
vendor/json/JSON-java.jar:
	mkdir -p vendor/json/build
	javac vendor/json/JSON-java/*.java -d vendor/json/build
	jar cvf $@ -C vendor/json/build .

build/CombineL2Spectrometer.class: src/CombineL2Spectrometer.java
	javac $^ -d build
build/CombineL3Camera.class: src/CombineL3Camera.java vendor/jai/jai_imageio-1_1
	javac -cp vendor/jai/jai_imageio-1_1/lib/jai_imageio.jar $< -d build
build/CombineL3Lidar.class: src/CombineL3Lidar.java vendor/imagej/source/ij.jar
	javac -cp vendor/imagej/source/ij.jar $< -d build
build/ShrinkL2Spectrometer.class: src/ShrinkL2Spectrometer.java
	javac $^ -d build
build/GrepKML.class: src/GrepKML.java
	javac $^ -d build
build/RenderOverviews.class: src/RenderOverviews.java vendor/json/JSON-java.jar
	javac -sourcepath src -cp vendor/json/JSON-java.jar:vendor/tiff_tags/tiff_tags.jar $< -d build

output/shrink_l2_spectrometer/BART/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/BART
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/BART $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D1/BART/2014/BART_L2/BART_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/CPER/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/CPER
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/CPER $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D10/CPER/2013/CPER_L2/CPER_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/DELA/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/DELA
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/DELA $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D8/DELA/2015/DELA_L2/DELA_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/DSNY/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/DSNY
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/DSNY $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D3/DSNY/2014/DSNY_L2/DSNY_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/GRSM/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/GRSM
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/GRSM $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D7/GRSM/2015/GRSM_L2/Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/HARV/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/HARV
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/HARV $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D1/HARV/2014/HARV_L2/HARV_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/JERC/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/JERC
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/JERC $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D3/JERC/2014/JERC_L2/JERC_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/LENO/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/LENO
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/LENO $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D8/LENO/2015/LENO_L2/LENO_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/MLBS/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/MLBS
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/MLBS $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D7/MLBS/2015/MLBS_L2/MLBS_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/OSBS/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/OSBS
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/OSBS $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/PROV/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/PROV
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/PROV $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D17/PROV/2013/PROV_L2/PROV_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/SAWB/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/SAWB
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/SAWB $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D1/SAWB/2014/SAWB_L2/SAWB_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/SJER/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/SJER
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/SJER $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D17/SJER/2013/SJER_L2/SJER_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/SOAP/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/SOAP
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/SOAP $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D17/SOAP/2013/SOAP_L2/SOAP_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/STER/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/STER
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/STER $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D10/STER/2013/STER_L2/STER_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/TALL/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/TALL
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/TALL $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D8/TALL/2015/TALL_L2/TALL_Spectrometer/Veg_Indices/*.dat))
	touch $@
output/shrink_l2_spectrometer/TEAK/DONE: build/ShrinkL2Spectrometer.class
	mkdir -p output/shrink_l2_spectrometer/TEAK
	java -cp build ShrinkL2Spectrometer output/shrink_l2_spectrometer/TEAK $(filter %.dat,$(wildcard $(VOLUME)/1.3a/D17/TEAK/2013/TEAK_L2/TEAK_Spectrometer/Veg_Indices/*.dat))
	touch $@

output/combine_l2_spectrometer/BART.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/BART/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/BART $@
output/combine_l2_spectrometer/CPER.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/CPER/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/CPER $@
output/combine_l2_spectrometer/DELA.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/DELA/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/DELA $@
output/combine_l2_spectrometer/DSNY.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/DSNY/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/DSNY $@
output/combine_l2_spectrometer/GRSM.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/GRSM/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/GRSM $@
output/combine_l2_spectrometer/HARV.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/HARV/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/HARV $@
output/combine_l2_spectrometer/JERC.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/JERC/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/JERC $@
output/combine_l2_spectrometer/LENO.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/LENO/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/LENO $@
output/combine_l2_spectrometer/MLBS.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/MLBS/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/MLBS $@
output/combine_l2_spectrometer/OSBS.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/OSBS/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/OSBS $@
output/combine_l2_spectrometer/PROV.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/PROV/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/PROV $@
output/combine_l2_spectrometer/SAWB.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/SAWB/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/SAWB $@
output/combine_l2_spectrometer/SJER.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/SJER/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/SJER $@
output/combine_l2_spectrometer/SOAP.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/SOAP/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/SOAP $@
output/combine_l2_spectrometer/STER.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/STER/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/STER $@
output/combine_l2_spectrometer/TALL.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/TALL/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/TALL $@
output/combine_l2_spectrometer/TEAK.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/TEAK/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer output/shrink_l2_spectrometer/TEAK $@

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

output/combine_l3_lidar/BART.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D1/BART/2014/BART_L3/BART_Lidar/DSM,$@)
output/combine_l3_lidar/CPER.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D10/CPER/2013/CPER_L3/CPER_Lidar/DSM,$@)
output/combine_l3_lidar/DELA.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D8/DELA/2015/DELA_L3/DELA_Lidar/DSM,$@)
output/combine_l3_lidar/DSNY.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D3/DSNY/2014/DSNY_L3/DSNY_Lidar/DSM,$@)
output/combine_l3_lidar/GRSM.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D7/GRSM/2015/GRSM_L3/GRSM_Lidar/DSM,$@)
output/combine_l3_lidar/HARV.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D1/HARV/2014/HARV_L3/HARV_Lidar/DSM,$@)
output/combine_l3_lidar/JERC.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D3/JERC/2014/JERC_L3/JERC_Lidar/DSM,$@)
output/combine_l3_lidar/LENO.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D8/LENO/2015/LENO_L3/LENO_LiDAR/DSM,$@)
output/combine_l3_lidar/MLBS.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D7/MLBS/2015/MLBS_L3/MLBS_Lidar/DSM,$@)
output/combine_l3_lidar/OSBS.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D3/OSBS/2014/OSBS_L3/OSBS_Lidar/DSM,$@)
output/combine_l3_lidar/PROV.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D17/PROV/2013/PROV_L3/DSM,$@)
output/combine_l3_lidar/SAWB.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D1/SAWB/2014/SAWB_L3/SAWB_Lidar/DSM,$@)
output/combine_l3_lidar/SOAP.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D17/SOAP/2013/SOAP_L3/SOAP_Lidar/DSM,$@)
output/combine_l3_lidar/SJER.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D17/SJER/2013/SJER_L3/SJER_Lidar/DSM,$@)
output/combine_l3_lidar/STER.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D10/STER/2013/STER_L3/STER_Lidar/DSM,$@)
output/combine_l3_lidar/TALL.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D8/TALL/2015/TALL_L3/DSM,$@)
output/combine_l3_lidar/TEAK.DSM.png: build/CombineL3Lidar.class
	$(call combine_l3_lidar,1.3a/D17/TEAK/2013/TEAK_L3/TEAK_Lidar/DSM,$@)

output/grep_kml/D17.txt: build/GrepKML.class
	mkdir -p output/grep_kml
	java -cp build GrepKML $(VOLUME)/1.3a/D17/NEON_2013_D17_Camera_Images.kmz > $@

copy_offline:
	mkdir -p offline/1.3a/D17
	cp -v /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D17/NEON_2013_D17_Camera_Images.kmz offline/1.3a/D17

	mkdir -p offline/1.3a/D8/LENO/2015/LENO_L3/LENO_Camera
	cp -v `/bin/ls -S /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D8/LENO/2015/LENO_L3/LENO_Camera/*_image.tif | head -2` offline/1.3a/D8/LENO/2015/LENO_L3/LENO_Camera

	mkdir -p offline/1.3a/D8/LENO/2015/LENO_L3/LENO_LiDAR/DSM
	cp -v `/bin/ls -S /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D8/LENO/2015/LENO_L3/LENO_LiDAR/DSM/*_DSM.tif | head -2` offline/1.3a/D8/LENO/2015/LENO_L3/LENO_LiDAR/DSM

	mkdir -p offline/1.3a/D8/LENO/2015/LENO_L2/LENO_Spectrometer/Veg_Indices
	cp -v `/bin/ls /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D8/LENO/2015/LENO_L2/LENO_Spectrometer/Veg_Indices/* | head -2` offline/1.3a/D8/LENO/2015/LENO_L2/LENO_Spectrometer/Veg_Indices

render_overviews: build/RenderOverviews.class
	java -cp build:vendor/json/JSON-java.jar:vendor/imagej/source/ij.jar:vendor/tiff_tags/tiff_tags.jar RenderOverviews /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a #offline/1.3a

output/neon_domains.geojson: vendor/neon/NEON_Domains.shp
	rm -f output/neon_domains.geojson output/neon_domains_big.geojson
	~/dev/mapshaper/bin/mapshaper \
		-i vendor/neon/NEON_Domains.shp \
		-simplify 5% \
		-filter 'DomainName != "Taiga" && DomainName != "Tundra"' \
		-o output/neon_domains_big.geojson
	cat output/neon_domains_big.geojson | sed 's/\([0-9]*\.[0-9]\)[0-9]*/\1/g' \
		>output/neon_domains.geojson
	rm -f output/neon_domains_big.geojson

output/usa_states.geojson: vendor/naturalearthdata/ne_110m_admin_1_states_provinces.shp
	~/dev/mapshaper/bin/mapshaper \
		-i vendor/naturalearthdata/ne_110m_admin_1_states_provinces.shp \
		-o output/usa_states.geojson
