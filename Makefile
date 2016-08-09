.PHONY: combine_l3_camera

L3_CAMERA_CLASSPATH := build
L3_CAMERA_CLASSPATH := $(L3_CAMERA_CLASSPATH):vendor/jai/jai_imageio-1_1/lib/jai_imageio.jar
L3_CAMERA_CLASSPATH := $(L3_CAMERA_CLASSPATH):vendor/imagej/source/ij.jar

default: output/combine_l3_camera/HARV.png

.PRECIOUS: output/combine_l3_camera/HARV.png

# 1st param: directory
# 2nd param: output file
define combine_l3_camera 
  mkdir -p output/combine_l3_camera
	java -Djava.awt.headless=true -cp $(L3_CAMERA_CLASSPATH) CombineL3Camera \
		/Volumes/AOP_1.3a_w_WF_v1.1a/$(1) 4000 4000 $(2) jai
endef

build/CombineL2Spectrometer.class: src/CombineL2Spectrometer.java
	javac $^ -d build
build/CombineL3Camera.class: src/CombineL3Camera.java
	javac -cp $(L3_CAMERA_CLASSPATH) $^ -d build
build/ShrinkL2Spectrometer.class: src/ShrinkL2Spectrometer.java
	javac $^ -d build

output/shrink_l2_spectrometer/OSBS/DONE: build/ShrinkL2Spectrometer.class /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices/*.dat
	mkdir -p output/shrink_l2_spectrometer/OSBS
	java -cp build -Djava.awt.headless=true \
		ShrinkL2Spectrometer output/shrink_l2_spectrometer/OSBS $(filter %.dat,$(^))
	touch $@

output/combine_l3_camera/HARV.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D1/HARV/2014/HARV_L3/HARV_Camera,$@)
output/combine_l3_camera/OSBS.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D3/OSBS/2014/OSBS_L3/OSBS_Camera,$@)

output/combine_l2_spectrometer/OSBS.png: build/CombineL2Spectrometer.class output/shrink_l2_spectrometer/OSBS/DONE
	java -cp build -Djava.awt.headless=true CombineL2Spectrometer /Volumes/AOP_1.3a_w_WF_v1.1a/1.3a/D3/OSBS/2014/OSBS_L2/OSBS_Spectrometer/Veg_Indices output/shrink_l2_spectrometer/OSBS $@
