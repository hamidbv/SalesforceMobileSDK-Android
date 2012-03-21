#!/bin/sh

export ORIGINAL_DIR=`pwd`
export NEW_APP=$1
export NEW_APP_LC=`echo $NEW_APP | tr '[:upper:]' '[:lower:]'`
export TARGET_FILES="AndroidManifest.xml .project build.xml res/values/strings.xml res/values/rest.xml src/com/salesforce/samples/templateapp/*.java"

if [ -d  native/$NEW_APP ]; 
then
    echo "Error: native/$NEW_APP already exists"
    exit 1
fi

echo "Creating native app $NEW_APP from template"
cp -R native/TemplateApp native/$NEW_APP
cd native/$NEW_APP
echo "1/3) created project at `pwd`"

sed -i s/TemplateApp/$NEW_APP/g $TARGET_FILES
sed  -i s/samples.templateapp/$NEW_APP_LC/g $TARGET_FILES
mv src/com/salesforce/samples/templateapp/TemplateApp.java src/com/salesforce/samples/templateapp/$NEW_APP.java
echo "2/3) renamed references"
mv src/com/salesforce/samples/templateapp  src/com/salesforce/$NEW_APP_LC
echo "3/3) fixed package structure"

