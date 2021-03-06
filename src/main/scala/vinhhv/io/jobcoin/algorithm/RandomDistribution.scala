package vinhhv.io.jobcoin.algorithm

import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import vinhhv.io.jobcoin.Settings.PRECISION

import scala.math.{BigDecimal, BigInt}
import scala.util.Random

object RandomDistribution {
  // Splits the total into randomly sized N parts.
  //
  // This achieved by choosing N random numbers between 1 and the total
  // and calculating the distances between each of those numbers.
  //
  // The total is multiplied by a chosen power of 10 to give us more precision.
  //
  // ex. 10000000.45020 split into 4 parts might return
  // -> Calculate random numbers after scaling the total by a certain power of 10 (let's say 8):
  //    [0, 553911710858704, 863063339046268, 965516199923388, 1000000045020000]
  // -> Calculate the differences between the numbers and scale it back down:
  //    [5539117.10858704, 3091516.28187564, 1024528.6087712, 344838.45096612]
  def randomDistribution(total: Double, nParts: Int): IO[List[Double]] = {
    if (nParts < 2 || total <= 0.0) IO.pure(List(total))
    else {
      val totalScaled = BigDecimal(total * math.pow(10, PRECISION)).toBigInt
      val rangesIO =
        (1 to nParts)
          .toList
          .traverse(_ => generateRandomBigInt(totalScaled))
          .map(_.patch(nParts - 1, List(totalScaled), 1))

      rangesIO.map { ranges =>
        (BigInt(0) +: ranges)
          .sorted
          .sliding(2, 1)
          .map { pairs =>
            (BigDecimal(pairs(1) - pairs.head) / BigDecimal(math.pow(10, PRECISION))).toDouble
          }
          .toList
      }
    }
  }

  // Calculates how much of the total housing balance we should randomly distribute.
  // For simplicity, let's come up with some arbitrary rules:
  //
  // Let's compare if we did even distribution across all N sink addresses
  // 1. Less than 10 JobCoin? Let's randomly distribute 100% of the coin
  // 2. More than 10, but less than 100 JobCoins? Let's randomly distribute 40% - 80%
  // 3. More than 100 JobCoins? Let's randomly distribute 10% - 30%
  def getPercentageDistribution(total: Double, nParts: Int): IO[Double] = {
    val evenDistribution = total / nParts
    if (evenDistribution <= 10.toDouble)
      IO.pure(1.toDouble)
    else if (evenDistribution <= 100)
      IO((Random.nextInt(40) + 40).toDouble / 100.toDouble)
    else
      IO((Random.nextInt(20) + 10).toDouble / 100.toDouble)
  }

  // Generate a random BigInt that is greater than 0 and less than the max
  def generateRandomBigInt(max: BigInt): IO[BigInt] = {
    IO(BigInt(max.bitLength, Random)).flatMap { randomBigInt =>
      if (randomBigInt == BigInt(0) || max - randomBigInt <= BigInt(0))
        generateRandomBigInt(max)
      else
        IO.pure(randomBigInt)
    }
  }
}
