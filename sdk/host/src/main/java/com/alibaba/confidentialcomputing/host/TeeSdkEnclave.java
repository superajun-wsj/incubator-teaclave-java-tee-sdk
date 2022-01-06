package com.alibaba.confidentialcomputing.host;

import com.alibaba.confidentialcomputing.host.exception.*;

import java.io.IOException;

/**
 * TeeSdkEnclave is a sgx2 enclave based on Alibaba cloud's tee sdk.
 */
class TeeSdkEnclave extends AbstractEnclave {
    private final static String JNI_EXTRACTED_PACKAGE_PATH = "jni/lib_jni_tee_sdk.so";
    private final static String TEE_SDK_SIGNED_PACKAGE_PATH = "libs/lib_enclave_tee_sdk.signed";
    private static volatile TeeSdkExtractTempPath extractTempPath;
    private final EnclaveNativeContext nativeHandlerContext = new EnclaveNativeContext(
            0, 0, 0);

    TeeSdkEnclave(EnclaveDebug mode) throws EnclaveCreatingException {
        // Set EnclaveContext for this enclave instance.
        super(EnclaveType.TEE_SDK, mode, new EnclaveServicesRecycler());

        // Extract jni .so and signed tee .so from .jar file.
        // Only once extract and load operation.
        if (extractTempPath == null) {
            synchronized (TeeSdkEnclave.class) {
                if (extractTempPath == null) {
                    try {
                        String jniTempFilePath = ExtractLibrary.extractLibrary(
                                TeeSdkEnclave.class.getClassLoader(),
                                JNI_EXTRACTED_PACKAGE_PATH);
                        String teeSdkSignedFilePath = ExtractLibrary.extractLibrary(
                                TeeSdkEnclave.class.getClassLoader(),
                                TEE_SDK_SIGNED_PACKAGE_PATH);
                        extractTempPath = new TeeSdkExtractTempPath(jniTempFilePath, teeSdkSignedFilePath);
                        System.load(jniTempFilePath);
                    } catch (IOException e) {
                        throw new EnclaveCreatingException("extracting tee sdk jni .so or signed .so failed.", e);
                    }
                }
            }
        }

        // Create tee sdk enclave by native call, enclaveHandler is set in jni in nativeHandlerContext.
        int ret = nativeCreateEnclave(mode.getValue(), extractTempPath.getTeeSdkSignedFilePath());
        if (ret != 0) {
            throw new EnclaveCreatingException("create tee sdk enclave by native calling failed.");
        }
        // Create svm attach isolate and isolateThread, and they are set in jni in nativeHandlerContext.
        ret = nativeSvmAttachIsolate(nativeHandlerContext.getEnclaveHandle());
        if (ret != 0) {
            throw new EnclaveCreatingException("create svm isolate by native calling failed.");
        }
    }

    private native int nativeCreateEnclave(int mode, String path);

    private native int nativeSvmAttachIsolate(long enclaveHandler);

    private native InnerNativeInvocationResult nativeLoadService(
            long enclaveHandler, long isolateHandler, byte[] serviceHandler);

    private native InnerNativeInvocationResult nativeInvokeMethod(
            long enclaveHandler, long isolateHandler, byte[] enclaveInvokeMetaWrapper);

    private native InnerNativeInvocationResult nativeUnloadService(
            long enclaveHandler, long isolateHandler, byte[] serviceHandler);

    private native int nativeSvmDetachIsolate(long enclaveHandler, long isolateThreadHandler);

    private native int nativeDestroyEnclave(long enclaveHandler);

    @Override
    InnerNativeInvocationResult loadServiceNative(byte[] payload) {
        return nativeLoadService(
                nativeHandlerContext.getEnclaveHandle(),
                nativeHandlerContext.getIsolateHandle(),
                payload);
    }

    @Override
    InnerNativeInvocationResult unloadServiceNative(byte[] payload) {
        return nativeUnloadService(
                nativeHandlerContext.getEnclaveHandle(),
                nativeHandlerContext.getIsolateHandle(),
                payload);
    }

    @Override
    InnerNativeInvocationResult invokeMethodNative(byte[] payload) {
        return nativeInvokeMethod(
                nativeHandlerContext.getEnclaveHandle(),
                nativeHandlerContext.getIsolateHandle(),
                payload);
    }

    @Override
    public void destroy() throws EnclaveDestroyingException {
        // destroyToken will wait for all ongoing enclave invocations finished.
        if (this.getEnclaveContext().getEnclaveToken().destroyToken()) {
            // interrupt enclave services' recycler firstly.
            this.getEnclaveContext().getEnclaveServicesRecycler().interruptServiceRecycler();
            // destroy svm isolate.
            int ret = nativeSvmDetachIsolate(nativeHandlerContext.getEnclaveHandle(),
                    nativeHandlerContext.getIsolateThreadHandle());
            if (ret != 0) {
                throw new EnclaveDestroyingException("isolate destroy native call failed.");
            }
            // destroy the enclave.
            ret = nativeDestroyEnclave(nativeHandlerContext.getEnclaveHandle());
            if (ret != 0) {
                throw new EnclaveDestroyingException("enclave destroy native call failed.");
            }
        }
    }

    class TeeSdkExtractTempPath {
        private final String jniTempFilePath;
        private final String teeSdkSignedFilePath;

        TeeSdkExtractTempPath(String jniTempFilePath, String teeSdkSignedFilePath) {
            this.jniTempFilePath = jniTempFilePath;
            this.teeSdkSignedFilePath = teeSdkSignedFilePath;
        }

        String getJniTempFilePath() {
            return jniTempFilePath;
        }

        String getTeeSdkSignedFilePath() {
            return teeSdkSignedFilePath;
        }
    }

    /**
     * JavaEnclave will create svm isolate handle and isolateThread handle by native call,
     * so EnclaveNativeContextCache will cache them for usage.
     */
    class EnclaveNativeContext {
        // enclaveHandle stores created enclave's handle id.
        private final long enclaveHandle;
        // isolate stores svm created isolate instance.
        // In JavaEnclave only one isolateHandle instance will be created.
        private final long isolateHandle;
        // isolateThreadHandle stores the first attached isolateThread Handle.
        private final long isolateThreadHandle;

        EnclaveNativeContext(long enclaveHandle, long isolateHandle, long isolateThreadHandle) {
            this.enclaveHandle = enclaveHandle;
            this.isolateHandle = isolateHandle;
            this.isolateThreadHandle = isolateThreadHandle;
        }

        long getEnclaveHandle() {
            return enclaveHandle;
        }

        long getIsolateHandle() {
            return isolateHandle;
        }

        long getIsolateThreadHandle() {
            return isolateThreadHandle;
        }
    }
}
