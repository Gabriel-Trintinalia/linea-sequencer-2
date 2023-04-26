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
package net.consensys.linea.zktracer.module.alu.mul;

import org.hyperledger.besu.evm.frame.MessageFrame;

import java.util.List;

import net.consensys.linea.zktracer.OpCode;
import net.consensys.linea.zktracer.bytes.UnsignedByte;
import net.consensys.linea.zktracer.module.ModuleTracer;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class MulTracer implements ModuleTracer {

  private int stamp = 0;

  @Override
  public String jsonKey() {
    return "mul";
  }

  @Override
  public List<OpCode> supportedOpCodes() {
    return List.of(OpCode.MUL, OpCode.EXP);
  }

  @SuppressWarnings("UnusedVariable")
  @Override
  public Object trace(MessageFrame frame) {
    final Bytes32 arg1 = Bytes32.wrap(frame.getStackItem(0));
    final Bytes32 arg2 = Bytes32.wrap(frame.getStackItem(1));

    final OpCode opCode = OpCode.of(frame.getCurrentOperation().getOpcode());

    // argument order is reversed ??
    final MulData data = new MulData(opCode, arg2, arg1);
    final MulTrace.Trace.Builder builder = MulTrace.Trace.Builder.newInstance();

    switch (data.getRegime()) {
      case EXPONENT_ZERO_RESULT -> {
        trace(builder, data);
      }

      case EXPONENT_NON_ZERO_RESULT -> {
        if (data.carryOn()) {
          data.update();
          trace(builder, data);
        }
      }

      case TRIVIAL_MUL, NON_TRIVIAL_MUL -> {
        data.setHsAndBits(UInt256.fromBytes(arg1), UInt256.fromBytes(arg2));
        trace(builder, data);
      }

      default -> throw new RuntimeException("regime not supported");
    }
    // TODO captureBlockEnd should be called from elsewhere - not within messageFrame
    //    captureBlockEnd();
    MulData finalZeroToTheZero = new MulData(OpCode.EXP, Bytes32.ZERO, Bytes32.ZERO);
    trace(builder, finalZeroToTheZero);

    return builder.build();
  }

  private void trace(final MulTrace.Trace.Builder builder, final MulData data) {
    stamp++;

    for (int ct = 0; ct < data.maxCt(); ct++) {
      trace(builder, data, ct);
    }
  }

  private void trace(final MulTrace.Trace.Builder builder, final MulData data, final int i) {
    builder.appendStamp(stamp);
    builder.appendCounter(i);

    builder
        .appendOneLineInstruction(data.isOneLineInstruction())
        .appendTinyBase(data.tinyBase)
        .appendTinyExponent(data.tinyExponent)
        .appendResultVanishes(data.res.isZero());

    builder
        .appendInst(UnsignedByte.of(data.opCode.value))
        .appendArg1Hi(data.arg1Hi.toUnsignedBigInteger())
        .appendArg1Lo(data.arg1Lo.toUnsignedBigInteger())
        .appendArg2Hi(data.arg2Hi.toUnsignedBigInteger())
        .appendArg2Lo(data.arg2Lo.toUnsignedBigInteger());

    builder
        .appendResHi(data.res.getHigh().toUnsignedBigInteger())
        .appendResLo(data.res.getLow().toUnsignedBigInteger());

    builder.appendBits(data.bits[i]);

    builder
        .appendByteA3(UnsignedByte.of(data.aBytes.get(3, i)))
        .appendByteA2(UnsignedByte.of(data.aBytes.get(2, i)))
        .appendByteA1(UnsignedByte.of(data.aBytes.get(1, i)))
        .appendByteA0(UnsignedByte.of(data.aBytes.get(0, i)));
    builder
        .appendAccA3(data.aBytes.getRange(3, 0, i + 1).toUnsignedBigInteger())
        .appendAccA2(data.aBytes.getRange(2, 0, i + 1).toUnsignedBigInteger())
        .appendAccA1(data.aBytes.getRange(1, 0, i + 1).toUnsignedBigInteger())
        .appendAccA0(data.aBytes.getRange(0, 0, i + 1).toUnsignedBigInteger());

    builder
        .appendByteB3(UnsignedByte.of(data.bBytes.get(3, i)))
        .appendByteB2(UnsignedByte.of(data.bBytes.get(2, i)))
        .appendByteB1(UnsignedByte.of(data.bBytes.get(1, i)))
        .appendByteB0(UnsignedByte.of(data.bBytes.get(0, i)));
    builder
        .appendAccB3(data.bBytes.getRange(3, 0, i + 1).toUnsignedBigInteger())
        .appendAccB2(data.bBytes.getRange(2, 0, i + 1).toUnsignedBigInteger())
        .appendAccB1(data.bBytes.getRange(1, 0, i + 1).toUnsignedBigInteger())
        .appendAccB0(data.bBytes.getRange(0, 0, i + 1).toUnsignedBigInteger());
    builder
        .appendByteC3(UnsignedByte.of(data.cBytes.get(3, i)))
        .appendByteC2(UnsignedByte.of(data.cBytes.get(2, i)))
        .appendByteC1(UnsignedByte.of(data.cBytes.get(1, i)))
        .appendByteC0(UnsignedByte.of(data.cBytes.get(0, i)));
    builder
        .appendAccC3(data.cBytes.getRange(3, 0, i + 1).toUnsignedBigInteger())
        .appendAccC2(data.cBytes.getRange(2, 0, i + 1).toUnsignedBigInteger())
        .appendAccC1(data.cBytes.getRange(1, 0, i + 1).toUnsignedBigInteger())
        .appendAccC0(data.cBytes.getRange(0, 0, i + 1).toUnsignedBigInteger());

    builder
        .appendByteH3(UnsignedByte.of(data.hBytes.get(3, i)))
        .appendByteH2(UnsignedByte.of(data.hBytes.get(2, i)))
        .appendByteH1(UnsignedByte.of(data.hBytes.get(1, i)))
        .appendByteH0(UnsignedByte.of(data.hBytes.get(0, i)));
    builder
        .appendAccH3(data.hBytes.getRange(3, 0, i + 1).toUnsignedBigInteger())
        .appendAccH2(data.hBytes.getRange(2, 0, i + 1).toUnsignedBigInteger())
        .appendAccH1(data.hBytes.getRange(1, 0, i + 1).toUnsignedBigInteger())
        .appendAccH0(data.hBytes.getRange(0, 0, i + 1).toUnsignedBigInteger());
    builder
        .appendExponentBit(data.exponentBit())
        .appendExponentBitAcc(data.expAcc.toUnsignedBigInteger())
        .appendExponentBitSource(data.exponentSource())
        .appendSquareAndMultiply(data.snm)
        .appendBitNum(data.getBitNum());
    builder.setStamp(stamp);
  }
}