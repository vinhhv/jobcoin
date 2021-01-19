package vinhhv.io.jobcoin

import com.typesafe.config.ConfigFactory

object Settings {
  val config = ConfigFactory.load().getConfig("jobcoin")
  val ADDRESSES_URL = config.getString("apiAddressesUrl")
  val TRANSACTIONS_URL = config.getString("apiTransactionsUrl")

  val PRECISION = config.getInt("precision")
}
