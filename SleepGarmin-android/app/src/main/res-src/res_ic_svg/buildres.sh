#!/bin/bash
if [ $# -eq 0 ]
  then
    echo "No arguments supplied, processing *.svg"
    for f in *.svg;
      do
        echo "Processing $f"
        inkscape -C -w 36 -h 36 -e ../../res/drawable-hdpi/${f/.svg}.png ./$f
        inkscape -C -w 24 -h 24 -e ../../res/drawable-mdpi/${f/.svg}.png ./$f
        inkscape -C -w 48 -h 48 -e ../../res/drawable-xhdpi/${f/.svg}.png ./$f
        inkscape -C -w 72 -h 72 -e ../../res/drawable-xxhdpi/${f/.svg}.png ./$f
      done
  else
    for f in "$@"
      do
        echo "Processing $f"
        inkscape -C -w 36 -h 36 -e ../../res/drawable-hdpi/${f/.svg}.png ./$f
        inkscape -C -w 24 -h 24 -e ../../res/drawable-mdpi/${f/.svg}.png ./$f
        inkscape -C -w 48 -h 48 -e ../../res/drawable-xhdpi/${f/.svg}.png ./$f
        inkscape -C -w 72 -h 72 -e ../../res/drawable-xxhdpi/${f/.svg}.png ./$f
      done
fi



