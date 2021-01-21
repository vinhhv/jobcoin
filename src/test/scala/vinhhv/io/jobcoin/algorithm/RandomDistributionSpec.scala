package vinhhv.io.jobcoin.algorithm

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import vinhhv.io.jobcoin.Settings.PRECISION

final class RandomDistributionSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {
  val randomDistributions = for {
    total <- Gen.choose(1.toDouble, 100000.toDouble).suchThat(_ > 1.0)
    nParts <- Gen.choose(1, 4)
  } yield (total, nParts)
  "Calculating random distributions" should "add up to the original total" in {
    forAll(randomDistributions) {
      case (total, nParts) =>
        val distributions = RandomDistribution.randomDistribution(total, nParts).unsafeRunSync
        val totalAdded = distributions.foldRight(0.0)(_ + _)
        val totalAddedRounded = BigDecimal(totalAdded).setScale(PRECISION, BigDecimal.RoundingMode.HALF_EVEN).toDouble
        val totalRounded = BigDecimal(total).setScale(PRECISION, BigDecimal.RoundingMode.HALF_EVEN).toDouble

        // If we are within 1 precision margin, consider it successful
        val precision = 1.toDouble / scala.math.pow(10, PRECISION)
        if (
          !(totalAddedRounded - precision == totalRounded ||
            totalAddedRounded == totalRounded ||
            totalAddedRounded + precision == totalRounded))
          fail("Distributions did not add up")
    }
  }
}
