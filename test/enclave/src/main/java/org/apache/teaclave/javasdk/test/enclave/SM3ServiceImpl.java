// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.teaclave.javasdk.test.enclave;

import org.apache.teaclave.javasdk.test.common.SM3Service;
import com.google.auto.service.AutoService;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SM3Digest;

@AutoService(SM3Service.class)
public class SM3ServiceImpl implements SM3Service {
    @Override
    public byte[] sm3Service(String plainText) {
        byte[] messages = plainText.getBytes();
        Digest md = new SM3Digest();
        md.update(messages, 0, messages.length);
        byte[] digest = new byte[md.getDigestSize()];
        md.doFinal(digest, 0);
        return digest;
    }
}
