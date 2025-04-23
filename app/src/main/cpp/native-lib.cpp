#include <jni.h>
#include <iostream>
#include <string>
#include <vector>  // ✅ Add this to fix your build error

extern "C" JNIEXPORT jobjectArray JNICALL
Java_mc_assignment3_matrixcalculator_MainActivity_addMatrices(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray matA,
        jobjectArray matB,
        jint rows,
        jint cols) {

    std::vector<std::vector<float>> result(rows, std::vector<float>(cols));

    for (int i = 0; i < rows; ++i) {
        auto rowA = (jfloatArray) env->GetObjectArrayElement(matA, i);
        auto rowB = (jfloatArray) env->GetObjectArrayElement(matB, i);

        jfloat* elemsA = env->GetFloatArrayElements(rowA, 0);
        jfloat* elemsB = env->GetFloatArrayElements(rowB, 0);

        for (int j = 0; j < cols; ++j) {
            result[i][j] = elemsA[j] + elemsB[j];
        }

        env->ReleaseFloatArrayElements(rowA, elemsA, 0);
        env->ReleaseFloatArrayElements(rowB, elemsB, 0);
    }

    jobjectArray output = env->NewObjectArray(rows, env->FindClass("[F"), nullptr);
    for (int i = 0; i < rows; ++i) {
        jfloatArray row = env->NewFloatArray(cols);
        env->SetFloatArrayRegion(row, 0, cols, result[i].data());
        env->SetObjectArrayElement(output, i, row);
        env->DeleteLocalRef(row);
    }

    return output;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_mc_assignment3_matrixcalculator_MainActivity_subMatrices(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray matA,
        jobjectArray matB,
        jint rows,
        jint cols) {

    std::vector<std::vector<float>> result(rows, std::vector<float>(cols));

    for (int i = 0; i < rows; ++i) {
        auto rowA = (jfloatArray) env->GetObjectArrayElement(matA, i);
        auto rowB = (jfloatArray) env->GetObjectArrayElement(matB, i);

        jfloat* elemsA = env->GetFloatArrayElements(rowA, 0);
        jfloat* elemsB = env->GetFloatArrayElements(rowB, 0);

        for (int j = 0; j < cols; ++j) {
            result[i][j] = elemsA[j] - elemsB[j];
        }

        env->ReleaseFloatArrayElements(rowA, elemsA, 0);
        env->ReleaseFloatArrayElements(rowB, elemsB, 0);
    }

    jobjectArray output = env->NewObjectArray(rows, env->FindClass("[F"), nullptr);
    for (int i = 0; i < rows; ++i) {
        jfloatArray row = env->NewFloatArray(cols);
        env->SetFloatArrayRegion(row, 0, cols, result[i].data());
        env->SetObjectArrayElement(output, i, row);
        env->DeleteLocalRef(row);
    }

    return output;
}

