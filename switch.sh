#!/bin/bash

sed -i -s s/$1/$2/ src/com/kedzie/drawer/drag/DragLayout.java src/com/kedzie/drawer/drag/DraggerActivity.java res/layout/drawer_layout.xml