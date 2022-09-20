#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# this script will build multi enclave platform's jni.so,
# also compile enclave's edge routine files which was
# generated by enclave sdk toolchain.

# step one: cd make.sh script's path location.

# shellcheck disable=SC2006
this_script_dir=`dirname "$0"`
# shellcheck disable=SC2164
cd "$this_script_dir"

# step two: parse parameters from pom.xml
# parse and store host base dir path
export HOST_BASE_DIR=$1
export NATIVE_BASE_DIR="$HOST_BASE_DIR"/../native

# parse and store supported enclave platform set
enclave_platform_config=$2
# process supported enclave platform set
OLD_IFS="$IFS"
IFS=":"
enclave_platform_array=($enclave_platform_config)
IFS="$OLD_IFS"

# shellcheck disable=SC2068
for enclave_platform in ${enclave_platform_array[@]}
do
  echo "$enclave_platform"
  # set "enclave_platform" as TRUE to indicate how
  # to compile jni.so and edge routine
  export "$enclave_platform"=TRUE
done

if [ "$3" == clean ];
then
  # make clean.
  make -f ./Makefile clean
elif [ "$3" == build ];
then
  # make build.
  make -f ./Makefile build
  # copy MOCK_IN_SVM jni.so to target/classes, which will be packed into a jar file.
  if [[ $MOCK_IN_SVM == TRUE ]]; then
    cp -r "$NATIVE_BASE_DIR"/bin/platform/mock_in_svm/jni "$HOST_BASE_DIR"/target/classes
  fi
  # copy jni.so to target/classes, which will be packed into a jar file.
  if [[ $TEE_SDK == TRUE ]]; then
    cp -r "$NATIVE_BASE_DIR"/bin/platform/tee_sdk_svm/jni "$HOST_BASE_DIR"/target/classes
  fi
  # copy jni.so to target/classes, which will be packed into a jar file.
  if [[ $EMBEDDED_LIB_OS == TRUE ]]; then
    cp -r "$NATIVE_BASE_DIR"/bin/platform/libos_occlum_enclave/jni "$HOST_BASE_DIR"/target/classes
  fi
  # copy sgx remote attestation verification jni.so to target/classes, which will be packed into a jar file.
  if [ "$TEE_SDK" == TRUE ] || [ "$EMBEDDED_LIB_OS" == TRUE ]; then
    cp -r "$NATIVE_BASE_DIR"/bin/remote_attestation "$HOST_BASE_DIR"/target/classes
  fi
else
  echo "unsupported make command!!!"
fi
