package vinhhv.io.jobcoin

import com.typesafe.config.ConfigFactory

object Settings {
  val config = ConfigFactory.load().getConfig("jobcoin")
  val ADDRESSES_URL = config.getString("apiAddressesUrl")
  val TRANSACTIONS_URL = config.getString("apiTransactionsUrl")

  val PRECISION = config.getInt("precision")
  val DEPOSIT_SERVICE_SCHEDULE = config.getInt("schedule_for_deposit_service")
  val MIXER_SERVICE_SCHEDULE = config.getInt("schedule_for_mixer_service")

  val PORT = config.getInt("port")
}
