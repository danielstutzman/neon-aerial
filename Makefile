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

build/CombineL3Camera.class: src/CombineL3Camera.java
	javac -cp $(L3_CAMERA_CLASSPATH) $^ -d build

output/combine_l3_camera/HARV.png: build/CombineL3Camera.class
	$(call combine_l3_camera,1.3a/D1/HARV/2014/HARV_L3/HARV_Camera,$@)
