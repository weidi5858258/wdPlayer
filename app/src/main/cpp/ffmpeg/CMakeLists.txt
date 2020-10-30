# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
add_definitions(-std=c++11)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

# 哪些cpp文件生成什么样的动态库
# 一个CMakeLists.txt只能生成一个动态库

#################################################################
# 引用头文件(所有头文件放在include这个目录)
include_directories(../include/)
#################################################################
# 找到所有cpp文件
# file(GLOB SOURCE src/main/cpp/*.cpp)
add_library(ffmpeg

        SHARED

        # SimpleVideoPlayer.cpp
        # SimpleVideoPlayer2.cpp

        ffmpeg.cpp
        MediaPlayer.cpp
        OnlyVideoPlayer.cpp
        OnlyAudioPlayer.cpp
        AudioVideoPlayer.cpp
        AACH264Player.cpp
        MediaPlayerForMediaCodec.cpp

        AudioRecord.cpp
        AudioTrack.cpp

        # 把所有cpp文件编译到ffmpeg这个动态库中
        # ${SOURCE}
        )
################################FFMPEG################################

# 这个一般是找系统库,放在这个命令里面的不会被打包到我们的apk里面
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
find_library(android-lib
        android)

#[[find_library(audioclient-lib
        audioclient)
find_library(binder-lib
        binder)
find_library(cutils-lib
        cutils)
find_library(utils-lib
        utils)]]

#################################################################
# 关联库 # ffmpeg
# 好像只能出现一个target_link_libraries(...)
target_link_libraries(
        # Specifies the target library.
        ffmpeg

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        ${android-lib}

        #[[${audioclient-lib}
        ${binder-lib}
        ${cutils-lib}
        ${utils-lib}]]

        # 编译当前库时需要引用的第三方库
        ijkffmpeg
        ijkplayer
        ijksdl
)
#################################################################

# 设置cpp目录路径
set(CPP_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp)

# 设置jniLibs目录路径(jniLibs ---> nativeLibs)
set(LIBS_DIR ${CMAKE_SOURCE_DIR}/src/main/nativeLibs)

# 设置CPU目录
# armeabi-v7a
if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CPU_DIR armeabi-v7a)
endif (${ANDROID_ABI} STREQUAL "armeabi-v7a")

# armeabi
if (${ANDROID_ABI} STREQUAL "armeabi")
    set(CPU_DIR armeabi)
endif (${ANDROID_ABI} STREQUAL "armeabi")

# arm64-v8a
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CPU_DIR arm64-v8a)
endif (${ANDROID_ABI} STREQUAL "arm64-v8a")

# x86
if (${ANDROID_ABI} STREQUAL "x86")
    set(CPU_DIR x86)
endif (${ANDROID_ABI} STREQUAL "x86")

# x86_64
if (${ANDROID_ABI} STREQUAL "x86_64")
    set(CPU_DIR x86_64)
endif (${ANDROID_ABI} STREQUAL "x86_64")

#################################################################
# 添加库
# add_library( # 库名称
#              ffmpeg
# 动态库，生成so文件
#              SHARED
# 源码
#             ${CPP_DIR}/cmdutils.c
#             ${CPP_DIR}/ffmpeg.c
#             ${CPP_DIR}/ffmpeg_filter.c
#             ${CPP_DIR}/ffmpeg_opt.c
#             ${CPP_DIR}/ffmpeg_cmd.c )
#################################################################
# 动态库
# 下面都是添加库,然后引用库
# 用于各种类型声音、图像编解码

# 添加库
add_library(
        # 库名称
        ijkffmpeg
        # 动态库
        SHARED
        # 表示该库是引用的不是生成的
        IMPORTED)
# 引用库
set_target_properties(
        # 库名称
        ijkffmpeg
        # 库的路径
        PROPERTIES IMPORTED_LOCATION
        ${LIBS_DIR}/${CPU_DIR}/libijkffmpeg.so)
#################################################################
add_library(ijkplayer
        SHARED
        IMPORTED)
set_target_properties(ijkplayer
        PROPERTIES IMPORTED_LOCATION
        ${LIBS_DIR}/${CPU_DIR}/libijkplayer.so)
#################################################################
add_library(ijksdl
        SHARED
        IMPORTED)
set_target_properties(ijksdl
        PROPERTIES IMPORTED_LOCATION
        ${LIBS_DIR}/${CPU_DIR}/libijksdl.so)
#################################################################
#add_library(avcodec
#        SHARED
#        IMPORTED)
#set_target_properties(avcodec
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libavcodec.so)
#################################################################
# 提供了各种音视频过滤器
#add_library(avfilter
#        SHARED
#        IMPORTED)
#set_target_properties(avfilter
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libavfilter.so)
#################################################################
# 用于各种音视频封装格式的生成和解析，读取音视频帧等功能
#add_library(avformat
#        SHARED
#        IMPORTED)
#set_target_properties(avformat
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libavformat.so)
#################################################################
# 包含一些公共的工具函数
#add_library(avutil
#        SHARED
#        IMPORTED)
#set_target_properties(avutil
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libavutil.so)
#################################################################
# 用于音频重采样，采样格式转换和混合
#add_library(swresample
#        SHARED
#        IMPORTED)
#set_target_properties(swresample
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libswresample.so)
#################################################################
# 用于视频场景比例缩放、色彩映射转换
#add_library(swscale
#        SHARED
#        IMPORTED)
#set_target_properties(swscale
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libswscale.so)
#################################################################
#add_library(crypto
#        SHARED
#        IMPORTED)
#set_target_properties(crypto
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libcrypto.so)
#################################################################
#add_library(ssl
#        SHARED
#        IMPORTED)
#set_target_properties(ssl
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libssl.so)
#################################################################

# 静态库
#add_library(crypto
#        STATIC
#        IMPORTED)
#set_target_properties(crypto
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libcrypto.a)
#################################################################
#add_library(ssl
#        STATIC
#        IMPORTED)
#set_target_properties(ssl
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBS_DIR}/${CPU_DIR}/libssl.a)
#################################################################