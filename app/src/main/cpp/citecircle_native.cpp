#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "citation_parser.h"

#define TAG "CiteCircleNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_data_nativebridge_NativeCitationEngine_nativeGetVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "CiteCircle-Native-Core 1.0 (C++20/ARM-NEON)";
    return env->NewStringUTF(version.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_data_nativebridge_NativeCitationEngine_nativeValidateDocumentBytes(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray byte_array) {
    if (byte_array == nullptr) return 0;

    jsize len = env->GetArrayLength(byte_array);
    if (len <= 0) return 0;

    jbyte* buffer = env->GetByteArrayElements(byte_array, nullptr);
    if (buffer == nullptr) return 0;

    int32_t format = citecircle::detectDocumentFormat(
            reinterpret_cast<const uint8_t*>(buffer),
            static_cast<size_t>(len)
    );

    env->ReleaseByteArrayElements(byte_array, buffer, JNI_ABORT);
    return format;
}

JNIEXPORT jobjectArray JNICALL
Java_com_example_data_nativebridge_NativeCitationEngine_nativeParseBibTeX(
        JNIEnv* env,
        jobject /* this */,
        jstring raw_bibtex) {
    if (raw_bibtex == nullptr) return nullptr;

    const char* chars = env->GetStringUTFChars(raw_bibtex, nullptr);
    if (chars == nullptr) return nullptr;

    std::string_view bibView(chars);
    std::vector<citecircle::ParsedBibTeXEntry> entries = citecircle::parseBibTeX(bibView);

    env->ReleaseStringUTFChars(raw_bibtex, chars);

    // Locate Kotlin NativeBibEntry class
    jclass entryClass = env->FindClass("com/example/data/nativebridge/NativeBibEntry");
    if (entryClass == nullptr) {
        LOGE("Could not find class com.example.data.nativebridge.NativeBibEntry");
        return nullptr;
    }

    jmethodID entryConstructor = env->GetMethodID(
            entryClass,
            "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
    );
    if (entryConstructor == nullptr) {
        LOGE("Could not find constructor for NativeBibEntry");
        return nullptr;
    }

    jobjectArray resultArray = env->NewObjectArray(
            static_cast<jsize>(entries.size()),
            entryClass,
            nullptr
    );

    for (size_t i = 0; i < entries.size(); ++i) {
        const auto& e = entries[i];

        auto getField = [&e](const std::string& key) -> std::string {
            auto it = e.fields.find(key);
            return (it != e.fields.end()) ? it->second : "";
        };

        jstring jType = env->NewStringUTF(e.entryType.c_str());
        jstring jKey = env->NewStringUTF(e.citeKey.c_str());
        jstring jTitle = env->NewStringUTF(getField("title").c_str());
        jstring jAuthor = env->NewStringUTF(getField("author").c_str());
        jstring jYear = env->NewStringUTF(getField("year").c_str());
        jstring jVenue = env->NewStringUTF(getField("journal").empty() ? getField("booktitle").c_str() : getField("journal").c_str());
        jstring jDoi = env->NewStringUTF(getField("doi").c_str());

        jobject entryObj = env->NewObject(
                entryClass,
                entryConstructor,
                jType, jKey, jTitle, jAuthor, jYear, jVenue, jDoi
        );

        env->SetObjectArrayElement(resultArray, static_cast<jsize>(i), entryObj);

        env->DeleteLocalRef(jType);
        env->DeleteLocalRef(jKey);
        env->DeleteLocalRef(jTitle);
        env->DeleteLocalRef(jAuthor);
        env->DeleteLocalRef(jYear);
        env->DeleteLocalRef(jVenue);
        env->DeleteLocalRef(jDoi);
        env->DeleteLocalRef(entryObj);
    }

    return resultArray;
}

} // extern "C"
