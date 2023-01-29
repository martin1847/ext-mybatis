rm -rf ~/.gradle/caches/modules-2/files-2.*/com.bt*
rm -rf ~/.gradle/caches/modules-2/metadata-2.*/descriptors/com.bt*

gradle clean publish
#&& rm -rf ~/.gradle/caches/modules-2/files-2.1/com.bt.common/*  && rm -rf ~/.gradle/caches/modules-2/files-2.1/com.bt.ext/*
