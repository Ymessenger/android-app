# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)



# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.



include_directories(${CMAKE_SOURCE_DIR}/openssl/lib/${ANDROID_ABI}/include)

add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/openssl/lib/${ANDROID_ABI}/libcrypto.a)

add_library(ssl SHARED IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/openssl/lib/${ANDROID_ABI}/libssl.a)



add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
    CAsymmetricDecryptor.cpp
    CAsymmetricDecryptor.h
    CAsymmetricEncryptor.cpp
    CAsymmetricEncryptor.h
    CAsymmetricKeysGenerator.cpp
    CAsymmetricKeysGenerator.h
    CDecryptor.cpp
    CDecryptor.h
    CEncryptor.cpp
    CEncryptor.h
    CError.cpp
    CError.h
    CGenerator.cpp
    CGenerator.h
    COpenSSLAsymmetricKeysGenerator.cpp
    COpenSSLAsymmetricKeysGenerator.h
    COpenSSLRSADecryptor.cpp
    COpenSSLRSADecryptor.h
    COpenSSLRSAEncryptAsymmetricKeysGenerator.cpp
    COpenSSLRSAEncryptAsymmetricKeysGenerator.h
    COpenSSLRSAEncryptor.cpp
    COpenSSLRSAEncryptor.h
    CRandomOpenSSL.cpp
    CRandomOpenSSL.h
    CSign.cpp
    CSign.h
    CSignOpenSSLRSA.cpp
    CSignOpenSSLRSA.h
    CSymmetricOpenSSLGeneratorFromPassword.cpp
    CSymmetricOpenSSLGeneratorFromPassword.h
    CVerify.cpp
    CVerify.h
    CVerifyOpenSSLRSA.cpp
    CVerifyOpenSSLRSA.h
    CYDigestOpenSSL.cpp
    CYDigestOpenSSL.h
    CYException.cpp
    CYException.h
    CYSecurityFacad.cpp
    CYSecurityFacad.h
    native-lib.cpp
    openssl
    SymmetricEncrypt.cpp
    SymmetricEncrypt.h
    WrapperDigest.cpp
    WrapperDigest.h
    WrapperSymmetricEncryptor.cpp
    WrapperSymmetricEncryptor.h
    YDigest.h
    YSecurityElementaryTypes.h
    YSecurityTypes.cpp
    YSecurityTypes.h
    YTime.cpp
    YTime.h)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib

        ssl
        crypto

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})


