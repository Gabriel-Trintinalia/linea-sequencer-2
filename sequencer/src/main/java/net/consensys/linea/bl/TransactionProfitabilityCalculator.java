/*
 * Copyright Consensys Software Inc.
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
package net.consensys.linea.bl;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import net.consensys.linea.compress.LibCompress;
import net.consensys.linea.config.LineaProfitabilityConfiguration;
import org.hyperledger.besu.datatypes.Transaction;
import org.hyperledger.besu.datatypes.Wei;
import org.slf4j.spi.LoggingEventBuilder;

@Slf4j
public class TransactionProfitabilityCalculator {
  private final LineaProfitabilityConfiguration profitabilityConf;

  public TransactionProfitabilityCalculator(
      final LineaProfitabilityConfiguration profitabilityConf) {
    this.profitabilityConf = profitabilityConf;
  }

  public Wei profitablePriorityFeePerGas(
      final Transaction transaction,
      final double minMargin,
      final long gas,
      final Wei minGasPriceWei) {
    final int compressedTxSize = getCompressedTxSize(transaction);

    final long variableCostWei =
        profitabilityConf.extraDataPricingEnabled()
            ? profitabilityConf.variableCostWei()
            : minGasPriceWei.toLong();

    final var profitAt =
        minMargin * (variableCostWei * compressedTxSize / gas + profitabilityConf.fixedCostWei());

    final var profitAtWei = Wei.ofNumber(BigDecimal.valueOf(profitAt).toBigInteger());

    log.atDebug()
        .setMessage(
            "Estimated profitable priorityFeePerGas: {}; minMargin={}, fixedCostWei={}, "
                + "variableCostWei={}, gas={}, txSize={}, compressedTxSize={}")
        .addArgument(profitAtWei::toHumanReadableString)
        .addArgument(minMargin)
        .addArgument(profitabilityConf.fixedCostWei())
        .addArgument(variableCostWei)
        .addArgument(gas)
        .addArgument(transaction::getSize)
        .addArgument(compressedTxSize)
        .log();

    return profitAtWei;
  }

  public boolean isProfitable(
      final String context,
      final Transaction transaction,
      final double minMargin,
      final Wei effectiveGasPrice,
      final long gas,
      final Wei minGasPriceWei) {

    final Wei profitablePriorityFee =
        profitablePriorityFeePerGas(transaction, minMargin, gas, minGasPriceWei);

    if (effectiveGasPrice.lessThan(profitablePriorityFee)) {
      log(
          log.atDebug(),
          context,
          transaction,
          minMargin,
          effectiveGasPrice,
          profitablePriorityFee,
          gas,
          minGasPriceWei);
      return false;
    }

    log(
        log.atTrace(),
        context,
        transaction,
        minMargin,
        effectiveGasPrice,
        profitablePriorityFee,
        gas,
        minGasPriceWei);
    return true;
  }

  private int getCompressedTxSize(final Transaction transaction) {
    final byte[] bytes = transaction.encoded().toArrayUnsafe();
    return LibCompress.CompressedSize(bytes, bytes.length);
  }

  private void log(
      final LoggingEventBuilder leb,
      final String context,
      final Transaction transaction,
      final double minMargin,
      final Wei effectiveGasPrice,
      final Wei profitableGasPrice,
      final long gasUsed,
      final Wei minGasPriceWei) {

    leb.setMessage(
            "Context {}. Transaction {} has a margin of {}, minMargin={}, effectiveGasPrice={},"
                + " profitableGasPrice={}, fixedCostWei={}, variableCostWei={}, "
                + " gasUsed={}")
        .addArgument(context)
        .addArgument(transaction::getHash)
        .addArgument(
            () ->
                effectiveGasPrice.toBigInteger().doubleValue()
                    / profitableGasPrice.toBigInteger().doubleValue())
        .addArgument(minMargin)
        .addArgument(effectiveGasPrice::toHumanReadableString)
        .addArgument(profitableGasPrice::toHumanReadableString)
        .addArgument(profitabilityConf.fixedCostWei())
        .addArgument(
            () ->
                profitabilityConf.extraDataPricingEnabled()
                    ? profitabilityConf.variableCostWei()
                    : minGasPriceWei.toLong())
        .addArgument(gasUsed)
        .log();
  }
}
