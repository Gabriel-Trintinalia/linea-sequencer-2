/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.consensys.linea.zktracer.module.hub;

import net.consensys.linea.zktracer.opcode.OpCode;

enum CallFrameType {
  InitCode,
  Standard,
  Delegate,
  Static,
  CallCode,
  Root;

  public CallFrameType ofOpCode(OpCode opCode) {
    return switch (opCode) {
      case CREATE, CREATE2 -> InitCode;
      case DELEGATECALL -> Delegate;
      case CALLCODE -> CallCode;
      case STATICCALL -> Static;
      default -> Standard;
    };
  }
}